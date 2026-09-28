CREATE TABLE services (
    service_id VARCHAR(100) PRIMARY KEY,
    service_name VARCHAR(200) NOT NULL,
    base_url VARCHAR(300) NOT NULL,
    registered_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE metric_rollups (
    id BIGSERIAL PRIMARY KEY,
    service_id VARCHAR(100) NOT NULL REFERENCES services(service_id),
    window_start TIMESTAMP NOT NULL,
    window_size VARCHAR(20) NOT NULL,
    avg_cpu DOUBLE PRECISION,
    max_cpu DOUBLE PRECISION,
    avg_heap_used_mb DOUBLE PRECISION,
    max_heap_used_mb DOUBLE PRECISION,
    avg_latency_ms DOUBLE PRECISION,
    p95_latency_ms DOUBLE PRECISION,
    uptime_percent DOUBLE PRECISION
);

CREATE INDEX idx_metric_rollups_service_window ON metric_rollups (service_id, window_start);