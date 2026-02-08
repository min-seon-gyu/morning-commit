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

# Docker (all infrastructure services)
docker-compose up -d
```

## Project Overview

MorningCommit is a daily tech blog newsletter service that:
1. Crawls RSS feeds from tech blogs
2. Scrapes full article content
3. Summarizes using OpenAI GPT (filters out promotional content)
4. Indexes posts into Elasticsearch for full-text search
5. Delivers personalized newsletters via email
6. Tracks link clicks for analytics
7. Supports email-verified subscriber signup

## Tech Stack

- **Kotlin 1.9.25** with Java 21
- **Spring Boot 3.4.2**, Spring Cloud 2024.0.0
- **Spring Batch 5** for scheduled batch processing
- **MySQL** with Spring Data JPA
- **RabbitMQ** for async email delivery and click tracking
- **Thymeleaf** for email templates and web UI
- **OpenFeign** for OpenAI API integration
- **Rome** for RSS/Atom parsing
- **Jsoup** for HTML scraping
- **Redis** for caching (dashboard, post listings, search results) and email verification codes
- **Elasticsearch 8.12.0** for full-text post search

## Architecture

```
blogCrawlingJob (Daily at 1 AM)
    │
    ├─► Read active BlogSource entities
    ├─► Fetch RSS feeds (Rome)
    ├─► Filter recent posts
    ├─► Scrape full content (Jsoup)
    ├─► Summarize & analyze (OpenAI via Feign)
    │       └─► Promotional content filtered out
    ├─► Batch save Post entities (pre-filtered duplicates via findExistingLinks)
    └─► Index saved posts to Elasticsearch

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
                    ├─► Save ClickLog entity to DB
                    └─► Clear analytics cache

Email Verification Flow
    │
    ├─► POST /api/subscribers/send-verification
    │       └─► Generate 6-digit code → Store in Redis (5-min TTL) → Send via email
    └─► POST /api/subscribers/verify
            └─► Validate code from Redis → Create active Subscriber
```

## Package Structure

```
server.morningcommit
├── domain/           # JPA Entities (BlogSource, Post, Subscriber, ClickLog, PostSendHistory, BaseEntity)
│                     # Blog enum, PostDocument (Elasticsearch)
├── repository/       # Spring Data JPA Repositories, PostSearchRepository (Elasticsearch)
├── batch/            # Spring Batch Jobs (BlogCrawlingJob, EmailDeliveryJob)
├── scheduler/        # @Scheduled job orchestration (JobScheduler)
├── scraper/          # HtmlScraper (Jsoup)
├── controller/
│   ├── dto/          # SendVerificationRequest, VerifyRequest
│   ├── ViewController       # Web UI (posts, analytics)
│   ├── TrackingController   # Click tracking redirect
│   ├── SearchController     # Elasticsearch search endpoint
│   └── SubscriberController # Email verification & subscription management
├── ai/
│   ├── client/       # OpenAiClient (Feign)
│   ├── dto/          # ChatCompletion DTOs
│   └── service/
│       ├── SummaryService        # OpenAI summarization + promotional analysis
│       └── dto/BlogAnalysisResult # Summary, tags, difficulty, isPromotional
├── email/
│   ├── dto/          # EmailRequest, ClickLogEvent, TrackedPost
│   ├── EmailService  # Thymeleaf + JavaMailSender
│   ├── EmailProducer # RabbitMQ publisher
│   ├── EmailConsumer # RabbitMQ listener
│   └── TrackingConsumer # Click tracking listener (clears analytics cache)
├── service/          # AnalyticsService, TrackingService, PostService,
│   │                 # PostSearchService, SubscriberService, BlogSourceService
│   └── dto/          # PostClickCount, BlogClickCount, DailyClickCount, AnalyticsDashboard
└── config/           # RabbitMqConfig, RedisConfig, JpaConfig, FeignConfig,
                      # ElasticsearchConfig, SchedulingConfig, StringListConverter, RestPage
```

## Supported Blogs (Blog Enum)

KAKAO_TECH, KAKAO_PAY, TOSS_TECH, WOOWA_BROS, LINE_ENGINEERING, HYPERCONNECT_TECH, KURLY, SOCAR, OLIVE_YOUNG, BANKSALAD, DEV_SISTERS

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `db.url` | `jdbc:mysql://localhost:13306/morningcommit` | MySQL connection URL |
| `db.username` | `root` | MySQL username |
| `db.password` | `1234` | MySQL password |
| `OPENAI_API_KEY` | - | OpenAI API key for summarization |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `RABBITMQ_PORT` | `15673` | RabbitMQ port |
| `RABBITMQ_USERNAME` | `guest` | RabbitMQ username |
| `RABBITMQ_PASSWORD` | `guest` | RabbitMQ password |
| `MAIL_HOST` | `smtp.naver.com` | SMTP server |
| `MAIL_PORT` | `465` | SMTP port |
| `MAIL_USERNAME` | - | SMTP username |
| `MAIL_PASSWORD` | - | SMTP password |
| `app.base-url` | `http://localhost:18080` | Base URL for click tracking and unsubscribe links |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `16379` | Redis port |
| `ELASTICSEARCH_HOST` | `localhost` | Elasticsearch host |
| `ELASTICSEARCH_PORT` | `19200` | Elasticsearch port |

