# Habit Flow

Spring Boot 3.x, Spring Data JPA, QueryDSL을 기반으로 구현한 일정 관리 서비스입니다.

일정 도메인의 등록·조회와 다중 조건 검색을 지원하며, 계층별 책임을 분리한 백엔드 구조와 객체 중심의 도메인 모델을 적용했습니다.  
MariaDB 실행 환경은 Docker Compose로 구성해 개발 환경을 일관되게 재현할 수 있도록 했습니다.

---

## 주요 구현 내용

### 1. 일정 도메인 설계

- 일정 관리 도메인에 필요한 엔티티 및 연관관계 설계
- Spring Data JPA 기반 Repository 계층 구현
- 지연 로딩과 변경 감지를 고려한 영속성 관리
- 도메인, 애플리케이션, 인프라 계층의 책임 분리

### 2. QueryDSL 기반 검색 API

- 다중 검색 조건을 조합할 수 있는 동적 검색 기능 구현
- 조건별 Predicate 구성을 통한 검색 로직 관리
- 컴파일 시점 타입 검증이 가능한 쿼리 구조 적용
- 검색 조건 확장과 유지보수를 고려한 조회 계층 구성

### 3. Docker 기반 개발 환경

- MariaDB 실행 환경을 Docker Compose로 구성
- 애플리케이션과 데이터베이스 실행 환경 표준화
- 신규 개발 환경에서 동일한 구성을 재현할 수 있도록 설정

---

## Tech Stack

| 분류 | 기술 |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| ORM / Query | Spring Data JPA, QueryDSL |
| Database | MariaDB |
| Infra | Docker Compose |

---

## 주요 기술적 고려사항

- 엔티티 간 연관관계와 조회 범위를 고려한 도메인 모델 설계
- 지연 로딩으로 발생할 수 있는 추가 쿼리와 조회 성능 관리
- 복합 검색 조건을 확장할 수 있는 QueryDSL 조회 구조
- 개발 환경의 일관성을 확보하기 위한 데이터베이스 컨테이너화
