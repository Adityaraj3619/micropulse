package com.micropulse.monitoringengine;

import com.micropulse.monitoringengine.config.TargetServiceProperties;
import com.micropulse.monitoringengine.dto.HealthTick;
import com.micropulse.monitoringengine.dto.MetricsTick;
import com.micropulse.monitoringengine.dto.RiskProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RiskService {

    private static final Logger log = LoggerFactory.getLogger(RiskService.class);

    private static final long METRICS_WINDOW_MS = 60_000L;
    private static final long HEALTH_WINDOW_MS = 300_000L;
    private static final double THREAD_CAPACITY = 200.0;
    private static final int FLAPPING_CHANGES = 3;

    private final RedisTemplate<String, Object> redisTemplate;
    private final TargetServiceProperties targetServiceProperties;
    private final SseService sseService;

    private final Map<String, RiskProfile> latestProfiles = new ConcurrentHashMap<>();
    private final Map<String, String> lastLevels = new ConcurrentHashMap<>();

    public RiskService(RedisTemplate<String, Object> redisTemplate,
                       TargetServiceProperties targetServiceProperties,
                       SseService sseService) {
        this.redisTemplate = redisTemplate;
        this.targetServiceProperties = targetServiceProperties;
        this.sseService = sseService;
    }

    public List<RiskProfile> getLatestProfiles() {
        return new ArrayList<>(latestProfiles.values());
    }

    @Scheduled(fixedRate = 10000, initialDelay = 15000)
    public void evaluateAll() {
        for (TargetServiceProperties.Target target : targetServiceProperties.getTargets()) {
            try {
                evaluate(target.getName());
            } catch (Exception e) {
                log.error("[{}] Risk evaluation failed: {}", target.getName(), e.getMessage());
            }
        }
    }

    private void evaluate(String serviceId) {
        long now = System.currentTimeMillis();

        Set<Object> metricsRaw = redisTemplate.opsForZSet()
                .rangeByScore("metrics:" + serviceId, now - METRICS_WINDOW_MS, now);
        Set<Object> healthRaw = redisTemplate.opsForZSet()
                .rangeByScore("health:" + serviceId, now - HEALTH_WINDOW_MS, now);

        double cpuRisk = 0, heapRisk = 0, threadRisk = 0;

        if (metricsRaw != null && !metricsRaw.isEmpty()) {
            double cpuSum = 0, heapPctSum = 0, threadSum = 0;
            int cpuCount = 0, heapCount = 0;

            for (Object obj : metricsRaw) {
                MetricsTick t = (MetricsTick) obj;
                threadSum += t.getActiveThreads();
                if (t.getCpuUsagePercent() >= 0) {
                    cpuSum += t.getCpuUsagePercent();
                    cpuCount++;
                }
                if (t.getHeapMaxMb() > 0) {
                    heapPctSum += (t.getHeapUsedMb() / t.getHeapMaxMb()) * 100.0;
                    heapCount++;
                }
            }

            if (cpuCount > 0) cpuRisk = scale(cpuSum / cpuCount, 50, 95);
            if (heapCount > 0) heapRisk = scale(heapPctSum / heapCount, 60, 95);
            threadRisk = scale((threadSum / metricsRaw.size()) / THREAD_CAPACITY * 100.0, 50, 90);
        }

        double availabilityRisk = 0;
        boolean flapping = false;
        String latestStatus = "UNKNOWN";

        if (healthRaw != null && !healthRaw.isEmpty()) {
            int down = 0, changes = 0;
            String previous = null;

            for (Object obj : healthRaw) {
                HealthTick h = (HealthTick) obj;
                if (!"UP".equals(h.getStatus())) down++;
                if (previous != null && !previous.equals(h.getStatus())) changes++;
                previous = h.getStatus();
            }

            latestStatus = previous;
            availabilityRisk = scale(down * 100.0 / healthRaw.size(), 0, 20);
            flapping = changes >= FLAPPING_CHANGES;
            if (flapping) availabilityRisk = Math.max(availabilityRisk, 80);
            if (!"UP".equals(latestStatus)) availabilityRisk = 100;
        }

        double score = Math.max(Math.max(cpuRisk, heapRisk), Math.max(threadRisk, availabilityRisk));
        String level = score >= 70 ? "HIGH" : (score >= 30 ? "MEDIUM" : "LOW");
        String dominant = dominantFactor(score, cpuRisk, heapRisk, threadRisk, availabilityRisk);

        RiskProfile profile = new RiskProfile();
        profile.setServiceId(serviceId);
        profile.setScore(round1(score));
        profile.setLevel(level);
        profile.setCpuRisk(round1(cpuRisk));
        profile.setHeapRisk(round1(heapRisk));
        profile.setThreadRisk(round1(threadRisk));
        profile.setAvailabilityRisk(round1(availabilityRisk));
        profile.setFlapping(flapping);
        profile.setDominantFactor(dominant);
        profile.setLatestStatus(latestStatus);
        profile.setTimestamp(now);

        latestProfiles.put(serviceId, profile);
        sseService.broadcast("risk", profile);

        String previousLevel = lastLevels.put(serviceId, level);
        if ("HIGH".equals(level) && !"HIGH".equals(previousLevel)) {
            log.warn("RISK INCIDENT OPENED: [{}] risk is HIGH (score {}), driven by {}",
                    serviceId, profile.getScore(), dominant);
        } else if (!"HIGH".equals(level) && "HIGH".equals(previousLevel)) {
            log.info("RISK INCIDENT CLOSED: [{}] risk is back to {} (score {})",
                    serviceId, level, profile.getScore());
        }
    }

    private double scale(double value, double comfort, double critical) {
        double risk = (value - comfort) / (critical - comfort) * 100.0;
        return Math.max(0, Math.min(100, risk));
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private String dominantFactor(double score, double cpu, double heap, double threads, double availability) {
        if (score <= 0) return "NONE";
        if (score == availability) return "AVAILABILITY";
        if (score == cpu) return "CPU";
        if (score == heap) return "HEAP";
        return "THREADS";
    }
}