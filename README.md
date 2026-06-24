# habit-flow : B2C 일정/습관 관리 백엔드 아키텍처
<img width="381" height="610" alt="Image" src="https://github.com/user-attachments/assets/e2a4b0fa-8c23-4f3f-a746-36a07a9c8fa8" />

트래픽 확장성과 데이터 정합성을 고려한 B2C 일정/습관 관리 백엔드 아키텍처 구축

- **목적:** 대용량 트래픽 병목 제어 및 모던 백엔드(Spring Boot 3.x / JPA) 기술 내재화
---
## 핵심 트러블슈팅 및 성능 최적화

단순한 기능 구현을 넘어, 데이터베이스의 실행 계획(EXPLAIN)을 분석하고 대용량 트래픽 환경을 고려한 아키텍처 개선에 집중했습니다.

### 1. 커버링 인덱스 페이징을 통한 조회 성능 극대화 (Filesort 제거)
* **문제 상황 (Before):** 다중 프로젝트의 태스크 목록을 커서 페이징으로 조회할 때, `ORDER BY`와 다중 조건(`IN` 절)으로 인해 인덱스를 타지 못하고 5만 건의 데이터를 메모리에 올려 정렬하는 `Using filesort` 대참사 발생.
* **해결 과정:**
  * 조인(JOIN)으로 인해 DB 옵티마이저가 드라이빙 테이블을 잘못 잡는 문제를 파악.
  * 쿼리를 2단계로 분리(지연 조인 패턴 적용). 1차 쿼리에서 조인을 끊고 조건에 맞는 타겟 ID만 커버링 인덱스로 서브 밀리초 만에 추출.
  * 2차 쿼리에서 추출한 ID를 `IN` 절로 넘겨 `type: const` 및 `ref` 로 상세 데이터를 가볍게 조립.
* **결과 (After):** * **개선 전:** Table Full Scan 및 디스크 정렬 발생
  * **개선 후:** Index Range Scan으로 타겟 데이터만 정확히 스캔하여, 디스크 I/O 및 정렬 부하를 획기적으로 감소.

### 2. Spring Security 비동기 스레드 컨텍스트 증발 이슈 해결
* **문제 상황:** SSE(Server-Sent Events) 실시간 알림 연결 타임아웃(50초) 시, Tomcat 비동기 스레드에 JWT 인증 컨텍스트가 전파되지 않아 엉뚱한 `Access Denied(403)` 예외와 로그 노이즈 발생.
* **해결 과정:** Security 설정에서 `DispatcherType.ASYNC` 타입의 요청을 시큐리티 필터 체인에서 통과(`permitAll`)시키도록 구조 변경 및 SseEmitter 타임아웃 콜백 로직 안전장치 구현.
* **결과:** 의미 없는 403 에러 로그를 제거하고, 좀비 커넥션으로 인한 메모리 누수(OOM) 가능성을 차단

### 3. Redisson 분산 락을 활용한 동시성 이슈(따닥) 완벽 제어
* **문제 상황:** 사용자의 네트워크 지연 및 중복 클릭(따닥)으로 인해 `toggleCompletion` API가 동시에 여러 번 호출될 경우, 통계 데이터 및 상태 업데이트 로직에 Race Condition이 발생하여 데이터 무결성이 깨지는 위험 발견.
* **해결 과정:**
  * DB 락(Pessimistic/Optimistic)과 Redis 기반 락을 비교 분석. 
  * Redis Redisson을 활용한 분산 락(Distributed Lock)을 적용하여, 다중 서버 환경에서 중복 클릭(따닥)으로 인한 데이터 무결성 문제 방지.
  * `task_id`를 기반으로 고유한 Lock Key를 생성하여, 첫 번째 요청이 처리되는 동안 후속 중복 요청은 대기(또는 즉시 실패/Idempotency)하도록 분산 락 적용.
* **결과:** 다중 서버 환경 및 초당 수백 건의 동시 요청 상황에서도 데이터 정합성 확보 및 중복 실행 방지.

|항목|기술|도입 배경 및 근거|
|---|---|---------------|
|언어|Java 17||
|프레임워크|Spring Boot 3.5.14 | |
|데이터베이스|MariaDB 10.11 | |
|Cache/Lock | Redis (Redisson) | 분산 락(Distributed Lock)을 활용한 동시성 제어 및 병목 해소|
|ORM / Query | Spring Data JPA, QueryDSL | 동적 커서 페이징 처리 및 컴파일 타임의 타입 안정성 확보|
|보안 | Spring Security, JWT | 무상태(Stateless) 기반의 빠르고 확장성 있는 인증/인가 처리|
|보안 | Hashids | DB의 Auto Increment PK를 URL에 직접 노출하지 않도록 Hashids를 활용한 난독화(Obfuscation) 적용 |
|실시간 | SSE(Server-Sent Events) | 태스크 알림 등 서버 -> 클라이언트 단방향 실시간 푸시 최적화|

### 로컬 개발 & 인프라 아키텍처

현재 프로젝트는 핵심 비즈니스 로직(동시성 제어, N+1 쿼리 최적화 등)의 완성도에 집중하기 위해 로컬 개발 환경(Local Development Environment)을 기준으로 아키텍처가 구성되어 있습니다.

- **로컬 실행:** Spring Boot의 내장 Tomcat을 활용하여 별도의 웹 서버 설정 없이 독립적으로 실행됩니다.
- **코드형 인프라 (IaC):** MariaDB, Redis 등 인프라 의존성은 **Docker Compose**를 통해 일관된 환경으로 구축 및 실행되도록 세팅하여, 언제든 클라우드 환경으로 이관할 수 있는 준비를 마쳤습니다.
- **실시간 커뮤니케이션:** 클라이언트(React)와의 실시간 알림 동기화는 별도의 소켓 서버 없이 Spring Web MVC 기반의 **SSE(Server-Sent Events)**를 활용하여 경량화했습니다.

---

### 향후 인프라 고도화 계획 (To-Be)

- **CI/CD 파이프라인:** GitHub Actions를 활용한 테스트 및 빌드 자동화
- **컨테이너 배포:** Docker 이미지 빌드 및 AWS EC2 인스턴스 환경으로의 배포
- **웹 서버:** Nginx 리버스 프록시 도입 및 Let's Encrypt를 통한 HTTPS 보안 적용

### 현재 진행 상황 (26년 6월 기준)
- **[Core]** Spring Security + JWT 무상태 인증 및 도메인 모델링 완료
- **[Query]** QueryDSL 다중 조건 동적 쿼리 및 `default_batch_fetch_size` 최적화 완료
- **[Performance]** 더미 데이터(Task 10만, Log 50만) Bulk Insert 및 JMeter 1만 동시 접속 부하 테스트 환경 구축 완료
- **[Troubleshooting]** 트래픽 동시성 제어를 위한 Redis 분산 락 도입
- **[Next]** Fetch Join 기반 N+1 완전 해소 및 AWS 프리티어 배포 예정 (7월 완료 목표)
