# ⚾ FULL COUNT - Backend Repository

<p align="center">
  <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white"/>
  <img src="https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=Spring-Security&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white"/>
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white"/>
  <img src="https://img.shields.io/badge/WebSocket(STOMP)-000000?style=for-the-badge&logo=websocket&logoColor=white"/>
</p>

## 📖 1. 프로젝트 개요 (Overview)
FULL COUNT 프로젝트의 **백엔드 서버 레포지토리**입니다. Java와 Spring Boot 기반으로 구축된 본 서버는 정교한 RDBMS 기반 데이터를 처리하며, 에스크로 기반의 다단계 안전 결제 시스템 무결성 유지, 그리고 STOMP/WebSocket 기반의 분산형 푸시 알림 인프라를 제공하는 RESTful API 서버입니다.

## ✨ 2. 핵심 기술 및 시스템 하이라이트

### 🛡️ 에스크로 기반 상태 머신 시스템 (`TransferService.java`)
- 중고 거래 시 발생하는 사기 위험을 방지하기 위해 엄격한 **5단계 트랜잭션 파이프라인**(요청 -> 결제예치(진행중) -> 티켓전달 -> 인수확정 -> 완료/취소)을 구축했습니다.
- 원자적인 잔액 차감 및 정산 로직을 구현했으며, 최초 구매자와 판매자만이 본인의 상태 전이(State-transitioning) 엔드포인트를 호출할 수 있도록 엔티티 계층에서부터 강력한 접근 제어를 구현했습니다.
- 부당한 거래 취소 시에는 **매너 온도(Manner Temperature)** 가 즉각 하락하도록 페널티 메커니즘을 자동화했습니다.

### 🚀 Spring Data JPA 핵심 쿼리 최적화 및 N+1 문제 해결
- 무거운 엔티티 연관 관계로 인해 발생하는 치명적인 `N+1` 쿼리 비효율을 완벽히 해결했습니다.
- 특히 `TransferRepository.java`에서는 다대일/일대다로 엮여있는 `Post`, `TicketPost`, `Seller`, `Buyer` 속성을 한 번의 데이터베이스 호출로 해결할 수 있도록 커스텀 `@Query`와 `LEFT JOIN FETCH` 지시어 처리를 적용해 어드민 페이지 및 대용량 조회 시의 병목 현상을 방지했습니다.

### 📡 실시간 WebSocket 및 메시징 브로커 아키텍처
- 전역 STOMP 브로커링을 서버 단에 도입해 지연 없는 양방향 통신을 지원합니다.
- `ChatRoomController`를 통해 단순 1:1 통신뿐만 아니라 직관 메이트/크루의 성격에 따른 분할 통신(`GROUP_JOIN`, `GROUP_CREW`, `ONE_ON_ONE`) 인터페이스를 구현했으며, 메시지는 도달과 동시에 영속 계층(DB)에 비동기로 업데이트되어 오프라인 유저의 이력 조회까지 깔끔하게 지원합니다.

### 🔒 글로벌 단위 응답 정규화 및 보안 랩핑 적용
- **`GlobalResponseAdvice.java`**: 클라이언트가 어떠한 종류의 예외나 데이터 객체(DTO)를 반환받더라도 항상 구조화된 `{ "success": true, "data": { ... } }` 포맷을 보장받을 수 있도록 응답을 일괄 정규화했습니다. (이 과정에서 발생할 수 있는 캐스팅 오류는 조건부 파싱으로 안전하게 차단)
- **Spring Security + JWT**: 서블릿 통과 단계에서 무상태(Stateless) 기반의 필터 체인을 설계했습니다. 모든 API 요청은 검증된 토큰에서 `MemberId`를 추출해 ThreadLocal에 저장하므로 서비스 계층이 직접 토큰에 의존하지 않고도 안전하게 작동합니다.

## 🏗️ 3. 백엔드 아키텍처 흐름도
`[프로젝트 아키텍처 다이어그램 / 모델링(ERD) 이미지 삽입]`

**계층형 구조 (Core Layers)**:
- **도메인 계층 (`/domain`)**: `Member`, `Post`, `Transfer`, `ChatRooms` 등을 관장하는 순수 비즈니스 상태 엔티티.
- **DTO 및 매퍼 계층 (`/dto`, `/mapper`)**: DB 모델인 Entity가 컨트롤러 및 외부로 노출되지 않도록 완벽히 격리된 페이로드 구조 운용.
- **서비스 계층 (`/service`)**: `@Transactional(readOnly=true)`를 디폴트로 설정해 데드락 우려 및 DB 락 자원을 최적화하고 명시적인 쓰기 조작 메서드에만 트랜잭션을 허용합니다.
- **예외 처리 핸들러 (`/advice`, `/exception`)**: 발생 가능한 에러를 도메인 중심의 커스텀 예외(`CustomBusinessExceptions`)로 선언하여 유지 보수의 효율을 끌어올렸습니다.

## 🚀 4. 설치 및 구동 가이드

```bash
# 1. 레포지토리 클론
git clone [백엔드 깃허브 URL 삽입]
cd fullcount-backend

# 2. 데이터베이스(DB) 및 환경 설정
# src/main/resources/application.yml 파일 내에서 MySQL 및 JWT 보안 키를 직접 구성 환경에 맞춰 변경하세요.
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fullcount_db
    username: your_db_user
    password: your_db_password

# 3. 프로젝트 빌드 및 실행 (Gradle)
./gradlew clean build -x test
java -jar build/libs/fullcount-0.0.1-SNAPSHOT.jar

# 개발환경(IDE) 없이 명령어 실행 시
./gradlew bootRun
```

## 🔐 5. 핵심 API 엔드포인트 주요 예시

- `POST   /api/transfers/request/{roomId}` : 채팅방 연동을 통해 원클릭으로 티켓 양도 에스크로 거래를 개시합니다.
- `DELETE /api/posts/{postId}/members/{memberId}/expel` : 크루장/관리자 전용 기능으로 직관 모집 방 내부의 멤버 수용 강제 조정(퇴장) 로직을 제어합니다.
- `GET    /api/ticket-transfers` : 날짜(`matchDate`), 구장(`stadium`), 거래 상태(`status`) 등 다중 옵션을 활용한 동적 쿼리 페이징 처리의 기준점입니다.

`[스웨거 프론트(Swagger UI) API 문서 스크린샷 삽입]`
