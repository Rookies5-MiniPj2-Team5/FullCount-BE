# ⚾ 풀카운트 (Full Count)

야구 팬들을 위한 커뮤니티 및 안전한 에스크로 티켓 양도 플랫폼입니다.

## 🚀 주요 기능
- **직관 메이트 모집**: 선호 구단별 직관 파티원 모집 및 참여
- **에스크로 티켓 양도**: 5단계 상태 전이 시스템을 통한 안전한 티켓 거래
- **실시간 채팅**: WebSocket(STOMP) 기반 1:1 및 단체 채팅
- **마이페이지**: 내 직관 일정 관리 및 활동 통계 확인
- **관리자 시스템**: Thymeleaf 기반 회원 및 거래 모니터링 대시보드

## 🛠 기술 스택
### Backend
- **Framework**: Spring Boot 3.2.5
- **Language**: Java 17
- **Database**: H2 (Dev), MySQL (Prod)
- **Security**: Spring Security + JWT (Stateless)
- **Real-time**: Spring WebSocket + STOMP
- **Documentation**: SpringDoc OpenAPI (Swagger)
- **UI**: Thymeleaf (Admin Page)

### Frontend
- **Framework**: React 18 (Vite)
- **Styling**: Vanilla CSS (Custom Mobile-First Design)

## 🔗 Repository
- **Backend**: [FullCount-BE](https://github.com/Rookies5-MiniPj2-Team5/FullCount-BE)
- **Frontend**: [FullCount-FE](https://github.com/Rookies5-MiniPj2-Team5/FullCount-FE)

---

## 🏃 실행 방법

### 1. Backend (Spring Boot)
백엔드 서버를 기동하기 위한 단계입니다.
```powershell
# 레포지토리 클론
git clone https://github.com/Rookies5-MiniPj2-Team5/FullCount-BE.git
cd FullCount-BE

# 애플리케이션 실행 (Windows)
.\gradlew.bat bootRun
```
- **API Swagger**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **H2 Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:fullcountdb`)
- **Admin Page**: [http://localhost:8080/admin/dashboard](http://localhost:8080/admin/dashboard) (ID/PW: `admin`/`admin`) (아직 구현 X)

### 2. Frontend (React)
프론트엔드 개발 서버를 기동하기 위한 단계입니다.
```powershell
# 레포지토리 클론
git clone https://github.com/Rookies5-MiniPj2-Team5/FullCount-FE.git
cd FullCount-FE

# 의존성 설치 및 실행
npm install
npm run dev
```
- **URL**: [http://localhost:5173](http://localhost:5173)

---

## 📁 프로젝트 구조 (Backend 기준)
- `src/main/java/com/fullcount/` : 백엔드 도메인 및 비즈니스 로직
- `src/main/resources/templates/` : 관리자 페이지 Thymeleaf 템플릿
- `3_REST API 설계서.md` : 상세 API 명세서
- `PRD_풀카운트.md` : 프로젝트 기획 및 상세 요구사항 문서
