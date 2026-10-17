\# MicroPulse: Risk Management



\## 1. Approach

Risks were handled in four steps: identify, assess (likelihood and impact: Low/Medium/High), mitigate, and monitor. Risks R9 to R11 were not hypothetical. They occurred during development and were diagnosed and resolved. MicroPulse also acts as a risk monitor for the services it watches.



\## 2. Risk Register



| ID | Risk | Likelihood | Impact | Mitigation | Status |

|----|------|-----------|--------|-----------|--------|

| R1 | A monitored service crashes or stops responding | High | High | 3-second timeout on every poll; a failed health check is saved as a DOWN tick, streamed live, and counted in uptime % (`PollingService.recordHealth`, `RollupService`) | Implemented |

| R2 | Slow or hung services block monitor threads | Medium | High | Non-blocking WebClient with `Mono.zip`, so no thread waits on the network; timeout caps the wait | Implemented |

| R3 | Redis is unavailable | Medium | Medium | `saveToRedis` catches and logs failures so polling and live streaming continue; ticks during the outage are lost from the buffer (accepted) | Partially implemented |

| R4 | PostgreSQL is unavailable during a rollup | Medium | Medium | Per-service try/catch; the scheduler keeps running on the next cycle; missed windows are not back-filled | Partially implemented |

| R5 | Redis memory grows without limit | High | High | Every write trims ticks older than 15 minutes (`ZREMRANGEBYSCORE`) | Implemented |

| R6 | Database overloaded by high-frequency writes | High | High | Raw ticks buffered in Redis; only one summary row per service per window is written to PostgreSQL | Implemented |

| R7 | Invalid sensor readings (CPU = -1) corrupt averages | Medium | Low | Rollup and risk scoring ignore negative CPU readings | Implemented |

| R8 | Manual database changes cause schema drift | Medium | Medium | Versioned Flyway migrations (`V1\_\_create\_services\_and\_rollups\_tables.sql`) | Implemented |

| R9 | Port conflicts between native Windows services and Docker containers (occurred) | Medium | Medium | Native Redis/PostgreSQL services disabled; diagnosis with `netstat` and `Get-Service`; `start-containers.ps1` | Resolved |

| R10 | Framework version incompatibility (occurred: Spring Boot 4 moved Flyway auto-configuration into its own starter, so migrations silently did not run) | Medium | High | Check startup logs after adding each dependency; diagnosed with `dependency:tree` and the condition evaluation report; fixed with `spring-boot-starter-flyway` | Resolved |

| R11 | Timezone mismatch (Asia/Calcutta rejected by the PostgreSQL container) (occurred) | Medium | High | JVM timezone forced to UTC at startup | Resolved |

| R12 | Unauthorized access to monitoring data | Medium | High | JWT authentication with Spring Security | Planned (Phase 7) |

| R13 | Schedule and knowledge risk in a small team | High | Medium | Incremental phases, each tested and committed to Git/GitHub | Ongoing |

| R14 | A service is still UP but heading toward failure (high CPU, heap near its limit, thread build-up) | Medium | High | `RiskService` scores each service from 0 to 100 every 10 seconds (LOW/MEDIUM/HIGH), streams `risk` events, exposes `/api/risk`, and logs an incident when a service becomes HIGH | Implemented |

| R15 | A service keeps going UP and DOWN (unstable) | Medium | High | Flapping detection: three or more status changes in 5 minutes raises availability risk to at least 80 | Implemented |



\## 3. Risk Scoring Model

Every 10 seconds each service is scored on four factors, each from 0 (no risk) to 100 (critical):

\- \*\*CPU:\*\* average over the last minute; 0 risk below 50%, full risk at 95%.

\- \*\*Heap:\*\* used divided by maximum heap, averaged over the last minute; 0 risk below 60%, full risk at 95%.

\- \*\*Threads:\*\* live threads against a reference capacity of 200; 0 risk below 50% of capacity, full risk at 90%.

\- \*\*Availability:\*\* share of failed health checks in the last 5 minutes (full risk at 20%); a DOWN status now means 100; flapping means at least 80.



The service's score is the \*\*highest\*\* of the four factors, because a weighted average would hide a single critical problem. Below 30 is LOW, 30 to 69 is MEDIUM, and 70 or more is HIGH.



\## 4. Residual Risks and Known Limitations

\- Ticks that arrive while Redis is down are not recovered.

\- Rollup windows missed during a PostgreSQL outage are not back-filled.

\- HTTP latency is not collected, so `avg\_latency\_ms` and `p95\_latency\_ms` remain empty (future scope).

\- CPU is host-wide (`system.cpu.usage`), so services on the same machine show similar values; per-service CPU (`process.cpu.usage`) is future work.

\- The thread factor uses a reference capacity of 200, which is an assumption and not a measured pool size.

\- Risk incidents are logged and streamed live but not stored in the database.

\- No external alert delivery (Slack, email and similar); this is listed as out of scope in the project synopsis.

\- No automatic corrective action such as a circuit breaker, because MicroPulse observes services and does not sit in their request path.

\- The monitoring engine is a single instance, so it is itself a single point of failure.

\- API endpoints are unauthenticated until R12 is implemented, which is acceptable for local development only.



\## 5. Demonstration

\- \*\*Outage (R1, R14):\*\* stop `auth-service` while the live stream is open. A DOWN event appears within seconds, its risk level becomes HIGH with availability as the dominant factor, `payment-service` is unaffected, and `auth-service` recovers automatically when restarted.

\- \*\*Instability (R15):\*\* stop and start `auth-service` repeatedly; after three status changes the risk profile shows `flapping: true`.

