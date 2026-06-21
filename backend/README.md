# habit-flow (현재 고도화 진행 중)
> 트래픽 확장성과 데이터 정합성을 고려한 B2C 일정/습관 관리 백엔드 아키텍처 구축

- **목적:** 대용량 트래픽 병목 제어 및 모던 백엔드(Spring Boot 3.x / JPA) 기술 내재화

<img width="381" height="610" alt="Image" src="https://github.com/user-attachments/assets/e2a4b0fa-8c23-4f3f-a746-36a07a9c8fa8" />

|항목|기술|도입 배경 및 근거|
|---|---|---------------|
|언어|Java 17|레코드(Record)를 활용한 DTO 불변성 보장 및 최신 문법 활용|
|프레임워크|Spring Boot 3.5.14 | Spring Security 6 호환 및 웹 애플리케이션의 견고한 생태계|
|데이터베이스|MariaDB 10.11 | 커버링 인덱스를 활용한 페이징 쿼리 최적화 및 안정적인 RDBMS|
|Cache/Lock | Redis (Redisson) | 분산 락(Distributed Lock)을 활용한 동시성 제어 및 병목 해소|
|ORM / Query | Spring Data JPA, QueryDSL | 동적 커서 펭리징 처리 및 컴파일 타임의 타입 안정성 확보|
|보안 | Spring Security, JWT | 무상태(Stateless 기반의 빠르고 확장성 있는 인증/인가 처리|
|보안 | Hashids | DB의 Auto Increment PK 노출을 막고 IDOR 해킹 공격 원천 차단 |
|실시간 | SSE(Server-Sent Events) | 테스크 알림 등 서버 -> 클라이언트 단방향 실시간 푸시 취적화


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

- **현재 진행 상황 (24년 6월 기준)** - **[Core]** Spring Security + JWT 무상태 인증 및 도메인 모델링 완료
  - **[Query]** QueryDSL 다중 조건 동적 쿼리 및 `default_batch_fetch_size` 최적화 완료
  - **[Performance]** 더미 데이터(Task 10만, Log 50만) Bulk Insert 및 JMeter 1만 동시 접속 부하 테스트 환경 구축 완료
  - **[Troubleshooting]** 트래픽 동시성 제어를 위한 Redis 분산 락 도입
  - **[Next]** Fetch Join 기반 N+1 완전 해소 및 AWS 프리티어 배포 예정 (7월 완료 목표)

