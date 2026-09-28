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

    @Scheduled(fixedRate = 60000)
    public void aggregateAllTargets() {
        long windowEndMillis = System.currentTimeMillis();
        long windowStartMillis = windowEndMillis - 60000;

        for (TargetServiceProperties.Target target : targetServiceProperties.getTargets()) {
            aggregateOneTarget(target.getName(), windowStartMillis, windowEndMillis);
        }
    }

    private void aggregateOneTarget(String serviceId, long windowStartMillis, long windowEndMillis) {
        Set<Object> metricsRaw = redisTemplate.opsForZSet()
                .rangeByScore("metrics:" + serviceId, windowStartMillis, windowEndMillis);

        Set<Object> healthRaw = redisTemplate.opsForZSet()
                .rangeByScore("health:" + serviceId, windowStartMillis, windowEndMillis);

        if (metricsRaw == null || metricsRaw.isEmpty()) {
            log.warn("[{}] No metrics ticks found for this window, skipping rollup", serviceId);
            return;
        }

        double avgCpu = 0, maxCpu = Double.MIN_VALUE;
        double avgHeap = 0, maxHeap = Double.MIN_VALUE;

        for (Object obj : metricsRaw) {
            MetricsTick tick = (MetricsTick) obj;
            avgCpu += tick.getCpuUsagePercent();
            avgHeap += tick.getHeapUsedMb();
            maxCpu = Math.max(maxCpu, tick.getCpuUsagePercent());
            maxHeap = Math.max(maxHeap, tick.getHeapUsedMb());
        }
        avgCpu /= metricsRaw.size();
        avgHeap /= metricsRaw.size();

        double uptimePercent = 0;
        if (healthRaw != null && !healthRaw.isEmpty()) {
            long upCount = healthRaw.stream()
                    .filter(obj -> "UP".equals(((HealthTick) obj).getStatus()))
                    .count();
            uptimePercent = (upCount * 100.0) / healthRaw.size();
        }

        MetricRollupEntity rollup = new MetricRollupEntity();
        rollup.setServiceId(serviceId);
        rollup.setWindowStart(toLocalDateTime(windowStartMillis));
        rollup.setWindowSize("ONE_MIN");
        rollup.setAvgCpu(avgCpu);
        rollup.setMaxCpu(maxCpu);
        rollup.setAvgHeapUsedMb(avgHeap);
        rollup.setMaxHeapUsedMb(maxHeap);
        rollup.setUptimePercent(uptimePercent);

        metricRollupRepository.save(rollup);
        log.info("[{}] Saved rollup: avgCpu={}, maxCpu={}, avgHeap={}, uptime={}%",
                serviceId, avgCpu, maxCpu, avgHeap, uptimePercent);
    }

    private LocalDateTime toLocalDateTime(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDateTime();
    }
}