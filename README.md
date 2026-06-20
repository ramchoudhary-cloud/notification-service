# Notification Service

A backend microservice for sending and tracking push notifications asynchronously.
Built with Java 17, Spring Boot, Kafka, Redis, MySQL and JWT-based auth.

---

## What it does

Clients authenticate via JWT and POST a notification request. The service saves it to MySQL
as PENDING, publishes an event to Kafka, and a consumer picks it up asynchronously to deliver
it — updating the status to SENT or FAILED. Redis caches per-user notification history to
reduce DB load on repeated reads.

---

## Tech Stack

| Layer         | Tech                          |
|---------------|-------------------------------|
| Language      | Java 17                       |
| Framework     | Spring Boot 3.2               |
| Auth          | Spring Security + JWT (jjwt)  |
| Messaging     | Apache Kafka                  |
| Cache         | Redis                         |
| Database      | MySQL + Spring Data JPA       |
| Build         | Gradle                        |
| Testing       | JUnit 5 + Mockito             |

---

## Project Structure

```
src/main/java/com/ram/notificationservice/
├── controller/     # REST endpoints - thin layer, no business logic
├── service/        # Business logic (NotificationService, AuthService)
├── producer/       # Kafka event publisher
├── consumer/       # Kafka event consumer (async delivery)
├── repository/     # JPA repositories
├── model/          # DB entities (Notification, User, enums)
├── dto/            # Request/Response/Event objects
├── security/       # JWT filter + CustomUserDetailsService
├── config/         # Kafka topic + Security config
├── exception/      # Custom exceptions + global handler
└── util/           # JwtUtil
```

---

## API Endpoints

### Auth
```
POST /api/v1/auth/register   - register a new user, returns JWT
POST /api/v1/auth/login      - login, returns JWT
```

### Notifications (requires Bearer token)
```
POST   /api/v1/notifications              - send notification (async)
GET    /api/v1/notifications/{id}         - get by ID
GET    /api/v1/notifications/user/{userId}- get all for a user (Redis cached)
GET    /api/v1/notifications/status/{status} - ADMIN only
```

---

## Running Locally

**Prerequisites:** Java 17, Gradle, MySQL, Kafka, Redis

```bash
# 1. Start infrastructure (easiest with Docker)
docker run -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=notification_db mysql:8
docker run -d -p 6379:6379 redis:alpine
docker run -d -p 9092:9092 apache/kafka:latest

# 2. Update credentials in application.properties if different

# 3. Run
./gradlew bootRun

# 4. Run tests (no infrastructure needed - uses H2 + mocks)
./gradlew test
```

---

## Sample Requests

```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"ram","password":"pass123"}'

# Send notification (use token from above)
curl -X POST http://localhost:8080/api/v1/notifications \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user42",
    "title": "Weekend Deals!",
    "message": "Up to 60% off this weekend only.",
    "type": "DEAL_ALERT"
  }'
```

---

## Design Decisions & Challenges

### Why save to DB before publishing to Kafka?
Early on I had it the other way — publish to Kafka first, save to DB after.
During testing I realized if the service crashes between publish and save, we'd have
a Kafka event with no corresponding DB record. Flipping the order means we always
have a record even if Kafka publish fails — we can retry later.

### Why Redis caching on getByUser?
In a notification system, users or dashboards poll frequently to check delivery status.
Without caching, every poll hits MySQL. Redis with a 1-hour TTL handles this well.
The cache is evicted whenever a new notification is sent to that user, so it stays
reasonably fresh. Tradeoff: status might be slightly stale within the TTL window — acceptable
for a history view but I wouldn't cache the status-by-status admin endpoint.

### Why userId as Kafka message key?
Kafka routes all messages with the same key to the same partition. This means all
notifications for a given user are processed in order — useful if order matters
(e.g. don't send re-engagement before the deal alert that triggered it).

### JWT in Authorization header vs cookies?
Chose header-based JWT since this is a REST API likely consumed by mobile/backend clients,
not browsers. Cookies make more sense for browser apps. No refresh token yet — planned next.

### retryCount field on Notification
Initially had no way to know how many times delivery was attempted for a FAILED notification.
Added retryCount so a future retry job can skip ones that have already failed 3+ times
and route them to a dead letter queue instead. DLQ not implemented yet but the foundation is there.

---

## How to Scale This

*These are the questions I thought through while building:*

**High notification volume (millions/day)**
- Increase Kafka partitions (currently 3) and add more consumer instances
- Each consumer instance handles one partition — linear horizontal scaling
- Use consumer groups to distribute load

**DB bottleneck**
- Add read replica for SELECT queries (getByUser, getByStatus)
- Partition the notifications table by userId or date range
- Archive old notifications to cold storage

**Redis at scale**
- Redis already handles the read-heavy getByUser load
- Could extend caching to notification status checks per ID
- Use Redis Cluster for high availability

**Auth at scale**
- JWT is stateless so auth scales horizontally with no shared state
- Add refresh tokens to avoid frequent re-logins
- Rate-limit the login endpoint to prevent brute force

**Monitoring**
- Add Kafka consumer lag metrics — high lag = consumers falling behind
- Alert on FAILED notification ratio going above threshold
- Expose health endpoint for infrastructure checks
