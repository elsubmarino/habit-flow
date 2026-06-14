# habit-flow (현재 고도화 진행 중)
> 트래픽 확장성과 데이터 정합성을 고려한 B2C 일정/습관 관리 백엔드 아키텍처 구축

- **목적:** 대용량 트래픽 병목 제어 및 모던 백엔드(Spring Boot 3.x / JPA) 기술 내재화
- **현재 진행 상황 (24년 6월 기준)** - **[Core]** Spring Security + JWT 무상태 인증 및 도메인 모델링 완료
  - **[Query]** QueryDSL 다중 조건 동적 쿼리 및 `default_batch_fetch_size` 최적화 완료
  - **[Performance]** 더미 데이터(Task 10만, Log 50만) Bulk Insert 및 JMeter 1만 동시 접속 부하 테스트 환경 구축 완료
  - **[Troubleshooting 💡]** 트래픽 동시성 제어를 위한 Redis 분산 락 도입 (현재 Lettuce 스핀 락 부하 이슈를 확인하고 Redisson Pub/Sub 방식으로 마이그레이션 및 TPS 지표 비교 테스트 중)
  - **[Next]** Fetch Join 기반 N+1 완전 해소 및 AWS 프리티어 배포 예정 (7월 완료 목표)