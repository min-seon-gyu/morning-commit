-- Blog Sources Initialization
-- This file runs on every application startup with spring.sql.init.mode=always

INSERT IGNORE INTO blog_source (blog, rss_url, is_active) VALUES
-- Korean Tech Company Blogs (Verified RSS URLs)
('KAKAO_TECH', 'https://tech.kakao.com/feed/', true),
('TOSS_TECH', 'https://toss.tech/rss.xml', true),
('WOOWA_BROS', 'https://techblog.woowahan.com/feed/', true),
('NAVER_D2', 'https://d2.naver.com/d2.atom', true),
('LINE_ENGINEERING', 'https://techblog.lycorp.co.jp/ko/feed/index.xml', true),
('HYPERCONNECT_TECH', 'https://hyperconnect.github.io/feed.xml', true),
('DEVSISTERS_TECH', 'https://tech.devsisters.com/rss/', true),
('BANKSALAD_TECH', 'https://blog.banksalad.com/feed.xml', true),

-- Global Tech Company Blogs (Verified RSS URLs)
('NETFLIX_TECH', 'https://netflixtechblog.com/feed', true),
('GITHUB_BLOG', 'https://github.blog/feed/', true),
('AIRBNB_TECH', 'https://medium.com/feed/airbnb-engineering', true),
('SPOTIFY_ENGINEERING', 'https://engineering.atspotify.com/feed/', true);

-- ==========================================
-- Sample Posts Data
-- ==========================================
INSERT IGNORE INTO post (title, link, description, publish_date, blog, created_at, updated_at) VALUES
-- Kakao Tech Posts
('Kakao의 대규모 트래픽 처리 전략',
 'https://tech.kakao.com/2024/01/traffic-handling',
 'Kakao는 하루 수십억 건의 요청을 처리합니다. 이 글에서는 Kafka를 활용한 비동기 메시징, Redis 클러스터를 통한 캐싱 전략, 그리고 Kubernetes 기반의 오토스케일링 구현 방법을 상세히 다룹니다. 특히 피크 시간대 트래픽 급증에 대응하는 서킷 브레이커 패턴과 백프레셔 처리 방식을 소개합니다.',
 '2024-01-25 09:00:00', 'KAKAO_TECH', NOW(), NOW()),

('Kotlin Coroutines로 비동기 프로그래밍 마스터하기',
 'https://tech.kakao.com/2024/01/kotlin-coroutines',
 'Kotlin Coroutines의 핵심 개념인 suspend 함수, CoroutineScope, Dispatcher에 대해 알아봅니다. 실제 프로덕션 환경에서 발생할 수 있는 메모리 누수 방지법과 구조화된 동시성(Structured Concurrency) 패턴을 예제 코드와 함께 설명합니다.',
 '2024-01-24 10:30:00', 'KAKAO_TECH', NOW(), NOW()),

-- Toss Tech Posts
('Toss 결제 시스템의 멱등성 보장 전략',
 'https://toss.tech/2024/01/payment-idempotency',
 '금융 시스템에서 가장 중요한 것은 정확성입니다. Toss는 결제 요청의 멱등성을 보장하기 위해 Idempotency Key 기반의 중복 방지 시스템을 구축했습니다. 분산 환경에서의 동시성 제어, 재시도 메커니즘, 그리고 장애 복구 전략을 공유합니다.',
 '2024-01-26 14:00:00', 'TOSS_TECH', NOW(), NOW()),

('모노레포에서 마이크로서비스로: Toss의 아키텍처 진화',
 'https://toss.tech/2024/01/monorepo-to-microservices',
 'Toss가 모노레포 구조에서 마이크로서비스 아키텍처로 전환한 경험을 공유합니다. 서비스 분리 기준, API Gateway 도입, 서비스 메시(Istio) 적용, 그리고 분산 트레이싱 구현까지 전체 마이그레이션 여정을 다룹니다.',
 '2024-01-23 11:00:00', 'TOSS_TECH', NOW(), NOW()),

