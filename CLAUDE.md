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

# Lint check (ktlint)
./gradlew ktlintCheck

# Docker (all infrastructure services)
docker-compose up -d
```

## Project Overview

MorningCommit is a daily tech blog newsletter service that:
1. Crawls RSS feeds from tech blogs
2. Scrapes full article content
3. Summarizes using OpenAI GPT (filters out promotional content)
4. Indexes posts into Elasticsearch for full-text Korean search (nori analyzer)
5. Delivers personalized newsletters via email
6. Tracks link clicks for analytics
7. Supports email-verified subscriber signup
8. Provides secure HMAC-based unsubscribe links

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
- **Elasticsearch 8.12.0** with nori Korean analyzer for full-text post search
- **Kibana 8.12.0** for Elasticsearch visualization
- **ktlint** for Kotlin code style enforcement (disabled rules: `import-ordering`, `no-wildcard-imports`, `filename`, `indent`, `parameter-list-wrapping`)

## Architecture

```
blogCrawlingJob (Daily at 1 AM)
    │
    ├─► Read active BlogSource entities
    ├─► Fetch RSS feeds (Rome)
    ├─► Filter recent posts (last 2 days)
    ├─► Scrape full content (Jsoup)
    ├─► Summarize & analyze (OpenAI via Feign)
    │       └─► Promotional content filtered out
    │       └─► Extracts: summary, tags, difficulty, keyInsight, readingTimeMin
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
    └─► Publish EmailRequest to RabbitMQ (RetryTemplate retries on failure)
            │
            └─► EmailConsumer (async)
                    ├─► Fetch Post from DB
                    ├─► EmailUrlGenerator: Transform links to tracking URLs
                    ├─► EmailTemplateRenderer: Render Thymeleaf template (Korean)
                    │       └─► Embeds HMAC unsubscribe token via UnsubscribeTokenService
                    ├─► EmailSender: Send via SMTP
                    └─► On failure → retry 3 times → DLQ (email-queue-dlq)

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
                    ├─► Clear analytics cache
                    └─► On failure → retry 3 times → DLQ (tracking-queue-dlq)

Email Verification Flow
    │
    ├─► POST /api/subscribers/send-verification
    │       └─► Generate 6-digit code → Store in Redis (5-min TTL) → Send via email
    └─► POST /api/subscribers/verify
            └─► Validate code from Redis → Create active Subscriber (or reactivate existing)

Unsubscribe Flow (HMAC Token-based)
    │
    ├─► Email newsletter contains unsubscribe link:
    │       GET /unsubscribe?email={email}&token={hmac_token}
    │           └─► ViewController validates token via UnsubscribeTokenService
    │               └─► Renders unsubscribe.html confirmation page
    │
    └─► User confirms unsubscription:
            POST /api/subscribers/unsubscribe
                Body: { email, token }
                └─► SubscriberController validates HMAC token
                    └─► subscriberService.unsubscribe(email) → marks isActive=false
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
│   ├── dto/          # SendVerificationRequest, VerifyRequest, UnsubscribeRequest
│   ├── ViewController       # Web UI (posts, analytics, unsubscribe confirmation)
│   ├── TrackingController   # Click tracking redirect
│   ├── SearchController     # Elasticsearch search endpoint
│   └── SubscriberController # Email verification, subscription, unsubscription
├── ai/
│   ├── client/       # OpenAiClient (Feign)
│   ├── dto/          # ChatCompletion DTOs
│   └── service/
│       ├── SummaryService        # OpenAI summarization + promotional analysis
│       └── dto/BlogAnalysisResult # Summary, tags, difficulty, keyInsight, isPromotional
├── email/
│   ├── dto/          # EmailRequest, ClickLogEvent, TrackedPost (includes keyInsight)
│   ├── EmailService          # Orchestrator: delegates to Sender, Renderer, UrlGenerator
│   ├── EmailSender           # SMTP sending via JavaMailSender
│   ├── EmailTemplateRenderer # Thymeleaf rendering + unsubscribe token embedding
│   ├── EmailUrlGenerator     # Tracking URL generation
│   ├── EmailProducer         # RabbitMQ publisher
│   ├── EmailConsumer         # RabbitMQ listener
│   └── TrackingConsumer      # Click tracking listener (clears analytics cache)
├── service/          # AnalyticsService (contains nested AnalyticsDashboard), TrackingService,
│   │                 # PostService, PostSearchService, SubscriberService, BlogSourceService,
│   │                 # UnsubscribeTokenService (HMAC-SHA256 token generation/validation)
│   └── dto/          # PostClickCount, BlogClickCount, DailyClickCount
└── config/           # RabbitMqConfig, RedisConfig, JpaConfig, FeignConfig,
                      # ElasticsearchConfig, SchedulingConfig, StringListConverter, RestPage
