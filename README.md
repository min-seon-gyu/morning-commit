# MorningCommit

매일 아침 기술 블로그 뉴스레터를 자동으로 수집, 요약하여 이메일로 전달하는 서비스입니다.

## 링크
- [MorningCommit](https://morningcommit.store/)

## 주요 기능

- **RSS 피드 크롤링**: 등록된 기술 블로그의 RSS 피드를 자동으로 수집
- **본문 스크래핑**: Jsoup을 활용한 전체 아티클 콘텐츠 추출
- **AI 요약**: OpenAI GPT를 통한 아티클 자동 요약
- **홍보성 콘텐츠 필터링**: AI 기반 기술 아티클과 홍보성 콘텐츠 자동 분류 및 필터링
- **이메일 인증 구독**: Redis 기반 6자리 인증 코드를 통한 이메일 구독 관리
- **이메일 발송**: 개인화된 뉴스레터를 구독자에게 전달
- **클릭 트래킹**: 뉴스레터 링크 클릭 추적 및 분석
- **분석 대시보드**: 클릭 통계, 인기 포스트, 블로그별 현황, 일별 트렌드 시각화
- **Redis 캐싱**: 대시보드 및 포스트 목록 성능 최적화
- **웹 UI**: 블로그별 필터링, 페이지네이션, 구독 신청 지원

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
| Cache | Redis |
| Infra | Docker Compose |

## 아키텍처

```
blogCrawlingJob (매일 오전 1시 실행)
    │
    ├─► 활성화된 BlogSource 엔티티 조회
    ├─► RSS 피드 수집 (Rome)
    ├─► 최근 게시글 필터링
    ├─► 본문 스크래핑 (Jsoup)
    ├─► AI 요약 및 분석 (OpenAI via Feign)
    │       └─► 홍보성 콘텐츠 필터링
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
                    ├─► ClickLog 엔티티 DB 저장
                    └─► 분석 캐시 초기화

이메일 인증 흐름
    │
    ├─► POST /api/subscribers/send-verification
    │       └─► 6자리 인증 코드 생성 → Redis 저장 (5분 TTL) → 이메일 발송
    └─► POST /api/subscribers/verify
            └─► Redis에서 코드 검증 → 활성 Subscriber 생성
```

## 프로젝트 구조

```
server.morningcommit
├── domain/           # JPA 엔티티 (BlogSource, Post, Subscriber, ClickLog, PostSendHistory, BaseEntity)
│                     # Blog enum (지원 블로그 목록)
├── repository/       # Spring Data JPA Repository
├── batch/            # Spring Batch Job (BlogCrawlingJob, EmailDeliveryJob)
├── scheduler/        # @Scheduled 작업 오케스트레이션 (JobScheduler)
├── scraper/          # HtmlScraper (Jsoup)
├── controller/
│   ├── dto/          # SendVerificationRequest, VerifyRequest
│   ├── ViewController       # 웹 UI (포스트 목록, 분석 대시보드)
│   ├── TrackingController   # 클릭 트래킹 리다이렉트
│   └── SubscriberController # 이메일 인증 및 구독 관리
├── ai/
│   ├── client/       # OpenAiClient (Feign)
│   ├── dto/          # ChatCompletion DTO
│   └── service/
│       ├── SummaryService        # OpenAI 요약 + 홍보성 분석
│       └── dto/BlogAnalysisResult # 요약, 태그, 난이도, 홍보 여부
├── email/
│   ├── dto/          # EmailRequest, ClickLogEvent, TrackedPost
│   ├── EmailService  # Thymeleaf + JavaMailSender
│   ├── EmailProducer # RabbitMQ Publisher
│   ├── EmailConsumer # RabbitMQ Listener
│   └── TrackingConsumer # 클릭 트래킹 Listener (분석 캐시 초기화)
├── service/          # AnalyticsService, TrackingService, PostService,
│   │                 # SubscriberService, BlogSourceService
│   └── dto/          # PostClickCount, BlogClickCount, DailyClickCount, AnalyticsDashboard
└── config/           # RabbitMqConfig, RedisConfig, JpaConfig, FeignConfig,
                      # SchedulingConfig, StringListConverter, RestPage
```

## 홍보성 콘텐츠 필터링

블로그 크롤링 시 OpenAI가 각 아티클을 분석하여 홍보성 콘텐츠를 자동으로 필터링합니다.

**필터링 대상 (홍보성)**:
- 채용 공고, 이벤트/해커톤 안내
- 제품 추천, 회사 문화 소개
- 구현 없는 연구 소개, 마케팅 콘텐츠

**수집 대상 (기술 콘텐츠)**:
- 기술, 아키텍처, 알고리즘, 실무 노하우를 다루는 아티클

## 이메일 인증 및 구독 관리

| 엔드포인트 | 메서드 | 설명 |
|-----------|--------|------|
| `/api/subscribers/send-verification` | POST | 6자리 인증 코드 이메일 발송 (Redis, 5분 TTL) |
| `/api/subscribers/verify` | POST | 인증 코드 확인 후 구독자 등록 |
| `/api/subscribers?email=` | DELETE | 구독 해지 |
| `/api/subscribers/unsubscribe?email=` | GET | 이메일 링크를 통한 구독 해지 |

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

- `GET /` - 포스트 목록 (페이지네이션 9개/페이지, 블로그별 필터링, 구독 신청)
- `GET /analytics` - 분석 대시보드 (클릭 통계, 인기 포스트, 일별 트렌드)
- Thymeleaf + Tailwind CSS 기반

## 분석 대시보드

- **요약 카드**: 총 클릭 수, 유니크 사용자 수, 최다 클릭 블로그, 클릭된 포스트 수
- **인기 포스트 Top 10**: 클릭 수 기반 상위 10개 포스트 시각화
- **블로그별 인기도**: 블로그별 클릭 분포
- **일별 트렌드**: 최근 30일간 일별 클릭 추이

## Redis 캐싱

| 캐시 이름 | TTL | 용도 |
|-----------|-----|------|
| `ANALYTICS_DASHBOARD` | 10분 | 대시보드 통계 |
| `POST_LISTING` | 30분 | 포스트 목록 페이지네이션 |
| 이메일 인증 코드 | 5분 | 구독 인증 코드 저장 |

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