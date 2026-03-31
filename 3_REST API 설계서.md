# REST API 설계서

## 문서 정보

| 항목 | 내용                                  |
|------|-------------------------------------|
| **프로젝트명** | 풀카운트 (Fullcount) - KBO 티켓 양도 및 커뮤니티 |
| **작성일** | 2026-03-30                          |
| **버전** | v1.2                                |
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

#### 성공 응답 (단일 객체/메시지)
```json
{
  "success": true,
  "data": { },
  "message": "요청이 성공적으로 처리되었습니다",
  "timestamp": "2026-03-30T10:30:00Z"
}
```

#### 성공 응답 (목록/페이지네이션 - PagedResponse)
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

#### 성공 응답 (무한 스크롤 - CursorResponse)
```json
{
  "success": true,
  "data": {
    "content": [ ],
    "nextCursor": 123,
    "hasNext": true
  }
}
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
**Response 200 OK:**
```json
{ "success": true, "message": "요청이 성공적으로 처리되었습니다" }
```

---

#### 4.1.2 로그인 (JWT 토큰 발급)
```
POST /api/auth/login
Content-Type: application/json
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

---

### 4.2 게시글 API (Posts)

#### 4.2.1 게시글 목록 조회 (공개)
`GET /api/posts?boardType=GENERAL&page=0&size=10`
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
    "page": { "number": 0, "size": 10, "totalElements": 150, "totalPages": 15 }
  }
}
```

---

### 4.3 티켓 양도 API (Transfers)

#### 4.3.1 양도 요청 (순서 1: 양수자)
`POST /api/transfers/{postId}/request`
**Response 201 Created:**
```json
{
  "success": true,
  "data": { "transferId": 42, "chatRoomId": 101, "status": "REQUESTED" }
}
```

---

### 4.6 채팅 API (Chat)

#### 4.6.1 내 채팅방 목록 조회 (MEMBER)
`GET /api/chat/rooms?page=0&size=10`
- **Authorization**: `Bearer {JWT_TOKEN}`
- **설명**: 현재 로그인한 사용자가 참여 중인 채팅방 목록을 조회합니다. (일반 페이징 적용)

**Response 200 OK:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "chatRoomId": 1,
        "type": "ONE_ON_ONE",
        "title": "티켓 양도 채팅방",
        "lastMessage": "안녕하세요. 아직 거래 가능할까요?",
        "lastMessageAt": "2026-03-31T13:40:22",
        "unreadCount": 2
      }
    ],
    "page": { "number": 0, "size": 10, "totalElements": 1, "totalPages": 1 }
  }
}
```

#### 4.6.2 그룹 채팅방 생성 (MEMBER)
`POST /api/chat/rooms?postId={postId}&type={GROUP_JOIN|GROUP_CREW}`
**Response 201 Created:**
```json
{
  "success": true,
  "data": { "chatRoomId": 15 }
}
```

#### 4.6.5 채팅 메시지 내역 조회 (MEMBER)
`GET /api/chat/rooms/{roomId}/messages?lastMessageId=100&size=20`
- **Authorization**: `Bearer {JWT_TOKEN}`
- **설명**: 특정 채팅방의 이전 메시지 내역을 조회합니다. **커서 기반 무한 스크롤**을 위해 최신순으로 반환됩니다.

**Response 200 OK:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "messageId": 99,
        "senderId": 2,
        "senderNickname": "홈런왕",
        "content": "안녕하세요. 구매자입니다.",
        "timestamp": "2026-03-31T14:25:30"
      }
    ],
    "nextCursor": 80,
    "hasNext": true
  }
}
```

#### 4.6.6 채팅방 상세 조회 (MEMBER)
`GET /api/chat/rooms/{roomId}`
**Response 200 OK:**
```json
{
  "success": true,
  "data": {
    "chatRoomId": 1,
    "type": "ONE_ON_ONE",
    "participants": [
      { "memberId": 2, "nickname": "홈런왕" },
      { "memberId": 3, "nickname": "양도자닉네임" }
    ]
  }
}
```

---

## 5. API 전체 목록 요약

| 메서드 | 엔드포인트 | 설명 | 권한 |
|--------|-----------|------|------|
| `GET`  | `/api/posts` | 게시글 목록 조회 (Paged) | 공개 |
| `GET`  | `/api/chat/rooms` | 내 채팅방 목록 조회 (Paged) | MEMBER |
| `GET`  | `/api/chat/rooms/:roomId/messages` | 채팅 메시지 내역 조회 (Infinite Scroll) | MEMBER |
| ... | ... | ... | ... |

---

## 6. 작성 체크리스트

- [x] 전역 페이징 응답 구조(`PagedResponse`) 적용
- [x] 채팅 메시지 커서 기반 무한 스크롤(`CursorResponse`) 적용
- [x] Swagger 인증 설정 반영 및 401 에러 조치
- [x] 모든 도메인의 페이징 일관성 확보