## Key Components

### Batch Jobs
- **blogCrawlingJob**: Crawls RSS, scrapes content, summarizes (filters promotional), batch saves to DB, indexes to Elasticsearch
- **emailDeliveryJob**: Reads subscribers, selects random post (shuffle-and-deplete), publishes to RabbitMQ

### Promotional Content Filtering
During blog crawling, OpenAI analyzes each article and flags promotional content. Posts marked as promotional are filtered out before saving. Promotional content includes:
- Recruitment posts, events/hackathons, product recommendations
- Company culture posts, research showcases without implementation, marketing
- Only technical content (teaching technology, architecture, algorithms, practices) is kept

### Shuffle-and-Deplete Algorithm
Each subscriber receives one random post per day without duplicates until all posts are sent:
1. Fetch all Post IDs from database
2. Get already-sent Post IDs for user from `PostSendHistory`
3. Calculate candidates = all IDs - sent IDs
4. If candidates empty -> delete user's history (reset cycle) -> use all posts
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
- `GET /` - Post listing with pagination (9 items/page), blog filtering, and subscription signup
- `GET /analytics` - Analytics dashboard with click statistics and trends
- `GET /search?keyword=&blog=` - Full-text search with optional blog filter
- Uses Thymeleaf + Tailwind CSS

### Email Verification & Subscription
- `POST /api/subscribers/send-verification` - Sends 6-digit verification code via email (Redis, 5-min TTL)
- `POST /api/subscribers/verify` - Verifies code and creates active subscriber
- `DELETE /api/subscribers?email=` - Unsubscribes user
- `GET /api/subscribers/unsubscribe?email=` - Unsubscribe via link

### Full-Text Search (Elasticsearch)
- `PostDocument` maps Post entity to Elasticsearch index
- Multi-field search: title (boosted x2), tags
- Blog-specific filtering via compound queries
- Results paginated (9 items/page)
- Indexed automatically after blog crawling job

### Analytics Dashboard
- Summary cards: total clicks, unique clickers, top blog, clicked posts count
- Top 10 posts visualization by click count
- Blog popularity breakdown
- 30-day daily trend chart
- Uses sealed interface `AnalyticsResult` (Success/NoData) for type-safe error handling

### Redis Caching
- `ANALYTICS_DASHBOARD`: 10-minute TTL for dashboard statistics
- `POST_LISTING`: 30-minute TTL for paginated post results
- `POST_SEARCH`: 15-minute TTL for search results
- Email verification codes: 5-minute TTL

### Click Tracking
- `GET /track?url={encodedUrl}&subscriberId={id}` - Tracks click and redirects (302)
- Redirect URL is validated against known Post links in DB (prevents open redirect)
- Links in newsletter emails are wrapped with tracking URLs
- Click events stored in `ClickLog` entity for analytics
- Uses sealed interface `TrackResult` (Success/InvalidUrl) for type-safe results

## Docker Infrastructure

All services are orchestrated via `docker-compose.yml`:
- **MySQL** (port 13306) with health check
- **Redis** (port 16379)
- **Elasticsearch 8.12.0** (port 19200) with health check, single-node mode
- **RabbitMQ** (port 15673, management UI on 25672) with health check
- **Spring Boot app** (port 18080) depends on all services being healthy
- Persistent volumes: `mysql_data`, `es_data`

## Code Conventions

- Package: `server.morningcommit.*`
- JPA entities use `allOpen` plugin for `@Entity`, `@MappedSuperclass`, `@Embeddable`
- Kotlin strict JSR-305 null-safety mode enabled
- Sealed interfaces used for type-safe result handling (`AnalyticsResult`, `TrackResult`, `UnsubscribeResult`)
- `RestPage<T>` wrapper used for JSON-serializable paginated responses

### Layered Responsibility

- Business rules and domain-level validations must be implemented in the Service layer.
  - Examples: state transition validation, duplication checks, policy decisions, permission-based logic
- The API (Controller) layer is responsible only for the following:
  - Request parameter binding
  - Input format validation (e.g., @NotNull, @Size)
  - Authentication and authorization
  - Invoking Services and mapping responses
