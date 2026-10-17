package com.micropulse.monitoringengine;

import com.micropulse.monitoringengine.config.TargetServiceProperties;
import com.micropulse.monitoringengine.dto.HealthResponse;
import com.micropulse.monitoringengine.dto.HealthTick;
import com.micropulse.monitoringengine.dto.MetricResponse;
import com.micropulse.monitoringengine.dto.MetricsTick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class PollingService {

    private static final Logger log = LoggerFactory.getLogger(PollingService.class);

    private final WebClient webClient;
    private final TargetServiceProperties targetServiceProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SseService sseService;

    @Autowired
    public PollingService(WebClient webClient,
                          TargetServiceProperties targetServiceProperties,
                          RedisTemplate<String, Object> redisTemplate,
                          SseService sseService) {
        this.webClient = webClient;
        this.targetServiceProperties = targetServiceProperties;
        this.redisTemplate = redisTemplate;
        this.sseService = sseService;
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
        Mono<Double> heapMaxMono = fetchMetricValue(target.getBaseUrl(), "jvm.memory.max?tag=area:heap");
        Mono<Double> threadsMono = fetchMetricValue(target.getBaseUrl(), "jvm.threads.live");
        Mono<Double> cpuMono = fetchMetricValue(target.getBaseUrl(), "system.cpu.usage");

        Mono.zip(heapMono, heapMaxMono, threadsMono, cpuMono)
                .subscribe(
                        result -> {
                            double heapBytes = result.getT1();
                            double heapMaxBytes = result.getT2();
                            double threads = result.getT3();
                            double cpu = result.getT4();

                            MetricsTick tick = new MetricsTick();
                            tick.setServiceId(target.getName());
                            tick.setTimestamp(System.currentTimeMillis());
                            tick.setHeapUsedMb(heapBytes / (1024 * 1024));
                            tick.setHeapMaxMb(heapMaxBytes / (1024 * 1024));
                            tick.setActiveThreads((int) threads);
                            tick.setCpuUsagePercent(cpu * 100);

                            log.info("[{}] {}", target.getName(), tick);
                            saveToRedis("metrics:" + target.getName(), tick.getTimestamp(), tick);
                            sseService.broadcast("metrics", tick);
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
                .timeout(Duration.ofSeconds(3))
                .map(response -> response.getMeasurements().get(0).getValue());
    }

    private void pollHealth(TargetServiceProperties.Target target) {
        String healthUrl = target.getBaseUrl() + "/actuator/health";

        webClient.get()
                .uri(healthUrl)
                .retrieve()
                .bodyToMono(HealthResponse.class)
                .timeout(Duration.ofSeconds(3))
                .subscribe(
                        response -> recordHealth(target, response.getStatus()),
                        error -> {
                            log.warn("[{}] Health check failed: {}", target.getName(), error.getMessage());
                            recordHealth(target, "DOWN");
                        }
                );
    }

    private void recordHealth(TargetServiceProperties.Target target, String status) {
        HealthTick tick = new HealthTick();
        tick.setServiceId(target.getName());
        tick.setStatus(status);
        tick.setTimestamp(System.currentTimeMillis());

        log.info("[{}] {}", target.getName(), tick);
        saveToRedis("health:" + target.getName(), tick.getTimestamp(), tick);
        sseService.broadcast("health", tick);
    }

    private void saveToRedis(String redisKey, long timestamp, Object tick) {
        try {
            redisTemplate.opsForZSet().add(redisKey, tick, timestamp);
            long fifteenMinutesAgo = System.currentTimeMillis() - (15 * 60 * 1000);
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, fifteenMinutesAgo);

            log.info("Saved to Redis: {}", redisKey);
        } catch (Exception e) {
            log.error("Failed to save to Redis, key={}", redisKey, e);
        }
    }
}