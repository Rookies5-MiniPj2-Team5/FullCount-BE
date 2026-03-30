# REST API 설계서

## 문서 정보

| 항목 | 내용                                  |
|------|-------------------------------------|
| **프로젝트명** | 풀카운트 (Fullcount) - KBO 티켓 양도 및 커뮤니티 |
| **작성일** | 2026-03-30                          |
| **버전** | v1.0                                |
| **Base URL** | `http://localhost:8080/api`         |

---

## 1. API 설계 개요

### 1.1 설계 목적

KBO 팬들을 위한 안전한 티켓 양도(에스크로 기반) 및 커뮤니티 기능을 제공하기 위해 클라이언트(React)와 서버(Spring Boot) 간의 통신 규격을 정의한다.

### 1.2 설계 원칙

- **RESTful**: HTTP 메서드와 상태 코드의 명확한 사용
- **일관성**: 모든 API에서 동일한 응답 구조 사용
- **보안**: JWT 기반 인증, `/api/auth/**` (로그아웃 제외) 및 일부 조회 API 제외 보호
- **성능**: 게시글 및 회원 목록 페이징 처리 (기본 size=10)

### 1.3 기술 스택

| 항목 | 기술 |
|------|------|
| 프레임워크 | Spring Boot 3.4.6 |
| 인증 | JWT (액세스 토큰 1시간, 리프레시 토큰 7일) |
| 직렬화 | JSON |
| API 문서 | OpenAPI 3.0 (Swagger) |

---

## 2. API 공통 규칙

### 2.1 URL 설계 규칙

| 규칙 | 좋은 예 | 나쁜 예 |
|------|---------|---------|
| 명사 사용 | `GET /api/posts` | `GET /api/getPosts` |
| 복수형 사용 | `/api/posts`, `/api/teams` | `/api/post`, `/api/team` |
| 계층 구조 | `/api/posts/1/request` | `/api/requestTransfer` |
| 소문자+하이픈 | `/api/team-boards` | `/api/teamBoards` |
| HTTP 메서드로 동작 표현 | `POST /api/transfers` | `/api/createTransfer` |

### 2.2 HTTP 메서드 사용 규칙

| 메서드 | 용도 | 멱등성 | 예시 |
|--------|------|--------|------|
| `GET` | 리소스 조회 | ✅ | `GET /api/posts` |
| `POST` | 리소스 생성/액션 | ❌ | `POST /api/auth/signup` |
| `PUT` | 리소스 전체 수정 | ✅ | `PUT /api/members/me` |
| `PATCH` | 리소스 부분 수정 | ❌ | `PATCH /api/admin/members/1/status` |
| `DELETE` | 리소스 삭제 | ✅ | `DELETE /api/posts/1` |

### 2.3 공통 응답 구조

#### 성공 응답 (단일 객체)
```json
{
  "success": true,
  "data": { },
  "message": "요청이 성공적으로 처리되었습니다",
  "timestamp": "2026-03-30T10:30:00Z"
}
```

#### 성공 응답 (목록/페이지네이션)
```json
{
  "success": true,
  "data": {
    "content": [ ],
    "page": {
      "number": 0,
      "size": 10,
      "totalElements": 50,
      "totalPages": 5
    }
  }
}
```

