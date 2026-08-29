1. A target microservice's Actuator endpoint holds live health/metrics data internally.

2\. The polling engine asynchronously calls each target service on a timer, without blocking on slow ones.

3\. Each successful poll result does two things at once: gets written into Redis as a raw tick (with TTL), and gets pushed live over WebSocket/SSE to any        connected dashboard.

4\. Separately, a scheduled job runs every minute, reads the last window of raw ticks out of Redis, computes averages/max/p95, and writes one summary row into PostgreSQL.

5\. When a user opens the dashboard and asks for historical data, that query hits PostgreSQL directly — not Redis, since raw data has already expired.

