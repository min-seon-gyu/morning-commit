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
./gradlew test --tests "server.morningcommit.MorningCommitApplicationTests"

# Run a single test method
./gradlew test --tests "server.morningcommit.MorningCommitApplicationTests.contextLoads"

# Clean build
./gradlew clean build
```

## Project Overview

MorningCommit is a Kotlin/Spring Boot 4 newsletter service that aggregates content from various sources, summarizes it using AI, and delivers it via email.

**Tech Stack:**
- Kotlin 2.2 with Java 21
- Spring Boot 4.0.2, Spring Cloud 2025.1.0
- MySQL with Spring Data JPA
- RabbitMQ (spring-boot-starter-amqp) for async messaging
- Spring Mail for email delivery
- OpenFeign for external AI API integration
- Rome library for RSS/Atom feed parsing
- Jsoup for HTML scraping

## Architecture

The application follows a pipeline architecture:

1. **Collector (수집기)**: Crawls RSS/Atom feeds using Rome, scrapes full article content with Jsoup
2. **AI Summarizer**: Sends content to external AI service via OpenFeign for summarization
3. **Dispatcher (발송기)**: Queues emails via RabbitMQ and sends using Spring Mail
4. **Batch Processing**: Spring Batch handles scheduled content aggregation jobs

## Database Configuration

JPA is configured with batch optimizations (batch_size=100, fetch_size=100).

## Code Conventions

- Package structure: `server.morningcommit.*`
- JPA entities must use `allOpen` plugin annotations (`@Entity`, `@MappedSuperclass`, `@Embeddable`)
- Kotlin compiler uses strict JSR-305 null-safety mode