```

## Supported Blogs (Blog Enum)

KAKAO_TECH, KAKAO_PAY, TOSS_TECH, WOOWA_BROS, LINE_ENGINEERING, HYPERCONNECT_TECH, KURLY, SOCAR, OLIVE_YOUNG, BANKSALAD, DEV_SISTERS

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:mysql://localhost:13306/morningcommit` | MySQL connection URL |
| `DB_USERNAME` | `root` | MySQL username |
| `DB_PASSWORD` | `1234` | MySQL password |
| `OPENAI_API_KEY` | - | OpenAI API key for summarization |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `RABBITMQ_PORT` | `15673` | RabbitMQ port |
| `RABBITMQ_USERNAME` | `guest` | RabbitMQ username |
| `RABBITMQ_PASSWORD` | `guest` | RabbitMQ password |
| `MAIL_HOST` | `smtp.naver.com` | SMTP server |
| `MAIL_PORT` | `465` | SMTP port |
| `MAIL_USERNAME` | - | SMTP username |
| `MAIL_PASSWORD` | - | SMTP password |
| `APP_BASE_URL` | `http://localhost:18080` | Base URL for click tracking and unsubscribe links |
| `APP_UNSUBSCRIBE_SECRET` | - | HMAC-SHA256 secret for unsubscribe token generation |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `16379` | Redis port |
| `ELASTICSEARCH_HOST` | `localhost` | Elasticsearch host |
| `ELASTICSEARCH_PORT` | `19200` | Elasticsearch port |
| `ELASTIC_PASSWORD` | - | Elasticsearch xpack security password |

## Key Components

### Batch Jobs
- **blogCrawlingJob**: Crawls RSS, scrapes content, summarizes (filters promotional), batch saves to DB, indexes to Elasticsearch
- **emailDeliveryJob**: Reads subscribers, selects random post (shuffle-and-deplete), publishes to RabbitMQ

### Promotional Content Filtering (Strict Mode)
During blog crawling, OpenAI analyzes each article and flags promotional content. Posts marked as promotional are filtered out before saving. **When in doubt, filter out.** Promotional content includes:
- Recruitment/hiring posts, intern recruitment, career fair announcements
- Hackathon/ideathon/makeathon announcements or recaps, conference/meetup/seminar recaps
- Welcome kit unboxings, office tours, onboarding process descriptions
- Employee interviews, team introductions, company culture, year-in-review posts
- Product launches, feature updates, release notes, service introductions
- Project retrospectives focused on teamwork (not tech), research showcases without implementation
- Newsletter roundups, link collections, surveys, open source announcements without technical depth
- Only articles with substantial technical depth (code, architecture, implementation details) are kept

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
- Dead Letter Exchange: `email-dlx` (Direct)
- Dead Letter Queues:
  - `email-queue-dlq` (Routing Key: `send-email`) - Failed email messages
  - `tracking-queue-dlq` (Routing Key: `tracking-log`) - Failed tracking messages
- Consumer Dynamic Scaling:
  - EmailConsumer: `concurrency = "3-10"` — scales 3 to 10 threads based on SMTP I/O load
  - TrackingConsumer: `concurrency = "2-5"` — scales 2 to 5 threads based on DB insert load
- Message Loss Prevention:
  - Publisher Confirm (`correlated`) + Returns: Broker delivery acknowledgement with NACK/return logging
  - Publisher Retry: `RetryTemplate` on `RabbitTemplate` (3 attempts, exponential backoff 1s → 2s → 4s)
  - Consumer Retry: Spring listener retry (3 attempts, exponential backoff 1s → 2s → 4s)
  - DLQ: After retry exhaustion, rejected messages routed to DLQ via `x-dead-letter-exchange`
  - Messages are persistent by default (`Jackson2JsonMessageConverter` sets `deliveryMode=PERSISTENT`)