#### 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "DUPLICATE_EMAIL",
    "message": "이미 사용 중인 이메일입니다",
    "timestamp": "2026-03-30T10:30:00Z"
  }
}
```

### 2.4 HTTP 상태 코드

| 코드 | 상태 | 사용 예시 |
|------|------|-----------|
| `200` | OK | GET/PUT/PATCH 성공 |
| `201` | Created | POST 성공 (회원가입, 게시글 작성) |
| `204` | No Content | DELETE 성공, 로그아웃 성공 |
| `400` | Bad Request | 유효성 검사 실패 |
| `401` | Unauthorized | 토큰 없음 또는 만료, 로그인 실패 |
| `403` | Forbidden | 권한 없음, 비활성화된 계정 |
| `404` | Not Found | 존재하지 않는 리소스 (회원, 팀 등) |
| `409` | Conflict | 중복 데이터 (이메일, 닉네임) |
| `422` | Unprocessable Entity | 비즈니스 규칙 위반 |

### 2.5 공통 요청 헤더

```
Content-Type: application/json
Accept: application/json
Authorization: Bearer {JWT_ACCESS_TOKEN}   # 인증 필요한 API에만
```

---

## 3. 인증 및 권한 관리

### 3.1 권한 레벨

| 역할 | 접근 가능 API | 설명 |
|------|--------------|------|
| **공개(미인증)** | `GET /api/posts`, `GET /api/teams`, `/api/auth/(signup, login, refresh)` | 조회 및 로그인 프로세스 |
| **MEMBER** | 공개 + `/api/auth/logout`, 게시글 작성, 양도 요청, 채팅, 내 정보 | 로그인 회원 |
| **ADMIN** | 전체 + 관리자 전용 (회원 관리, 거래 모니터링) | 시스템 관리자 |

---

## 4. 상세 API 명세

---

### 4.1 인증 API (Auth)

#### 4.1.1 회원 가입
```
POST /api/auth/signup
Content-Type: application/json
```
**Request Body:**
```json
{
  "email": "user@example.com",
  "nickname": "홈런왕",
  "password": "password123!",
  "teamId": 1
}
```
**Response 200 OK:**
```json
{ "success": true, "message": "요청이 성공적으로 처리되었습니다" }
```
**에러 발생 케이스:**
- **409 Conflict (DUPLICATE_EMAIL)**: 이미 사용 중인 이메일
- **409 Conflict (DUPLICATE_NICKNAME)**: 이미 사용 중인 닉네임
- **404 Not Found (TEAM_NOT_FOUND)**: 존재하지 않는 팀 ID

---

#### 4.1.2 로그인 (JWT 토큰 발급)
```
POST /api/auth/login
Content-Type: application/json
```
**Request Body:**
```json
{ "email": "user@example.com", "password": "password123!" }
```
**Response 200 OK:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1Ni...",
    "refreshToken": "eyJhbGciOiJIUzI1Ni...",
    "tokenType": "Bearer"
  }
}
```
**에러 발생 케이스:**
- **404 Not Found (MEMBER_NOT_FOUND)**: 가입되지 않은 이메일
- **403 Forbidden (INACTIVE_MEMBER)**: 비활성화된 계정
- **401 Unauthorized (INVALID_CREDENTIALS)**: 비밀번호 불일치

---

#### 4.1.3 토큰 재발급
```
POST /api/auth/refresh
Content-Type: application/json
```
**Request Body:**
```json
{ "refreshToken": "eyJhbGciOiJIUzI1Ni..." }
```
**Response 200 OK:**
```json
{
  "success": true,
  "data": { "accessToken": "eyJhbGci...", "refreshToken": "eyJhbGci..." }
}
```
**에러 발생 케이스:**
- **401 Unauthorized (INVALID_TOKEN)**: 유효하지 않은 Refresh Token
- **401 Unauthorized (EXPIRED_TOKEN)**: 만료된 Refresh Token

---

#### 4.1.4 로그아웃
```
POST /api/auth/logout
Authorization: Bearer {JWT_ACCESS_TOKEN}
```
**Response 200 OK:**
```json
{ "success": true, "message": "요청이 성공적으로 처리되었습니다" }
```

---

### 4.2 게시글 API (Posts)

