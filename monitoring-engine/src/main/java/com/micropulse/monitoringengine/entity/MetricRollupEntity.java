package com.micropulse.monitoringengine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "metric_rollups")
public class MetricRollupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_id")
    private String serviceId;

    @Column(name = "window_start")
    private LocalDateTime windowStart;

    @Column(name = "window_size")
    private String windowSize;

    @Column(name = "avg_cpu")
    private Double avgCpu;

    @Column(name = "max_cpu")
    private Double maxCpu;

    @Column(name = "avg_heap_used_mb")
    private Double avgHeapUsedMb;

    @Column(name = "max_heap_used_mb")
    private Double maxHeapUsedMb;

    @Column(name = "uptime_percent")
    private Double uptimePercent;

    public Long getId() {
        return id;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public LocalDateTime getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(LocalDateTime windowStart) {
        this.windowStart = windowStart;
    }

    public String getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(String windowSize) {
        this.windowSize = windowSize;
    }

    public Double getAvgCpu() {
        return avgCpu;
    }

    public void setAvgCpu(Double avgCpu) {
        this.avgCpu = avgCpu;
    }

    public Double getMaxCpu() {
        return maxCpu;
    }

    public void setMaxCpu(Double maxCpu) {
        this.maxCpu = maxCpu;
    }

    public Double getAvgHeapUsedMb() {
        return avgHeapUsedMb;
    }

    public void setAvgHeapUsedMb(Double avgHeapUsedMb) {
        this.avgHeapUsedMb = avgHeapUsedMb;
    }

    public Double getMaxHeapUsedMb() {
        return maxHeapUsedMb;
    }

    public void setMaxHeapUsedMb(Double maxHeapUsedMb) {
        this.maxHeapUsedMb = maxHeapUsedMb;
    }

    public Double getUptimePercent() {
        return uptimePercent;
    }

    public void setUptimePercent(Double uptimePercent) {
        this.uptimePercent = uptimePercent;
    }
}