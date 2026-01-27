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
Daily Scheduler (7 AM)
    │
    ├─► blogCrawlingJob
    │       ├─► Read active BlogSource entities
    │       ├─► Fetch RSS feeds (Rome)
    │       ├─► Filter posts < 7 days old
    │       ├─► Scrape full content (Jsoup)
    │       ├─► Summarize (OpenAI via Feign)
    │       └─► Save Post entities
    │
    └─► emailDeliveryJob (if crawling succeeds)
            ├─► Read active Subscriber entities
            ├─► Get today's Post IDs
            └─► Publish EmailRequest to RabbitMQ
                    │
                    └─► EmailConsumer (async)
                            ├─► Fetch Posts from DB
                            ├─► Render Thymeleaf template
                            └─► Send via SMTP
```

## Package Structure

```
server.morningcommit
├── domain/           # JPA Entities (BlogSource, Post, Subscriber, BaseEntity)
├── repository/       # Spring Data JPA Repositories
├── batch/            # Spring Batch Jobs (BlogCrawlingJob, EmailDeliveryJob)
├── scheduler/        # @Scheduled job orchestration
├── scraper/          # HtmlScraper (Jsoup)
├── ai/
│   ├── client/       # OpenAiClient (Feign)
│   ├── dto/          # ChatCompletion DTOs
│   └── service/      # SummaryService
├── email/
│   ├── dto/          # EmailRequest
│   ├── EmailService  # Thymeleaf + JavaMailSender
│   ├── EmailProducer # RabbitMQ publisher
│   └── EmailConsumer # RabbitMQ listener
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

## Key Components

### Batch Jobs
- **blogCrawlingJob**: Crawls RSS, scrapes content, summarizes, saves to DB
- **emailDeliveryJob**: Reads subscribers, creates EmailRequests, publishes to RabbitMQ

### RabbitMQ
- Queue: `email-queue`
- Exchange: `email-exchange` (Direct)
- Routing Key: `send-email`

### Scheduler
- Cron: `0 0 7 * * *` (Daily at 7 AM)
- Runs blogCrawlingJob first, then emailDeliveryJob on success

## Code Conventions

- Package: `server.morningcommit.*`
- JPA entities use `allOpen` plugin for `@Entity`, `@MappedSuperclass`, `@Embeddable`
- Kotlin strict JSR-305 null-safety mode enabled