-- Woowa Bros Posts
('배달의민족 주문 시스템 리팩토링 여정',
 'https://techblog.woowahan.com/2024/01/order-system-refactoring',
 '레거시 주문 시스템을 이벤트 소싱(Event Sourcing)과 CQRS 패턴으로 재설계한 경험을 공유합니다. 기존 시스템과의 병행 운영, 데이터 마이그레이션 전략, 그리고 점진적 트래픽 전환 방법을 상세히 설명합니다.',
 '2024-01-22 09:30:00', 'WOOWA_BROS', NOW(), NOW()),

('JPA N+1 문제 완벽 해결 가이드',
 'https://techblog.woowahan.com/2024/01/jpa-n-plus-1',
 'JPA를 사용하면서 가장 흔히 마주치는 N+1 문제의 원인과 해결책을 정리했습니다. Fetch Join, EntityGraph, Batch Size 설정 등 다양한 해결 방법의 장단점을 비교하고, 실제 쿼리 로그를 통해 성능 개선 효과를 측정합니다.',
 '2024-01-21 15:00:00', 'WOOWA_BROS', NOW(), NOW()),

-- Naver D2 Posts
('대용량 로그 처리를 위한 Elasticsearch 최적화',
 'https://d2.naver.com/2024/01/elasticsearch-optimization',
 '하루 TB 단위의 로그를 처리하는 Elasticsearch 클러스터 운영 노하우를 공유합니다. 인덱스 설계, 샤드 전략, Hot-Warm-Cold 아키텍처, 그리고 쿼리 성능 튜닝 방법을 실제 사례와 함께 설명합니다.',
 '2024-01-20 10:00:00', 'NAVER_D2', NOW(), NOW()),

-- LINE Engineering Posts
('LINE 메시징 서버의 고가용성 아키텍처',
 'https://techblog.lycorp.co.jp/ko/messaging-ha',
 'LINE 메시징 서버가 어떻게 99.99% 가용성을 달성하는지 소개합니다. 멀티 리전 배포, 자동 페일오버, 메시지 유실 방지를 위한 저장소 이중화 전략을 다룹니다.',
 '2024-01-19 13:00:00', 'LINE_ENGINEERING', NOW(), NOW()),

-- Netflix Tech Posts
('Building Resilient Microservices at Netflix',
 'https://netflixtechblog.com/resilient-microservices',
 'Netflix shares its battle-tested patterns for building resilient microservices. Learn about Hystrix circuit breakers, retry strategies with exponential backoff, bulkhead isolation, and chaos engineering practices that help Netflix achieve 99.99% uptime.',
 '2024-01-18 08:00:00', 'NETFLIX_TECH', NOW(), NOW()),

-- GitHub Blog Posts
('How GitHub Copilot Understands Your Code',
 'https://github.blog/copilot-architecture',
 'An inside look at the architecture behind GitHub Copilot. We explore how large language models are fine-tuned on code, the retrieval-augmented generation (RAG) system that provides context, and the real-time inference pipeline that delivers suggestions in milliseconds.',
 '2024-01-17 12:00:00', 'GITHUB_BLOG', NOW(), NOW()),

-- Hyperconnect Tech Posts
('Hyperconnect의 WebRTC 최적화 전략',
 'https://hyperconnect.github.io/webrtc-optimization',
 '실시간 영상 통화 서비스를 위한 WebRTC 최적화 경험을 공유합니다. 네트워크 상태에 따른 비트레이트 조절, 지연 시간 최소화, 그리고 다양한 디바이스 호환성 확보 방법을 다룹니다.',
 '2024-01-16 11:30:00', 'HYPERCONNECT_TECH', NOW(), NOW()),

-- Spotify Engineering Posts
('Event-Driven Architecture at Spotify Scale',
 'https://engineering.atspotify.com/event-driven-architecture',
 'How Spotify processes billions of events daily using Apache Kafka and Google Cloud Pub/Sub. We discuss event schema evolution, exactly-once processing semantics, and monitoring strategies for high-throughput event pipelines.',
 '2024-01-15 09:00:00', 'SPOTIFY_ENGINEERING', NOW(), NOW());
