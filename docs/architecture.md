# Notification Service - Architecture Notes

## Why I Built This

I built this project to get hands-on experience with event-driven architecture using Spring Boot and Kafka.

Most CRUD applications process everything synchronously. I wanted to understand how real systems decouple request handling from background processing, so I used Kafka as a messaging layer between notification creation and notification processing.

The project also gave me an opportunity to work with:

- Spring Security + JWT
- Kafka producers and consumers
- Redis caching
- Docker-based local setup
- Layered Spring Boot architecture

---

## How the System Works

When a user creates a notification, the request follows this flow:

1. API receives the request.
2. Notification data is stored in the database.
3. An event is published to Kafka.
4. A Kafka consumer reads the event.
5. Notification processing is performed.
6. Notification status is updated.

The main idea is that the API should not be responsible for performing all processing synchronously.

---

## Request Flow

```text
Client
   |
   v
Notification API
   |
   v
Database Save
   |
   v
Kafka Producer
   |
   v
Kafka Topic
   |
   v
Kafka Consumer
   |
   v
Status Update
```

---

## Security Flow

Protected endpoints require a valid JWT token.

```text
Login Request
      |
      v
Authentication
      |
      v
JWT Token Generated
      |
      v
Client Stores Token
      |
      v
Token Sent In Requests
      |
      v
Spring Security Validation
```

---

## Design Choices

### Persist Before Publishing

I chose to save notifications before publishing events to Kafka.

This ensures the notification request is recorded even if Kafka publishing or downstream processing fails.

Without persistence, a failure during event publishing could result in lost notification requests.

### Why Kafka?

The primary reason was to learn asynchronous communication between services.

Kafka also provides:

- Loose coupling
- Better scalability
- Separation between API and processing logic

For this project, Kafka is probably more powerful than strictly necessary, but learning it was one of the goals.

### Why JWT?

JWT keeps authentication stateless and integrates well with REST APIs.

It also helped me understand how Spring Security filters work internally.

---

## Current Limitations

While building the project, I noticed a few areas that could be improved:

- Failed notifications are not retried automatically.
- There is no Dead Letter Queue (DLQ).
- Notification delivery channels are limited.
- Monitoring and metrics are not implemented.
- Integration test coverage can be expanded.

---

## Possible Future Enhancements

### Retry Mechanism

Instead of marking notifications as failed immediately, retry processing a few times before giving up.

### Dead Letter Queue

Move repeatedly failing messages into a separate Kafka topic for investigation.

### Analytics Endpoint

Provide statistics such as:

- Total notifications
- Sent notifications
- Failed notifications

### Monitoring

Add metrics and dashboards using tools such as Prometheus and Grafana.

---

## What I Learned

This project helped me understand:

- Producer-consumer communication using Kafka
- Spring Security authentication flow
- JWT-based authorization
- Asynchronous processing patterns
- Organizing a Spring Boot application using layered architecture

The biggest takeaway was understanding how event-driven systems differ from traditional request-response applications.