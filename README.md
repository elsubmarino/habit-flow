# habit-flow : 모던 백엔드 아키텍처 전환 연구 (PoC)

기존 MyBatis/RDB 중심 환경에서 모던 백엔드 스택인 Spring Boot 3.x 및 Spring Data JPA 생태계로의 전환을 위한 기술 검증(Proof of Concept) 프로젝트입니다. 

9년간의 엔터프라이즈 레거시 개발 경험을 바탕으로, 새로운 기술 생태계 도입 시 발생할 수 있는 퍼시스턴스 계층의 생산성 변화와 구조적 차이를 객관적으로 비교·검증하는 데 목적을 두고 있습니다.

---

## 주요 검증 및 연구 내용

### 1. Spring Boot 3.x 기반 엔티티 모델링 및 영속성 메커니즘 분석
* 레거시 관계형 데이터베이스 구조를 JPA 엔티티로 매핑하며 객체-DB 간 패러다임 불일치 해결 구조 분석
* 1차 캐시, 지연 로딩(Lazy Loading), 쓰기 지연 등 영속성 컨텍스트(Persistence Context)의 핵심 메커니즘 검증을 통한 퍼시스턴스 계층 안정성 확보 연구

### 2. QueryDSL을 활용한 다중 조건 동적 쿼리 표준화
* 컴파일 시점의 타입 안정성(Type Safety) 확보를 통해 문자열 기반 쿼리가 가질 수 있는 휴먼 에러 원천 차단 검증
* 복잡한 다중 검색 조건을 가독성 높은 Java 코드로 구현하여, 레거시 MyBatis 동적 XML 구문 대비 개발 생산성 및 유지보수성 개선 비교

---

## Tech Stack

| 분류 | 기술 스택 |
| --- | --- |
| 언어 | Java 17 |
| 프레임워크 | Spring Boot 3.x |
| ORM / Query | Spring Data JPA, QueryDSL |
| 데이터베이스 | MariaDB |

---

## 환경 구성
* **로컬 개발 환경:** Spring Boot 내장 Tomcat을 활용한 독립 실행 환경 구축
* **인프라 컨테이너화:** MariaDB 등 개발에 필요한 필수 인프라 의존성을 `Docker Compose`로 코드화하여 일관된 로컬 개발 환경 유지
