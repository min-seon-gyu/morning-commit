# MorningCommit

매일 아침 기술 블로그 뉴스레터를 자동으로 수집, 요약하여 이메일로 전달하는 서비스입니다.

## 주요 기능

- **RSS 피드 크롤링**: 등록된 기술 블로그의 RSS 피드를 자동으로 수집
- **본문 스크래핑**: Jsoup을 활용한 전체 아티클 콘텐츠 추출
- **AI 요약**: OpenAI GPT를 통한 아티클 자동 요약
- **이메일 발송**: 개인화된 뉴스레터를 구독자에게 전달

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Kotlin 1.9.25, Java 21 |
| Framework | Spring Boot 3.4.2, Spring Cloud 2024.0.0 |
| Batch | Spring Batch 5 |
| Database | MySQL, Spring Data JPA |
| Message Queue | RabbitMQ |
| Template | Thymeleaf |
| External API | OpenFeign (OpenAI API) |
| Parser | Rome (RSS/Atom), Jsoup (HTML) |

## 아키텍처

```
Daily Scheduler (매일 오전 7시)
    │
    ├─► blogCrawlingJob
    │       ├─► 활성화된 BlogSource 엔티티 조회
    │       ├─► RSS 피드 수집 (Rome)
    │       ├─► 7일 이내 게시글 필터링
    │       ├─► 본문 스크래핑 (Jsoup)
    │       ├─► AI 요약 (OpenAI via Feign)
    │       └─► Post 엔티티 저장
    │
    └─► emailDeliveryJob (크롤링 성공 시 실행)
            ├─► 활성화된 Subscriber 엔티티 조회
            ├─► 오늘 수집된 Post ID 조회
            └─► EmailRequest를 RabbitMQ에 발행
                    │
                    └─► EmailConsumer (비동기 처리)
                            ├─► DB에서 Post 조회
                            ├─► Thymeleaf 템플릿 렌더링
                            └─► SMTP를 통한 이메일 발송
```

## 프로젝트 구조

```
server.morningcommit
├── domain/           # JPA 엔티티 (BlogSource, Post, Subscriber, BaseEntity)
├── repository/       # Spring Data JPA Repository
├── batch/            # Spring Batch Job (BlogCrawlingJob, EmailDeliveryJob)
├── scheduler/        # @Scheduled 작업 오케스트레이션
├── scraper/          # HtmlScraper (Jsoup)
├── ai/
│   ├── client/       # OpenAiClient (Feign)
│   ├── dto/          # ChatCompletion DTO
│   └── service/      # SummaryService
├── email/
│   ├── dto/          # EmailRequest
│   ├── EmailService  # Thymeleaf + JavaMailSender
│   ├── EmailProducer # RabbitMQ Publisher
│   └── EmailConsumer # RabbitMQ Listener
└── config/           # Spring 설정
```

## RabbitMQ 설정

| 항목 | 값 |
|------|-----|
| Queue | `email-queue` |
| Exchange | `email-exchange` (Direct) |
| Routing Key | `send-email` |

## 스케줄러

- **Cron 표현식**: `0 0 7 * * *` (매일 오전 7시)
- **실행 순서**: blogCrawlingJob 실행 후 성공 시 emailDeliveryJob 실행