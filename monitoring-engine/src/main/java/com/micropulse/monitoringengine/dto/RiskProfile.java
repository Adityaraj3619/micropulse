package com.micropulse.monitoringengine.dto;

public class RiskProfile {

    private String serviceId;
    private double score;
    private String level;
    private double cpuRisk;
    private double heapRisk;
    private double threadRisk;
    private double availabilityRisk;
    private boolean flapping;
    private String dominantFactor;
    private String latestStatus;
    private long timestamp;

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public double getCpuRisk() { return cpuRisk; }
    public void setCpuRisk(double cpuRisk) { this.cpuRisk = cpuRisk; }

    public double getHeapRisk() { return heapRisk; }
    public void setHeapRisk(double heapRisk) { this.heapRisk = heapRisk; }

    public double getThreadRisk() { return threadRisk; }
    public void setThreadRisk(double threadRisk) { this.threadRisk = threadRisk; }

    public double getAvailabilityRisk() { return availabilityRisk; }
    public void setAvailabilityRisk(double availabilityRisk) { this.availabilityRisk = availabilityRisk; }

    public boolean isFlapping() { return flapping; }
    public void setFlapping(boolean flapping) { this.flapping = flapping; }

    public String getDominantFactor() { return dominantFactor; }
    public void setDominantFactor(String dominantFactor) { this.dominantFactor = dominantFactor; }

    public String getLatestStatus() { return latestStatus; }
    public void setLatestStatus(String latestStatus) { this.latestStatus = latestStatus; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}