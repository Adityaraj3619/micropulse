\# MicroPulse - Telemetry Schema





\## Health Tick Payload



&#x09;1. serviceId (String): Identifies which target service this tick belongs to — every downstream consumer (Redis key, Postgres row, dashboard) keys 			       off this.

&#x09;2. serviceName (String): Human-readable label for the dashboard, separate from the ID so you can rename services without breaking keys.

&#x09;3. status (Enum: UP / DOWN / DEGRADED): Comes straight from Actuator's health endpoint — DEGRADED is your own interpretation layer (e.g., 							"responding but slow") since Actuator itself is usually just UP/DOWN.

&#x09;4. timestamp (epoch millis): Needed for TTL calculation, rollup windowing, and chart X-axis — store as epoch, not a formatted string, so all 					     downstream math is simple integer comparison.

&#x09;5. responseTimeMs (long): How long the actuator call itself took — this is your polling engine's own measurement, not something Actuator reports.





\## Metrics Tick Payload



&#x09;1. serviceId (String): Same join key as above.

&#x09;2. timestamp (epoch millis): Same reasoning as above.

&#x09;3. cpuUsagePercent (double): From Actuator's system.cpu.usage metric.

&#x09;4. heapUsedMb (double): From jvm.memory.used — this is your "is it about to OOM" signal.

&#x09;5. heapMaxMb (double): Needed alongside heapUsedMb to compute a percentage — storing both instead of just a ratio means you can recompute if your 			       definition of "healthy %" changes later.

&#x09;6. activeThreads (int): From jvm.threads.live — a rising number here without a rising load is a leak signal.

&#x09;7. httpLatencyAvgMs (double): From http.server.requests — average request latency since last poll.

&#x09;8. httpLatencyP95Ms (double): Same source, p95 — this is what you'll actually alert on, since averages hide spikes.





Note: Why two separate payload types instead of one combined object — health checks are cheap and should run frequently; full metrics collection is heavier. Keeping them separate lets you poll health every few seconds but metrics on a slightly longer interval if you ever need to tune for load — and it maps cleanly onto Actuator's own separate /health and /metrics endpoints.



\## Redis Key Convention



&#x09;1. Key Structure (metrics:{serviceId}): Use one Redis Sorted Set key per service rather than individual per-tick keys.

&#x09;2. Score (epoch millis): Store the payload's timestamp as the score to enable fast time-range querying.

&#x09;3. Member (JSON Payload): Store the serialized tick payload as the sorted set member value.

&#x09;4. Performance Advantage (ZRANGEBYSCORE): Plain per-tick keys require key scanning to fetch range data, whereas Sorted Sets allow ZRANGEBYSCORE to 						  pull all ticks between time A and time B in a single efficient command.

&#x09;5. Retention \& Cleanup (ZREMRANGEBYSCORE): TTL and memory management are handled via periodic trimming using ZREMRANGEBYSCORE or key expiry 							   mechanisms, shielding Postgres from raw I/O overhead.





\## PostgreSQL Rollup Schema





Table: services (Registry Table):

&#x09;1. service\_id (PK, String): Primary identifier for the registered service.

&#x09;2. service\_name (String): Display name of the service.

&#x09;3. base\_url (String): Target HTTP endpoint for health and metrics polling.

&#x09;4. registered\_at (timestamp): Registration timestamp for audit tracking.



Table: metric\_rollups:

&#x09;1. id (PK, BigInt/UUID): Unique record identifier for each aggregated rollup entry.

&#x09;2. service\_id (FK -> services): Foreign key linking back to the target service.

&#x09;3. window\_start (timestamp): Start time of the aggregation window.

&#x09;4. window\_size (Enum: ONE\_MIN / FIVE\_MIN): Duration window for aggregated datapoints.

&#x09;5. avg\_cpu (double): Average CPU usage percentage over the window.

&#x09;6. max\_cpu (double): Peak CPU usage percentage over the window.

&#x09;7. avg\_heap\_used\_mb (double): Average JVM heap usage in megabytes over the window.

&#x09;8. max\_heap\_used\_mb (double): Peak JVM heap usage in megabytes over the window.

&#x09;9. avg\_latency\_ms (double): Average HTTP request latency over the window.

&#x09;10. p95\_latency\_ms (double): 95th percentile HTTP request latency over the window.

&#x09;11. uptime\_percent (double): Fraction of health ticks in this window that reported status as UP.

&#x09;12. Index (Composite Index on service\_id, window\_start): Composite index to optimize frequent historical query lookups from the dashboard API.







