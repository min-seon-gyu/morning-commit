# MorningCommit

매일 아침 기술 블로그 뉴스레터를 자동으로 수집, 요약하여 이메일로 전달하는 서비스입니다.

## 주요 기능

- **RSS 피드 크롤링**: 등록된 기술 블로그의 RSS 피드를 자동으로 수집
- **본문 스크래핑**: Jsoup을 활용한 전체 아티클 콘텐츠 추출
- **AI 요약**: OpenAI GPT를 통한 아티클 자동 요약
- **이메일 발송**: 개인화된 뉴스레터를 구독자에게 전달
- **클릭 트래킹**: 뉴스레터 링크 클릭 추적 및 분석
- **웹 UI**: 블로그별 필터링 및 페이지네이션 지원

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
blogCrawlingJob (매일 오전 1시 실행)
    │
    ├─► 활성화된 BlogSource 엔티티 조회
    ├─► RSS 피드 수집 (Rome)
    ├─► 최근 게시글 필터링
    ├─► 본문 스크래핑 (Jsoup)
    ├─► AI 요약 (OpenAI via Feign)
    └─► Post 엔티티 일괄 저장 (중복 사전 필터링)

emailDeliveryJob (매일 오전 7시 실행)
    │
    ├─► 활성화된 Subscriber 엔티티 조회
    ├─► Shuffle-and-Deplete 포스트 선택:
    │       ├─► 전체 Post ID 조회
    │       ├─► PostSendHistory에서 발송 완료된 Post ID 조회
    │       ├─► 후보 목록 계산 (전체 - 발송완료)
    │       ├─► 후보 없으면: 히스토리 초기화, 전체 사용
    │       ├─► 후보 중 랜덤 1개 선택
    │       └─► PostSendHistory에 저장
    └─► EmailRequest를 RabbitMQ에 발행
            │
            └─► EmailConsumer (비동기 처리)
                    ├─► DB에서 Post 조회
                    ├─► 링크를 트래킹 URL로 변환
                    ├─► Thymeleaf 템플릿 렌더링 (한국어)
                    └─► SMTP를 통한 이메일 발송

클릭 트래킹 흐름
    │
    사용자가 이메일에서 트래킹 링크 클릭
    │
    └─► GET /track?url={encodedUrl}&subscriberId={id}
            ├─► ClickLogEvent를 RabbitMQ에 발행
            ├─► 원본 URL로 리다이렉트 (302)
            │
            └─► TrackingConsumer (비동기 처리)
                    └─► ClickLog 엔티티 DB 저장
```

## 프로젝트 구조

```
server.morningcommit
├── domain/           # JPA 엔티티 (BlogSource, Post, Subscriber, ClickLog, PostSendHistory, BaseEntity)
├── repository/       # Spring Data JPA Repository
├── batch/            # Spring Batch Job (BlogCrawlingJob, EmailDeliveryJob)
├── scheduler/        # @Scheduled 작업 오케스트레이션
├── scraper/          # HtmlScraper (Jsoup)
├── controller/       # Web Controller (ViewController, TrackingController)
├── ai/
│   ├── client/       # OpenAiClient (Feign)
│   ├── dto/          # ChatCompletion DTO
│   └── service/      # SummaryService
├── email/
│   ├── dto/          # EmailRequest, ClickLogEvent, TrackedPost
│   ├── EmailService  # Thymeleaf + JavaMailSender
│   ├── EmailProducer # RabbitMQ Publisher
│   ├── EmailConsumer # RabbitMQ Listener
│   └── TrackingConsumer # 클릭 트래킹 Listener
└── config/           # Spring 설정
```

## RabbitMQ 설정

| 항목 | 값 | 용도 |
|------|-----|------|
| Exchange | `email-exchange` (Direct) | 공통 Exchange |
| Queue | `email-queue` | 이메일 발송 |
| Routing Key | `send-email` | 이메일 발송 |
| Queue | `tracking-queue` | 클릭 트래킹 |
| Routing Key | `tracking-log` | 클릭 트래킹 |

## 스케줄러

| Job | Cron 표현식 | 설명 |
|-----|-------------|------|
| `blogCrawlingJob` | `0 0 1 * * *` | 매일 오전 1시 RSS 피드 크롤링 |
| `emailDeliveryJob` | `0 0 7 * * *` | 매일 오전 7시 뉴스레터 발송 |

## 웹 UI

- `GET /` - 포스트 목록 (페이지네이션 9개/페이지, 블로그별 필터링)
- Thymeleaf + Tailwind CSS 기반

## 클릭 트래킹

- `GET /track?url={encodedUrl}&subscriberId={id}` - 클릭 추적 후 원본 URL로 리다이렉트 (302)
- 뉴스레터 이메일의 링크가 트래킹 URL로 변환되어 발송
- 클릭 이벤트는 `ClickLog` 엔티티에 저장되어 분석에 활용
- 리다이렉트 URL은 DB에 등록된 Post 링크만 허용 (Open Redirect 방지)

## Shuffle-and-Deplete 알고리즘

구독자에게 중복 없이 매일 하나의 랜덤 포스트를 발송하는 로직:

1. 전체 Post ID 조회
2. `PostSendHistory`에서 해당 사용자에게 발송된 Post ID 조회
3. 후보 목록 계산 (전체 ID - 발송 완료 ID)
4. 후보가 없으면 (사이클 완료) → 히스토리 초기화 후 전체 사용
5. 후보 중 랜덤으로 1개 선택
6. `PostSendHistory`에 저장 후 이메일 발송