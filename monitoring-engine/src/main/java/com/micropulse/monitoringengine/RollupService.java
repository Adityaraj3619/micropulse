package com.micropulse.monitoringengine;

import com.micropulse.monitoringengine.config.TargetServiceProperties;
import com.micropulse.monitoringengine.dto.HealthTick;
import com.micropulse.monitoringengine.dto.MetricsTick;
import com.micropulse.monitoringengine.entity.MetricRollupEntity;
import com.micropulse.monitoringengine.repository.MetricRollupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;

@Component
public class RollupService {

    private static final Logger log = LoggerFactory.getLogger(RollupService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final TargetServiceProperties targetServiceProperties;
    private final MetricRollupRepository metricRollupRepository;

    public RollupService(RedisTemplate<String, Object> redisTemplate,
                         TargetServiceProperties targetServiceProperties,
                         MetricRollupRepository metricRollupRepository) {
        this.redisTemplate = redisTemplate;
        this.targetServiceProperties = targetServiceProperties;
        this.metricRollupRepository = metricRollupRepository;
    }

    @Scheduled(fixedRate = 60000, initialDelay = 60000)
    public void aggregateOneMinute() {
        aggregateAllTargets(60_000L, "ONE_MIN");
    }

    @Scheduled(fixedRate = 300000, initialDelay = 300000)
    public void aggregateFiveMinutes() {
        aggregateAllTargets(300_000L, "FIVE_MIN");
    }

    private void aggregateAllTargets(long windowMillis, String windowLabel) {
        long windowEndMillis = System.currentTimeMillis();
        long windowStartMillis = windowEndMillis - windowMillis;

        for (TargetServiceProperties.Target target : targetServiceProperties.getTargets()) {
            try {
                aggregateOneTarget(target.getName(), windowStartMillis, windowEndMillis, windowLabel);
            } catch (Exception e) {
                log.error("[{}] {} rollup failed, continuing: {}", target.getName(), windowLabel, e.getMessage());
            }
        }
    }

    private void aggregateOneTarget(String serviceId, long windowStartMillis, long windowEndMillis, String windowLabel) {
        Set<Object> metricsRaw = redisTemplate.opsForZSet()
                .rangeByScore("metrics:" + serviceId, windowStartMillis, windowEndMillis);
        Set<Object> healthRaw = redisTemplate.opsForZSet()
                .rangeByScore("health:" + serviceId, windowStartMillis, windowEndMillis);

        boolean noMetrics = metricsRaw == null || metricsRaw.isEmpty();
        boolean noHealth = healthRaw == null || healthRaw.isEmpty();

        if (noMetrics && noHealth) {
            log.warn("[{}] No ticks found for this {} window, skipping rollup", serviceId, windowLabel);
            return;
        }

        MetricRollupEntity rollup = new MetricRollupEntity();
        rollup.setServiceId(serviceId);
        rollup.setWindowStart(toLocalDateTime(windowStartMillis));
        rollup.setWindowSize(windowLabel);

        if (!noMetrics) {
            double cpuSum = 0, heapSum = 0, maxCpu = 0, maxHeap = 0;
            int cpuCount = 0;

            for (Object obj : metricsRaw) {
                MetricsTick tick = (MetricsTick) obj;
                heapSum += tick.getHeapUsedMb();
                maxHeap = Math.max(maxHeap, tick.getHeapUsedMb());

                if (tick.getCpuUsagePercent() >= 0) {
                    cpuSum += tick.getCpuUsagePercent();
                    maxCpu = Math.max(maxCpu, tick.getCpuUsagePercent());
                    cpuCount++;
                }
            }

            rollup.setAvgHeapUsedMb(heapSum / metricsRaw.size());
            rollup.setMaxHeapUsedMb(maxHeap);
            if (cpuCount > 0) {
                rollup.setAvgCpu(cpuSum / cpuCount);
                rollup.setMaxCpu(maxCpu);
            }
        }

        if (!noHealth) {
            long upCount = healthRaw.stream()
                    .filter(obj -> "UP".equals(((HealthTick) obj).getStatus()))
                    .count();
            rollup.setUptimePercent((upCount * 100.0) / healthRaw.size());
        }

        metricRollupRepository.save(rollup);
        log.info("[{}] Saved {} rollup: avgCpu={}, maxCpu={}, avgHeap={}, uptime={}%",
                serviceId, windowLabel, rollup.getAvgCpu(), rollup.getMaxCpu(),
                rollup.getAvgHeapUsedMb(), rollup.getUptimePercent());
    }

    private LocalDateTime toLocalDateTime(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDateTime();
    }
}