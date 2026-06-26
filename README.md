# habit-flow

Spring Boot 3.x, Spring Data JPA, QueryDSL 기반의 일정 관리 서비스입니다.

기존 MyBatis/RDB 중심의 개발 경험을 바탕으로, JPA 기반 도메인 모델링과 QueryDSL을 활용한 동적 검색 API 구현 방식을 검증하기 위해 진행한 개인 프로젝트입니다.

레거시 SQL 중심의 데이터 접근 방식과 비교하여, 객체 중심의 엔티티 설계, 영속성 컨텍스트, 타입 안전한 쿼리 작성 방식이 백엔드 개발 생산성과 유지보수성에 어떤 차이를 만드는지 확인하는 데 목적을 두었습니다.

---

## 주요 구현 내용

### 1. Spring Boot 3.x 기반 도메인 모델링

* 일정 관리 도메인에 필요한 엔티티 설계
* Spring Data JPA 기반 Repository 계층 구현
* 엔티티 연관관계 매핑을 통한 객체 중심 데이터 모델 구성
* 영속성 컨텍스트, 지연 로딩, 변경 감지 등 JPA 핵심 동작 방식 검증

### 2. QueryDSL 기반 동적 검색 API 구현

* 다중 검색 조건을 처리하기 위한 QueryDSL 기반 동적 쿼리 구현
* 문자열 기반 쿼리 작성 방식 대비 컴파일 시점 타입 안정성 확보
* MyBatis XML 기반 동적 SQL과 비교하여 코드 가독성 및 유지보수성 검토

### 3. Docker 기반 로컬 개발 환경 구성

* MariaDB 실행 환경을 Docker Compose로 구성
* 로컬 개발 환경의 데이터베이스 의존성을 컨테이너화
* 개발 환경 재현성과 초기 설정 편의성 개선

---

## Tech Stack

| 분류          | 기술                        |
| ----------- | ------------------------- |
| Language    | Java 17                   |
| Framework   | Spring Boot 3.x           |
| ORM / Query | Spring Data JPA, QueryDSL |
| Database    | MariaDB                   |
| Infra       | Docker Compose            |

---

## 프로젝트 목적

이 프로젝트는 단순 기능 구현뿐 아니라, 기존 MyBatis 중심의 개발 방식에서 Spring Data JPA 기반 개발 방식으로 전환할 때 발생하는 구조적 차이를 직접 검증하기 위해 진행했습니다.

특히 다음 항목을 중점적으로 확인했습니다.

* 엔티티 중심 도메인 모델링 방식
* JPA 영속성 컨텍스트의 동작 방식
* QueryDSL을 활용한 타입 안전한 동적 쿼리 작성
* Docker Compose 기반 로컬 개발 환경 구성
