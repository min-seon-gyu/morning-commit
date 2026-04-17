<div align="center">

# MorningCommit

**AI가 요약한 기술 블로그, 매일 아침 뉴스레터로 받아보세요**

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.12.0-005571?logo=elasticsearch&logoColor=white)](https://www.elastic.co/)
[![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white)](https://redis.io/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?logo=rabbitmq&logoColor=white)](https://www.rabbitmq.com/)
[![MySQL](https://img.shields.io/badge/MySQL-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)

[morningcommit.store](https://morningcommit.store/)

</div>

---

## 서비스 소개

MorningCommit은 국내 주요 기술 블로그를 자동으로 크롤링하고, AI가 요약하여 매일 아침 뉴스레터로 발송하는 서비스입니다.

### 핵심 기능

| 기능 | 설명 |
|------|------|
| RSS 크롤링 | 13개 기술 블로그의 RSS 피드를 매일 자동 수집 |
| AI 요약 | OpenAI GPT로 아티클 요약, 난이도 분석, 핵심 인사이트 추출 |
| 홍보성 필터링 | AI가 홍보성 콘텐츠를 자동 분류하여 기술 아티클만 선별 |
| 한국어 전문 검색 | Elasticsearch + Nori 형태소 분석기 기반 전문 검색 |
| 뉴스레터 발송 | 구독자별 중복 없는 랜덤 포스트 매일 이메일 발송 |
| 이메일 인증 구독 | Redis 기반 6자리 인증 코드를 통한 구독 관리 |
| 클릭 트래킹 | 뉴스레터 링크 클릭 추적 및 분석 대시보드 |
| 보안 구독 해지 | HMAC-SHA256 토큰 기반 구독 해지 |

---

## 지원 블로그

| 블로그 | Enum |
|--------|------|
| 카카오 테크 | `KAKAO_TECH` |
| 카카오페이 | `KAKAO_PAY` |
| 토스 테크 | `TOSS_TECH` |
| 우아한형제들 | `WOOWA_BROS` |
| LINE Engineering | `LINE_ENGINEERING` |
| 하이퍼커넥트 | `HYPERCONNECT_TECH` |
| 컬리 | `KURLY` |
| 쏘카 | `SOCAR` |
| 올리브영 | `OLIVE_YOUNG` |
| 뱅크샐러드 | `BANKSALAD` |
| 데브시스터즈 | `DEV_SISTERS` |
| 무신사 | `MUSINSA` |
| 당근 | `DAANGN` |

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Kotlin 1.9.25, Java 21 |
| Framework | Spring Boot 3.4.2, Spring Cloud 2024.0.0 |
| Batch | Spring Batch 5 |
| Database | MySQL, Spring Data JPA |
| Search | Elasticsearch 8.12.0 + Nori 한국어 분석기 |
| Visualization | Kibana 8.12.0 |
| Message Queue | RabbitMQ |
| Cache | Redis |
| Template | Thymeleaf + Tailwind CSS |
| AI | Spring AI 1.0.0 (OpenAI GPT) |
| Concurrency | Kotlin Coroutines 1.8.1 |
| Logging | KotlinLogging |
| Parser | Rome (RSS/Atom), Jsoup (HTML) |
| Lint | ktlint |
| Infra | Docker Compose |

---

## 아키텍처

### 블로그 크롤링 (매일 오전 1시)

```
blogCrawlingJob
    │
    ├─► 활성화된 BlogSource 엔티티 조회
    ├─► RSS 피드 수집 (Rome)
    ├─► 최근 게시글 필터링 (최근 2일)
    ├─► 본문 스크래핑 (Jsoup)
    ├─► AI 요약 및 분석 (OpenAI via Spring AI ChatClient)
    │       ├─► Kotlin Coroutines 병렬 처리 (Semaphore(5))
    │       ├─► 홍보성 콘텐츠 필터링
    │       └─► 요약, 태그, 난이도, 핵심 인사이트, 읽기 시간 추출
    ├─► Post 엔티티 일괄 저장 (중복 사전 필터링)
    └─► Elasticsearch 인덱싱
```

### 뉴스레터 발송 (매일 오전 7시)

```
emailDeliveryJob
    │
    ├─► 활성화된 Subscriber 엔티티 조회
    ├─► Shuffle-and-Deplete 포스트 선택
    │       ├─► 전체 Post ID 조회
    │       ├─► PostSendHistory에서 발송 완료된 Post ID 조회
    │       ├─► 후보 = 전체 - 발송완료
    │       ├─► 후보 없으면 → 히스토리 초기화, 전체 사용
    │       ├─► 후보 중 랜덤 1개 선택
    │       └─► PostSendHistory에 저장
    └─► EmailRequest를 RabbitMQ에 발행 (실패 시 RetryTemplate 재시도)
            │
            └─► EmailConsumer (비동기)
                    ├─► DB에서 Post 조회
                    ├─► EmailUrlGenerator: 트래킹 URL 변환
                    ├─► EmailTemplateRenderer: 템플릿 렌더링 + 구독해지 토큰 삽입
                    ├─► EmailSender: SMTP 발송
                    └─► 실패 시 → 재시도 3회 → DLQ(email-queue-dlq)로 이동
```

### 클릭 트래킹

```
사용자가 이메일에서 트래킹 링크 클릭
    │
    └─► GET /track?url={encodedUrl}&subscriberId={id}
            ├─► ClickLogEvent를 RabbitMQ에 발행
            ├─► 원본 URL로 리다이렉트 (302)
            │
            └─► TrackingConsumer (비동기)
                    ├─► ClickLog 엔티티 DB 저장
                    ├─► 분석 캐시 초기화
                    └─► 실패 시 → 재시도 3회 → DLQ(tracking-queue-dlq)로 이동
```

### 이메일 인증 & 구독 해지

```
이메일 인증 흐름
    ├─► POST /api/subscribers/send-verification
    │       └─► 6자리 인증 코드 생성 → Redis 저장 (5분 TTL) → 이메일 발송
    └─► POST /api/subscribers/verify
            └─► Redis에서 코드 검증 (최대 5회 시도 제한) → 구독자 등록 (또는 기존 구독자 재활성화)

구독 해지 흐름 (HMAC 토큰 기반)
    ├─► 뉴스레터 이메일의 구독 해지 링크 클릭
    │       GET /unsubscribe?email={email}&token={hmac_token}
    │           └─► HMAC 토큰 검증 → 구독 해지 확인 페이지 렌더링
    └─► 사용자 확인
            POST /api/subscribers/unsubscribe { email, token }
                └─► HMAC 토큰 재검증 → 구독자 비활성화
```

---

## 프로젝트 구조

```
server.morningcommit
├── domain/           # JPA 엔티티, Blog enum, Difficulty enum, PostDocument (Elasticsearch)
├── repository/       # Spring Data JPA Repository, PostSearchRepository (Elasticsearch)
├── batch/
│   ├── BlogCrawlingJobConfig  # Spring Batch Job/Step/Reader/Writer 배선만 담당
│   ├── BlogCrawlingService    # RSS 수집·스크래핑·AI 분석·Post 생성 비즈니스 로직
│   ├── PostIndexer            # Post 저장·캐시 초기화·Elasticsearch 색인
│   └── EmailDeliveryJobConfig # 뉴스레터 발송 Job
├── scheduler/        # @Scheduled 작업 오케스트레이션 (JobScheduler)
├── scraper/          # HtmlScraper (Jsoup)
├── controller/
│   ├── dto/          # @Valid 검증 적용된 요청 DTO (SendVerification, Verify, Unsubscribe)
│   ├── ViewController       # 웹 UI (포스트, 분석, 구독해지)
│   ├── TrackingController   # 클릭 트래킹 리다이렉트
│   ├── SearchController     # Elasticsearch 검색 엔드포인트
│   └── SubscriberController # 이메일 인증, 구독, 구독해지 (@Valid 적용)
├── exception/
│   ├── BusinessException    # 기본 예외 (NotFoundException, DuplicateException 등)
│   ├── ErrorCode            # 에러 코드 Enum (S001~S003, C001~C002)
│   └── GlobalExceptionHandler # 비즈니스/검증/Spring MVC 예외 통합 처리
├── ai/
│   └── service/
│       ├── SummaryService        # Spring AI ChatClient 기반 요약 + 홍보성 분석
│       └── dto/BlogAnalysisResult # 요약, 태그, 난이도, 핵심 인사이트, 홍보 여부
├── email/
│   ├── dto/          # EmailRequest, ClickLogEvent, TrackedPost
│   ├── EmailConstants        # EmailSubject (인증/뉴스레터 제목 상수)
│   ├── EmailService          # 오케스트레이터 (Sender, Renderer, UrlGenerator에 위임)
│   ├── EmailSender           # SMTP 발송 (JavaMailSender)
│   ├── EmailTemplateRenderer # Thymeleaf 렌더링 + 구독해지 토큰 삽입
│   ├── EmailUrlGenerator     # 트래킹 URL 생성
│   ├── EmailProducer         # RabbitMQ Publisher (RabbitMqProperties 주입)
│   ├── EmailConsumer         # RabbitMQ Listener (${app.rabbitmq.email.queue})
│   └── TrackingConsumer      # 클릭 트래킹 Listener
├── service/
│   ├── AnalyticsService          # 분석 대시보드 (AnalyticsDashboard 포함)
│   ├── TrackingService           # 클릭 트래킹 이벤트 발행
│   ├── PostLinkValidator         # @Cacheable 기반 URL 유효성 검증 (1시간 TTL)
│   ├── PostSelectionService      # Shuffle-and-Deplete 알고리즘 (뉴스레터 포스트 선택)
│   ├── PostService               # 포스트 관리
│   ├── PostSearchService         # Elasticsearch 검색
│   ├── SubscriberService         # 구독자 관리
│   ├── BlogSourceService         # 블로그 소스 관리
│   ├── UnsubscribeTokenService   # HMAC-SHA256 토큰 생성/검증
│   └── dto/          # PostClickCount, BlogClickCount, DailyClickCount
├── util/
│   ├── ErrorLogging          # KLogger.runLogging 확장 (try-catch log+throw 공통화)
│   └── XmlSanitizer          # RSS XML의 DOCTYPE·제어문자 제거
└── config/           # RabbitMqConfig, RabbitMqProperties(@ConfigurationProperties),
                      # RedisConfig, JpaConfig, ElasticsearchConfig, SchedulingConfig,
                      # StringListConverter, RestPage
```

---

## API 엔드포인트

### 웹 UI

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/` | 포스트 목록 (히어로 섹션, 페이지네이션 9개/페이지, 블로그 필터링, 구독 신청) |
| GET | `/analytics` | 분석 대시보드 (클릭 통계, 구독자 수, 일별 트렌드) |
| GET | `/search?keyword=&blog=` | 전문 검색 (키워드 + 블로그 필터) |
| GET | `/unsubscribe?email=&token=` | 구독 해지 확인 페이지 |
| GET | `/track?url=&subscriberId=` | 클릭 트래킹 후 원본 URL 리다이렉트 (302) |

### REST API

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/subscribers/send-verification` | 6자리 인증 코드 이메일 발송 (Redis, 5분 TTL) |
| POST | `/api/subscribers/verify` | 인증 코드 확인 후 구독자 등록/재활성화 (최대 5회 시도) |
| POST | `/api/subscribers/unsubscribe` | HMAC 토큰 검증 후 구독 해지 |

---

## 주요 컴포넌트 상세

### AI 요약 및 분석

OpenAI GPT가 각 아티클을 분석하여 다음을 추출합니다:

| 항목 | 설명 |
|------|------|
| `summary` | 아티클 요약 |
| `tags` | 관련 기술 태그 |
| `difficulty` | 난이도 (BEGINNER, INTERMEDIATE, ADVANCED, EXPERT) |
| `keyInsight` | 핵심 인사이트 |
| `readingTimeMin` | 예상 읽기 시간 (분) |
| `isPromotional` | 홍보성 여부 (true이면 저장하지 않음) |

### 홍보성 콘텐츠 필터링

**필터링 대상 (홍보성)**: 채용 공고, 이벤트/해커톤 안내, 제품 추천, 회사 문화 소개, 구현 없는 연구 소개, 마케팅 콘텐츠

**수집 대상 (기술 콘텐츠)**: 기술, 아키텍처, 알고리즘, 실무 노하우를 다루는 아티클

### 전문 검색 (Elasticsearch + Nori)

- Nori 한국어 형태소 분석기 기반 (`nori_tokenizer`, `nori_readingform`, `lowercase`)
- 다중 필드 검색: `title`(x3 부스트), `summary`, `tags`(x2 부스트)
- `Fuzziness.AUTO`로 오타 자동 허용 (검색어 길이에 따라 1~2글자 오차 허용)
- 4가지 검색 모드: 전체 조회 / 키워드 검색 / 블로그 필터 / 키워드 + 블로그 필터
- 키워드 없이 `/search` 접근 시 전체 글 목록 표시
- 결과 페이지네이션 (9개/페이지)

### Shuffle-and-Deplete 알고리즘

구독자에게 중복 없이 매일 하나의 랜덤 포스트를 발송합니다:

1. 전체 Post ID 조회
2. `PostSendHistory`에서 해당 사용자에게 발송된 Post ID 조회
3. 후보 목록 = 전체 ID - 발송 완료 ID
4. 후보가 없으면 (사이클 완료) → 히스토리 초기화 후 전체 사용
5. 후보 중 랜덤으로 1개 선택하여 발송

### Redis 캐싱

| 캐시 키 | TTL | 용도 |
|---------|-----|------|
| `ANALYTICS_DASHBOARD` | 10분 | 대시보드 통계 |
| `POST_LISTING` | 30분 | 포스트 목록 페이지네이션 |
| `POST_SEARCH` | 15분 | 검색 결과 |
| `POST_LINK_EXISTS` | 60분 | 트래킹 URL 유효성 검증 (유효한 링크만 캐싱) |
| 이메일 인증 코드 | 5분 | 구독 인증 코드 |

### RabbitMQ

Exchange·Queue·Routing Key는 `app.rabbitmq.*` 설정으로 외부화되어 `RabbitMqProperties`를 통해 주입됩니다. 기본값:

| 항목 | 값 | 용도 |
|------|-----|------|
| Exchange | `email-exchange` (Direct) | 이메일 발송 Exchange |
| Exchange | `tracking-exchange` (Direct) | 클릭 트래킹 Exchange |
| Queue | `email-queue` (Routing Key: `send-email`) | 이메일 발송 |
| Queue | `tracking-queue` (Routing Key: `tracking-log`) | 클릭 트래킹 |
| DLX | `email-queue-dlx` (Direct) | 이메일 Dead Letter Exchange |
| DLX | `tracking-queue-dlx` (Direct) | 트래킹 Dead Letter Exchange |
| DLQ | `email-queue-dlq` (Routing Key: `email-dead-letter`) | 이메일 발송 실패 메시지 보관 |
| DLQ | `tracking-queue-dlq` (Routing Key: `tracking-dead-letter`) | 클릭 트래킹 실패 메시지 보관 |

**Consumer 동적 확장:**

- **EmailConsumer**: `concurrency = "3-10"` — SMTP I/O 대기 시간에 따라 3~10개 인스턴스 자동 확장
- **TrackingConsumer**: `concurrency = "2-5"` — DB insert 부하에 따라 2~5개 인스턴스 자동 확장

**메시지 유실 방지:**

- **Publisher Confirm/Return**: 브로커 수신 확인 (NACK/라우팅 실패 시 경고 로그)
- **Publisher Retry**: `RetryTemplate`로 송신 실패 시 자동 재시도 (최대 3회, 1s → 2s → 4s)
- **Consumer Retry**: 수신 처리 실패 시 자동 재시도 (최대 3회, 1s → 2s → 4s)
- **DLQ**: 재시도 소진 후 메시지를 Dead Letter Queue로 이동하여 보관
- **메시지 영속성**: `Jackson2JsonMessageConverter` 기본 `deliveryMode=PERSISTENT`

### 분석 대시보드

- 요약 카드: 총 클릭 수, 유니크 사용자 수, 최다 클릭 블로그, 클릭된 포스트 수, 총 구독자 수
- 인기 포스트 Top 10 시각화
- 블로그별 인기도 분포
- 최근 30일 일별 클릭 트렌드

---

## 인프라 구성

Docker Compose로 전체 인프라를 관리하며, `morningcommit-net` 브릿지 네트워크로 연결됩니다.

| 서비스 | 포트 | 비고 |
|--------|------|------|
| MySQL | 13306 | Health check |
| Redis | 16379 | |
| Elasticsearch 8.12.0 | 19200 | Nori 플러그인, xpack 보안, JVM 256MB |
| Kibana 8.12.0 | 15601 | Elasticsearch 연결 |
| RabbitMQ | 15673 (AMQP), 25672 (관리 UI) | Health check |
| Spring Boot App | 18080 | 모든 서비스 의존 |

영구 볼륨: `mysql_data`, `es_data`

---

## 스케줄러

| Job | Cron | 설명 |
|-----|------|------|
| `blogCrawlingJob` | `0 0 1 * * *` | 매일 오전 1시 - RSS 크롤링, AI 요약, ES 인덱싱 |
| `emailDeliveryJob` | `0 0 7 * * *` | 매일 오전 7시 - 뉴스레터 발송 |

---

## 테스트

단위 테스트는 MockK + SpringMockK(`com.ninja-squad:springmockk`) 조합을 사용하며, 인프라 없이 실행 가능합니다.

### 실행

```bash
# 서비스 레이어 단위 테스트
./gradlew test --tests "server.morningcommit.service.*"

# 컨트롤러 레이어 @WebMvcTest
./gradlew test --tests "server.morningcommit.controller.*"
```

### 커버리지

| 영역 | 테스트 파일 | 개수 |
|------|-------------|------|
| `SubscriberService` | 인증 코드, 구독/해지, 재시도 제한 | 11 |
| `PostService` | 페이지네이션, 블로그 필터 | 6 |
| `AnalyticsService` | 대시보드 집계, NoData 분기 | 5 |
| `PostSelectionService` | Shuffle-and-Deplete | 5 |
| `TrackingService` | URL 검증, 이벤트 발행 | 2 |
| `SubscriberController` | @Valid 실패, 비즈니스 예외 매핑 | 9 |
| `TrackingController` | 리다이렉트, 파라미터 검증 | 4 |
| `SearchController` | 검색 모드 조합, enum 바인딩 | 3 |
| `ViewController` | index/analytics/unsubscribe 분기 | 6 |
| **합계** | | **51** |

---

## 커밋 메시지 규칙

- 신규 커밋은 **한글**로 작성합니다 (conventional prefix는 영문 유지: `feat:`, `fix:`, `refactor:`, `perf:`, `test:`, `docs:`)
- 예: `refactor: XML 정제 로직을 XmlSanitizer util로 공통화`