# ⚾ 풀카운트 (Full Count) - Backend

야구 팬들을 위한 커뮤니티 및 안전한 에스크로 티켓 양도 플랫폼, **풀카운트**의 백엔드 저장소입니다.

## 🚀 주요 기능 (Backend)
- **게시판 다형성 API**: 직관 크루(CREW), 메이트(MATE), 티켓 양도(TRANSFER) 등 타입별 맞춤형 데이터 처리 및 조회
- **에스크로 티켓 양도**: 안전한 거래를 위한 5단계 상태 전이 시스템 및 정산 비즈니스 로직 구현
- **실시간 채팅 서버**: WebSocket 및 STOMP 프로토콜 기반의 1:1 및 그룹 채팅 브로커 구축
- **보안 및 인증**: JWT(JSON Web Token) 기반의 Stateless 인증 체계 및 Spring Security 권한 제어
- **관리자 시스템**: Thymeleaf 기반의 회원 관리, 거래 모니터링 및 데이터 통계 대시보드

## 🛠 기술 스택
- **Framework**: Spring Boot 3.2.5
- **Language**: Java 17
- **Database**: H2 (Dev), MySQL (Prod)
- **ORM**: Spring Data JPA (Hibernate 6)
- **Security**: Spring Security, JWT
- **Real-time**: Spring WebSocket, STOMP
- **Documentation**: SpringDoc OpenAPI 2.5 (Swagger)

## 🔗 Repository
- **Backend**: [FullCount-BE](https://github.com/Rookies5-MiniPj2-Team5/FullCount-BE)
- **Frontend**: [FullCount-FE](https://github.com/Rookies5-MiniPj2-Team5/FullCount-FE)

---

## 🏃 실행 방법

백엔드 서버를 기동하기 위한 단계입니다.

```powershell
# 레포지토리 클론
git clone https://github.com/Rookies5-MiniPj2-Team5/FullCount-BE.git
cd FullCount-BE

# 애플리케이션 실행 (Windows)
.\gradlew.bat bootRun
```
- **API Swagger**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **H2 Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
- **Admin Page**: [http://localhost:8080/admin/dashboard](http://localhost:8080/admin/dashboard)

---

## 📁 프로젝트 구조
```text
src/main/java/com/fullcount/
├── controller/   # API 엔드포인트 정의
├── service/      # 비즈니스 로직 처리
├── domain/       # JPA 엔티티 및 도메인 모델
├── repository/   # 데이터베이스 액세스 (Spring Data JPA)
├── dto/          # 계층 간 데이터 전송 객체 (Request/Response)
├── mapper/       # Entity <-> DTO 변환 로직
├── security/     # JWT 필터 및 인증/인가 설정
├── config/       # 전역 설정 (Swagger, WebSocket, JPA 등)
└── exception/    # 전역 예외 처리 및 커스텀 에러 코드
```

---

## 🛠 Git Branch Strategy

본 프로젝트는 원활한 협업을 위해 다음과 같은 브랜치 전략을 따릅니다.

- **main**: 코드 통합 및 배포를 위한 기준 브랜치입니다.
- **feature/{기능명}**: 각자 맡은 기능 개발을 진행하는 개인 작업 브랜치입니다.
  - 예: `feature/post-api`, `feature/chat-server`

### 협업 프로세스
1. 본인의 작업 브랜치에서 개발을 완료합니다. (`git checkout -b feature/my-feature`)
2. 작업 내용을 원격 레포지토리에 Push 합니다.
3. GitHub에서 **Pull Request(PR)**를 생성하여 팀원들에게 공유합니다.
4. 팀원의 리뷰 또는 확인을 거친 후 `main` 브랜치로 **Merge** 합니다.

### 원격 저장소 설정 방법
기존 프로젝트의 원격 저장소를 백엔드 전용 레포지토리로 교체하려면 아래 명령어를 사용하세요.
```powershell
# 기존 origin 삭제
git remote remove origin

# 새 origin 등록 (Backend 기준)
git remote add origin https://github.com/Rookies5-MiniPj2-Team5/FullCount-BE.git

# 현재 연결 상태 확인
git remote -v
```
