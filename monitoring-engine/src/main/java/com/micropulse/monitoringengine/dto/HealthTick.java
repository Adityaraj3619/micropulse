package com.micropulse.monitoringengine.dto;

public class HealthTick {

    private String serviceId;
    private String status;
    private long timestamp;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "HealthTick{serviceId=" + serviceId + ", status=" + status + ", timestamp=" + timestamp + "}";
    }
}