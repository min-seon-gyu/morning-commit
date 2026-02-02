# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "server.morningcommit.RssParsingTest"

# Clean build
./gradlew clean build
```

## Project Overview

MorningCommit is a daily tech blog newsletter service that:
1. Crawls RSS feeds from tech blogs
2. Scrapes full article content
3. Summarizes using OpenAI GPT
4. Delivers personalized newsletters via email
5. Tracks link clicks for analytics

## Tech Stack

- **Kotlin 1.9.25** with Java 21
- **Spring Boot 3.4.2**, Spring Cloud 2024.0.0
- **Spring Batch 5** for scheduled batch processing
- **MySQL** with Spring Data JPA
- **RabbitMQ** for async email delivery
- **Thymeleaf** for email templates
- **OpenFeign** for OpenAI API integration
- **Rome** for RSS/Atom parsing
- **Jsoup** for HTML scraping

## Architecture

```
blogCrawlingJob (Daily at 1 AM)
    │
    ├─► Read active BlogSource entities
    ├─► Fetch RSS feeds (Rome)
    ├─► Filter recent posts
    ├─► Scrape full content (Jsoup)
    ├─► Summarize (OpenAI via Feign)
    └─► Batch save Post entities (pre-filtered duplicates via findExistingLinks)

emailDeliveryJob (Daily at 7 AM)
    │
    ├─► Read active Subscriber entities
    ├─► Shuffle-and-Deplete Post Selection:
    │       ├─► Fetch all Post IDs
    │       ├─► Get sent Post IDs from PostSendHistory
    │       ├─► Calculate candidates (all - sent)
    │       ├─► If empty: Reset history, use all posts
    │       ├─► Random select one post
    │       └─► Save to PostSendHistory
    └─► Publish EmailRequest to RabbitMQ
            │
            └─► EmailConsumer (async)
                    ├─► Fetch Post from DB
                    ├─► Transform links to tracking URLs
                    ├─► Render Thymeleaf template (Korean)
                    └─► Send via SMTP

Click Tracking Flow
    │
    User clicks tracked link in email
    │
    └─► GET /track?url={encodedUrl}&subscriberId={id}
            ├─► Publish ClickLogEvent to RabbitMQ
            ├─► Redirect to original URL (302)
            │
            └─► TrackingConsumer (async)
                    └─► Save ClickLog entity to DB
```

## Package Structure

```
server.morningcommit
├── domain/           # JPA Entities (BlogSource, Post, Subscriber, ClickLog, PostSendHistory, BaseEntity)
├── repository/       # Spring Data JPA Repositories
├── batch/            # Spring Batch Jobs (BlogCrawlingJob, EmailDeliveryJob)
├── scheduler/        # @Scheduled job orchestration
├── scraper/          # HtmlScraper (Jsoup)
├── controller/       # Web Controllers (ViewController, TrackingController)
├── ai/
│   ├── client/       # OpenAiClient (Feign)
│   ├── dto/          # ChatCompletion DTOs
│   └── service/      # SummaryService
├── email/
│   ├── dto/          # EmailRequest, ClickLogEvent, TrackedPost
│   ├── EmailService  # Thymeleaf + JavaMailSender
│   ├── EmailProducer # RabbitMQ publisher
│   ├── EmailConsumer # RabbitMQ listener
│   └── TrackingConsumer # Click tracking listener
└── config/           # Spring configurations
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `db.url` | `jdbc:mysql://localhost:3306/morningcommit` | MySQL connection URL |
| `db.username` | `root` | MySQL username |
| `db.password` | `1234` | MySQL password |
| `OPENAI_API_KEY` | - | OpenAI API key for summarization |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `RABBITMQ_PORT` | `5672` | RabbitMQ port |
| `RABBITMQ_USERNAME` | `guest` | RabbitMQ username |
| `RABBITMQ_PASSWORD` | `guest` | RabbitMQ password |
| `MAIL_HOST` | `smtp.naver.com` | SMTP server |
| `MAIL_PORT` | `465` | SMTP port |
| `MAIL_USERNAME` | - | SMTP username |
| `MAIL_PASSWORD` | - | SMTP password |
| `app.tracking.base-url` | `http://localhost:18080/track` | Base URL for click tracking |

## Key Components

### Batch Jobs
- **blogCrawlingJob**: Crawls RSS, scrapes content, summarizes, batch saves to DB (duplicates pre-filtered)
- **emailDeliveryJob**: Reads subscribers, selects random post (shuffle-and-deplete), publishes to RabbitMQ

### Shuffle-and-Deplete Algorithm
Each subscriber receives one random post per day without duplicates until all posts are sent:
1. Fetch all Post IDs from database
2. Get already-sent Post IDs for user from `PostSendHistory`
3. Calculate candidates = all IDs - sent IDs
4. If candidates empty → delete user's history (reset cycle) → use all posts
5. Randomly select one post from candidates
6. Save selection to `PostSendHistory`

### RabbitMQ
- Exchange: `email-exchange` (Direct)
- Queues:
  - `email-queue` (Routing Key: `send-email`) - Email delivery
  - `tracking-queue` (Routing Key: `tracking-log`) - Click tracking

### Scheduler
- `blogCrawlingJob`: `0 0 1 * * *` (Daily at 1 AM)
- `emailDeliveryJob`: `0 0 7 * * *` (Daily at 7 AM)

### Web UI
- `GET /` - Post listing with pagination (9 items/page) and blog filtering
- Uses Thymeleaf + Tailwind CSS

### Click Tracking
- `GET /track?url={encodedUrl}&subscriberId={id}` - Tracks click and redirects (302)
- Redirect URL is validated against known Post links in DB (prevents open redirect)
- Links in newsletter emails are wrapped with tracking URLs
- Click events stored in `ClickLog` entity for analytics

## Code Conventions

- Package: `server.morningcommit.*`
- JPA entities use `allOpen` plugin for `@Entity`, `@MappedSuperclass`, `@Embeddable`
- Kotlin strict JSR-305 null-safety mode enabled

### Layered Responsibility

- Business rules and domain-level validations must be implemented in the Service layer.
  - Examples: state transition validation, duplication checks, policy decisions, permission-based logic
- The API (Controller) layer is responsible only for the following:
  - Request parameter binding
  - Input format validation (e.g., @NotNull, @Size)
  - Authentication and authorization
  - Invoking Services and mapping responses