### Scheduler
- `blogCrawlingJob`: `0 0 1 * * *` (Daily at 1 AM)
- `emailDeliveryJob`: `0 0 7 * * *` (Daily at 7 AM)

### Web UI
- `GET /` - Post listing with pagination (9 items/page), blog filtering, subscription signup, and hero section ("AI가 요약한 기술 블로그")
- `GET /analytics` - Analytics dashboard with click statistics, subscriber count, and trends
- `GET /search?keyword=&blog=` - Full-text search with optional blog filter (keyword optional, defaults to all posts)
- `GET /unsubscribe?email=&token=` - Unsubscribe confirmation page (HMAC token validated)
- Uses Thymeleaf + Tailwind CSS
- Post cards display reading time and difficulty badges
- Search bar is positioned below the blog filter tag list (toggle on index page, always visible on search page)

### Email Verification & Subscription
- `POST /api/subscribers/send-verification` - Sends 6-digit verification code via email (Redis, 5-min TTL)
- `POST /api/subscribers/verify` - Verifies code and creates active subscriber (or reactivates existing)
- `POST /api/subscribers/unsubscribe` - Validates HMAC token and marks subscriber inactive

### Full-Text Search (Elasticsearch)
- `PostDocument` maps Post entity to Elasticsearch index with nori Korean analyzer
- Custom analyzer config in `src/main/resources/elasticsearch-settings.json` (nori_tokenizer, nori_readingform, lowercase)
- Multi-field search: title (boosted x3), summary, tags (boosted x2)
- `Fuzziness.AUTO` on multi_match queries for typo tolerance
- `keyword` parameter is optional (defaults to empty string) — `/search` without keyword shows all posts
- 4 search modes: all posts, keyword only, blog filter only, keyword + blog filter
- `findAllDocuments(pageable)` for browsing without keyword
- `findAllByBlog(blog, pageable)` for blog-specific listing
- Results paginated (9 items/page)
- Indexed automatically after blog crawling job

### Analytics Dashboard
- Summary cards: total clicks, unique clickers, top blog, clicked posts count, total subscribers
- Top 10 posts visualization by click count (with max values for percentage bars)
- Blog popularity breakdown
- 30-day daily trend chart
- `AnalyticsDashboard` is a nested data class inside `AnalyticsService`
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

### Unsubscribe (HMAC Token-based)
- `UnsubscribeTokenService` generates/validates HMAC-SHA256 tokens using `app.unsubscribe-secret`
- Tokens are stateless and non-expiring (unlike Redis-based verification codes)
- Newsletter emails embed unsubscribe links with pre-signed tokens
- Two-step flow: GET renders confirmation page, POST executes unsubscription

### Email Service Architecture
`EmailService` is an orchestrator that delegates to:
- `EmailUrlGenerator` - Transforms post links into tracking URLs
- `EmailTemplateRenderer` - Renders Thymeleaf newsletter template with unsubscribe token
- `EmailSender` - Sends email via JavaMailSender SMTP

## Docker Infrastructure

All services are orchestrated via `docker-compose.yml` on a `morningcommit-net` bridge network:
- **MySQL** (port 13306) with health check
- **Redis** (port 16379)
- **Elasticsearch 8.12.0** (port 19200) - Custom image with nori plugin (`docker/elasticsearch/Dockerfile`), xpack security enabled, JVM heap 256MB
- **Kibana 8.12.0** (port 15601) - Connected to Elasticsearch
- **RabbitMQ** (port 15673, management UI on 25672) with health check
- **Spring Boot app** (port 18080) depends on all services being healthy
- Persistent volumes: `mysql_data`, `es_data`

## Post Entity Fields

Key fields on the `Post` entity:
- `title`, `link`, `content`, `summary` - Core article data
- `tags: List<String>` - AI-extracted tags
- `difficulty: String` - Article difficulty level (BEGINNER, INTERMEDIATE, ADVANCED, EXPERT)
- `keyInsight: String?` - AI-extracted key insight from the article
- `readingTimeMin: Int` - Estimated reading time in minutes
- `blog: Blog` - Source blog enum
- `isPromotional` - Filtered out during crawling

## Templates

- `index.html` - Main post listing page with hero section
- `search.html` - Search results page
- `analytics.html` - Analytics dashboard
- `newsletter.html` - Email newsletter template
- `unsubscribe.html` - Unsubscribe confirmation page
- `verification.html` - Email verification code template

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