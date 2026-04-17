# MorningCommit 유지보수 플랜

프로젝트 전반의 리팩터링·개선 계획과 진행 상황을 추적하는 문서입니다.
세부 설계·구현 스펙은 [CLAUDE.md](./CLAUDE.md)를 참고합니다.

최종 업데이트: 2026-04-17

---

## 완료된 작업 (이번 세션 반영분)

### 테스트 안전망 구축

| 영역 | 내용 | 테스트 수 |
|------|------|-----------|
| SubscriberService | 인증 코드·구독·재활성화·시도 횟수 제한 | 11 |
| PostService | 목록·블로그 필터·ID 조회 | 6 |
| AnalyticsService | 대시보드 집계·NoData 분기 | 5 |
| PostSelectionService | Shuffle-and-Deplete | 5 |
| TrackingService | URL 검증·이벤트 발행 | 2 |
| SubscriberController | `@Valid` 실패·비즈니스 예외 매핑 | 9 |
| TrackingController | 302 리다이렉트·파라미터 검증 | 4 |
| SearchController | 검색 모드 조합·enum 바인딩 | 3 |
| ViewController | index/analytics/unsubscribe 분기 | 6 |
| **합계** | MockK + SpringMockK, `@WebMvcTest`, 인프라 의존 없음 | **51** |

### 아키텍처 개선

- `BlogCrawlingJobConfig` 책임 분리 (224줄 → 77줄): `BlogCrawlingService`(크롤링 로직) + `PostIndexer`(저장·색인)
- `EmailDeliveryJobConfig`에서 Shuffle-and-Deplete 알고리즘을 `PostSelectionService`로 이동
- `BlogCrawlingService.analyzeEntry` 다단계 null 체크를 early-return + 헬퍼(`toPost`, `parseDifficulty`, `estimateReadingTime`, `extractFallbackContent`)로 재구성

### 코드 품질

- `try-catch { log.error; throw }` 8곳 → `KLogger.runLogging` 확장 함수로 공통화
- `BlogCrawlingService`·`RssParsingTest`에 중복된 XML 정제 로직을 `XmlSanitizer` util로 추출
- `SubscriberService.verifyAndSubscribe`의 `?.apply ?: save` 패턴을 명시적 `if-else` 분기로 개선

### 설정 외부화·상수화

- 이메일 제목 하드코딩 → `email/EmailConstants.kt`의 `EmailSubject` object
- RabbitMQ exchange/queue 상수 12개 → `@ConfigurationProperties(prefix="app.rabbitmq")` 기반 `RabbitMqProperties`로 이동
  - `@RabbitListener`는 `${app.rabbitmq.*.queue}` placeholder 사용
  - `application.yml`에 기본값 명시

### 도메인·검증

- 구독자 DTO 3개에 `@Email`·`@NotBlank`·`@Pattern("\d{6}")` 추가, `SubscriberController`에 `@Valid` 적용
- `GlobalExceptionHandler` 확장: `MethodArgumentNotValid`, `HttpMessageNotReadable`, `MissingServletRequestParameter`, `MethodArgumentTypeMismatch`, `HttpRequestMethodNotSupported` → C001 매핑
- `Subscriber ↔ PostSendHistory` 관계 `CascadeType.ALL + orphanRemoval` → `CascadeType.PERSIST`로 완화 (감사 기록 보호)

### 성능

- 클릭 트래킹의 `existsByLink` 매 호출 DB 조회 → `PostLinkValidator.exists()`에 `@Cacheable(POST_LINK_EXISTS, 1h, unless="!#result")` 적용

### 문서·규칙

- 커밋 메시지 한글화: 기존 120개 커밋을 `git filter-branch`로 일괄 재작성 + force-push
- 신규 커밋 컨벤션: prefix(feat/fix/refactor/perf/test/docs)만 영문, 본문은 한글
- 메모리에 한글 커밋 규칙 저장 (`feedback_korean_commits.md`)

---

## 남은 작업

### MEDIUM — 스키마·인덱스 변경 동반

#### 1. `ClickLog` ↔ `Post` 외래키 관계 강화
- **현상**: `ClickLogRepository`에서 `c.targetUrl = p.link`로 문자열 조인. 타입 안전성·성능·정규화 부족.
- **계획**:
  - `ClickLog`에 `postId: Long` 컬럼 추가 및 Post FK 설정
  - 클릭 수집 시점에 URL → Post 매핑 1회 수행
  - 기존 JPQL을 `c.post.id = p.id` 기반으로 전환
- **마이그레이션**: 기존 ClickLog 데이터가 있다면 `targetUrl → postId` 백필 스크립트 필요
- **우선순위 근거**: 분석 쿼리 효율 향상, 데이터 정합성 확보

#### 2. Elasticsearch 쿼리 타입 안전화
- **현상**: `PostSearchRepository`에서 ES 쿼리를 JSON 문자열로 관리 → 필드명 오타 런타임 발견
- **계획**:
  - `NativeQuery` 빌더 또는 `ElasticsearchOperations` 기반 DSL로 재작성
  - nori 분석기 설정과 부스트 전략(title×3, tags×2)은 유지
- **영향 범위**: `PostSearchService`·`SearchController`까지 테스트 필요
- **우선순위 근거**: 가장 큰 작업. 스키마 변경은 없지만 검색 결과 동등성 검증 필수

---

### LOW — 품질·환경 개선

#### 3. 설정값 외부화 (@ConfigurationProperties)
- 대상: Redis TTL, HTTP timeout(10s), Semaphore(5), 재시도 횟수(3) 등 매직 넘버
- 형태: `app.blog-crawling.*`, `app.verification.*` 등 도메인별 properties 클래스

#### 4. 배치 레이어 단위 테스트 확충
- 대상: `BlogCrawlingService.processSource` 시나리오, `PostIndexer.indexAll` 성공/ES 실패 경로
- 도구: MockK + 코루틴 `runTest`

#### 5. `BlogSource` 관리 엔드포인트
- 현재는 DB에 직접 insert만 가능. 관리자 REST API 또는 Seeder 구조가 유용할 수 있음

#### 6. 로깅 관측성 강화
- 구독자 id·이메일 해시를 MDC에 주입해 consumer 로그 추적성 개선
- Actuator `/health`, `/metrics` 노출 여부 결정

#### 7. 테스트 실행 환경 정비
- `MorningCommitApplicationTests.contextLoads`가 환경 변수 없으면 실패 → `@SpringBootTest`용 Testcontainers 또는 프로필 분리 고려

---

## 세션별 작업 이력 참고

- 2026-04-17: 커밋 메시지 한글 재작성, HIGH/MEDIUM 유지보수 일괄 진행 (커밋 12건), 테스트 51건 추가
- 이전 이력은 `git log --oneline`으로 확인

---

## 우선순위 가이드

> **"지금부터 하나만 한다면"**
>
> → (1) ES 쿼리 타입 안전화 — 다음 기능 확장 시 걸림돌이 될 가장 큰 부채.

> **"여유가 된다면"**
>
> → (2) ClickLog-Post FK, (3) 설정값 외부화 순.

> **"신기능 추가 전 반드시"**
>
> → (4) 배치 레이어 테스트 확충 — 크롤링 로직 수정 시 안전망 필수.