#### 4.2.1 게시글 목록 조회 (공개)
```
GET /api/posts?boardType=GENERAL&page=0&size=10
```
**Response 200 OK:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "개막전 같이 가실 분!",
        "authorNickname": "야구팬",
        "boardType": "GENERAL",
        "status": "OPEN",
        "createdAt": "2026-03-30T10:00:00"
      }
    ],
    "page": { "totalElements": 150, "totalPages": 15 }
  }
}
```

#### 4.2.2 팀 전용 게시판 조회 (MEMBER)
```
GET /api/posts/team/{teamId}
Authorization: Bearer {JWT_TOKEN}
```
**Error 403 Forbidden (TEAM_BOARD_ACCESS_DENIED)**: 다른 팀 게시판 접근 시

---

### 4.3 티켓 양도 API (Transfers)

#### 4.3.1 양도 요청 (순서 1: 양수자)
```
POST /api/transfers/{postId}/request
Authorization: Bearer {JWT_TOKEN}
```
**설명:** 게시글을 보고 양수자(구매 희망자)가 양도 요청을 보냅니다. 이때 채팅방이 자동으로 생성됩니다.
**Response 201 Created:**
```json
{
  "success": true,
  "data": { "transferId": 42, "chatRoomId": 101, "status": "REQUESTED" }
}
```
**Error 422 (TRF_004)**: 자신의 게시글에 양도 요청 시

#### 4.3.2 에스크로 결제 (순서 2: 양수자)
```
POST /api/transfers/{transferId}/pay
Authorization: Bearer {JWT_TOKEN}
```
**설명:** 양수자가 에스크로 계좌(중간 지점)로 티켓 값을 결제합니다. 돈은 아직 판매자에게 전달되지 않습니다.
**Response 200 OK:**
```json
{ "success": true, "data": { "status": "PAID" } }
```

#### 4.3.3 티켓 전달 완료 (순서 3: 양도자)
```
POST /api/transfers/{transferId}/ticket-sent
Authorization: Bearer {JWT_TOKEN}
```
**설명:** 입금을 확인한 양도자(판매자)가 실제 티켓(핀번호 등)을 전달하고 '전달 완료' 표시를 합니다.
**Response 200 OK:**
```json
{ "success": true, "data": { "status": "TICKET_SENT" } }
```

#### 4.3.4 인수 확정 및 정산 (순서 4: 양수자)
```
POST /api/transfers/{transferId}/confirm
Authorization: Bearer {JWT_TOKEN}
```
**설명:** 티켓을 정상적으로 받은 양수자가 '인수 확정'을 누릅니다. 이때 판매자에게 정산(돈 입금)이 완료됩니다.
**Response 200 OK:**
```json
{ "success": true, "data": { "status": "COMPLETED" } }
```

---

### 4.4 회원 API (Members)

#### 4.4.1 내 정보 조회 (MEMBER)
```
GET /api/members/me
Authorization: Bearer {JWT_TOKEN}
```
**Response 200 OK:**
```json
{
  "success": true,
  "data": { "id": 1, "email": "user@example.com", "nickname": "홈런왕", "teamName": "LG 트윈스" }
}
```

#### 4.4.2 응원 팀 변경 (MEMBER)
```
PUT /api/members/me/team
Authorization: Bearer {JWT_TOKEN}
```
**Request Body:** `{ "teamId": 2 }`
**Error 422 (MEM_004)**: 시즌 중 변경 횟수 초과 시

---

### 4.5 관리자 API (Admin)

#### 4.5.1 회원 상태 변경 (ADMIN)
```
PATCH /api/admin/members/{id}/status
Authorization: Bearer {ADMIN_TOKEN}
```
**Request Body:** `{ "isActive": false }`

---

### 4.6 채팅 API (Chat)

#### 4.6.1 내 채팅방 목록 조회 (MEMBER)
```
GET /api/chat/rooms
Authorization: Bearer {JWT_TOKEN}
```

---

## 5. API 전체 목록 요약

| 메서드 | 엔드포인트 | 설명 | 권한 |
|--------|-----------|------|------|
| `POST` | `/api/auth/signup` | 회원가입 | 공개 |
| `POST` | `/api/auth/login` | 로그인 | 공개 |
| `POST` | `/api/auth/refresh` | 토큰 재발급 | 공개 |
| `POST` | `/api/auth/logout` | 로그아웃 | MEMBER |
| `GET` | `/api/posts` | 게시글 목록 조회 | 공개 |
| `POST` | `/api/posts` | 게시글 작성 | MEMBER |
| `POST` | `/api/transfers/:postId/request` | 양도 요청 | MEMBER |
| `POST` | `/api/transfers/:transferId/pay` | 에스크로 결제 | MEMBER |
| `POST` | `/api/transfers/:transferId/ticket-sent` | 티켓 전달 완료 | MEMBER |
| `POST` | `/api/transfers/:transferId/confirm` | 인수 확정 (정산) | MEMBER |
| `GET` | `/api/members/me` | 내 정보 조회 | MEMBER |
| `PUT` | `/api/members/me/team` | 응원 팀 변경 | MEMBER |
| `GET` | `/api/admin/members` | 회원 관리 목록 | ADMIN |
| `PATCH` | `/api/admin/members/:id/status` | 회원 상태 변경 | ADMIN |

---

## 6. 작성 체크리스트

- [x] 인증 API 전항목 (`signup`, `login`, `refresh`, `logout`) 상세 정의
- [x] 비즈니스 예외 상황 (`ErrorCode`) 및 상태 코드 매핑 반영
- [x] 게시글, 양도, 회원, 관리자, 채팅 API 누락 없이 포함
- [x] Request/Response JSON 예시 (DTO 기준) 작성
- [x] 권한 레벨 (공개/MEMBER/ADMIN) 명시
 `/api/auth/refresh` | 토큰 재발급 | 공개 |
| `POST` | `/api/auth/logout` | 로그아웃 | MEMBER |
| `GET` | `/api/posts` | 게시글 목록 조회 | 공개 |
| `POST` | `/api/posts` | 게시글 작성 | MEMBER |
| `POST` | `/api/transfers/:postId/request` | 양도 요청 | MEMBER |
| `GET` | `/api/members/me` | 내 정보 조회 | MEMBER |
| `PUT` | `/api/members/me/team` | 응원 팀 변경 | MEMBER |
| `GET` | `/api/admin/members` | 회원 관리 목록 | ADMIN |
| `PATCH` | `/api/admin/members/:id/status` | 회원 상태 변경 | ADMIN |

---

## 6. 작성 체크리스트

- [x] 인증 API 전항목 (`signup`, `login`, `refresh`, `logout`) 상세 정의
- [x] 비즈니스 예외 상황 (`ErrorCode`) 및 상태 코드 매핑 반영
- [x] 게시글, 양도, 회원, 관리자, 채팅 API 누락 없이 포함
- [x] Request/Response JSON 예시 (DTO 기준) 작성
- [x] 권한 레벨 (공개/MEMBER/ADMIN) 명시
