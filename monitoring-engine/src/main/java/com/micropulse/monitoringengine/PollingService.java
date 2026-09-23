package com.micropulse.monitoringengine;

import com.micropulse.monitoringengine.config.TargetServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import com.micropulse.monitoringengine.dto.MetricResponse;
import com.micropulse.monitoringengine.dto.MetricsTick;

import reactor.core.publisher.Mono;

@Component
public class PollingService {

    private static final Logger log = LoggerFactory.getLogger(PollingService.class);

    private final WebClient webClient;
    private final TargetServiceProperties targetServiceProperties;

    @Autowired
    public PollingService(WebClient webClient, TargetServiceProperties targetServiceProperties) {
        this.webClient = webClient;
        this.targetServiceProperties = targetServiceProperties;
    }





    @Scheduled(fixedRate = 5000)
    public void pollAllTargets() {
        for (TargetServiceProperties.Target target : targetServiceProperties.getTargets()) {
            pollHealth(target);
            pollMetrics(target);
        }
    }


    private void pollMetrics(TargetServiceProperties.Target target) {
        Mono<Double> heapMono = fetchMetricValue(target.getBaseUrl(), "jvm.memory.used?tag=area:heap");
        Mono<Double> threadsMono = fetchMetricValue(target.getBaseUrl(), "jvm.threads.live");
        Mono<Double> cpuMono = fetchMetricValue(target.getBaseUrl(), "system.cpu.usage");

        Mono.zip(heapMono, threadsMono, cpuMono)
                .subscribe(
                        result -> {
                            double heapBytes = result.getT1();
                            double threads = result.getT2();
                            double cpu = result.getT3();

                            MetricsTick tick = new MetricsTick();
                            tick.setServiceId(target.getName());
                            tick.setTimestamp(System.currentTimeMillis());
                            tick.setHeapUsedMb(heapBytes / (1024 * 1024));
                            tick.setActiveThreads((int) threads);
                            tick.setCpuUsagePercent(cpu * 100);

                            log.info("[{}] {}", target.getName(), tick);
                        },
                        error -> log.warn("[{}] Metrics poll failed: {}", target.getName(), error.getMessage())
                );
     }



    private Mono<Double> fetchMetricValue(String baseUrl, String metricPath) {
    String url = baseUrl + "/actuator/metrics/" + metricPath;

    return webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(MetricResponse.class)
            .map(response -> response.getMeasurements().get(0).getValue());
    }



    private void pollHealth(TargetServiceProperties.Target target) {
        String healthUrl = target.getBaseUrl() + "/actuator/health";

        webClient.get()
                .uri(healthUrl)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                        response -> log.info("[{}] Health check succeeded: {}", target.getName(), response),
                        error -> log.warn("[{}] Health check failed: {}", target.getName(), error.getMessage())
                );
    }   
}