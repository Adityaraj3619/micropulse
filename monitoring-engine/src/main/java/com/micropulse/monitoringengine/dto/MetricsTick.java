package com.micropulse.monitoringengine.dto;

public class MetricsTick {

    private String serviceId;
    private long timestamp;
    private double cpuUsagePercent;
    private double heapUsedMb;
    private int activeThreads;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getCpuUsagePercent() {
        return cpuUsagePercent;
    }

    public void setCpuUsagePercent(double cpuUsagePercent) {
        this.cpuUsagePercent = cpuUsagePercent;
    }

    public double getHeapUsedMb() {
        return heapUsedMb;
    }

    public void setHeapUsedMb(double heapUsedMb) {
        this.heapUsedMb = heapUsedMb;
    }

    public int getActiveThreads() {
        return activeThreads;
    }

    public void setActiveThreads(int activeThreads) {
        this.activeThreads = activeThreads;
    }

    @Override
    public String toString() {
        return "MetricsTick{serviceId=" + serviceId +
                ", timestamp=" + timestamp +
                ", cpu=" + cpuUsagePercent +
                ", heapMb=" + heapUsedMb +
                ", threads=" + activeThreads + "}";
    }
}