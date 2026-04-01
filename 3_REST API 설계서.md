# REST API 설계서

## 문서 정보

| 항목 | 내용                                  |
|------|-------------------------------------|
| **프로젝트명** | 풀카운트 (Fullcount) - KBO 티켓 양도 및 커뮤니티 |
| **작성일** | 2026-03-30                          |
| **버전** | v1.3 (직관 다이어리 API 추가)             |
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
| **MEMBER** | 공개 + `/api/auth/logout`, 게시글 작성, 양도 요청, 채팅, 내 정보, 직관 다이어리 | 로그인 회원 |
| **ADMIN** | 전체 + 관리자 전용 (회원 관리, 거래 모니터링) | 시스템 관리자 |

---

## 4. 상세 API 명세

---

### 4.1 인증 API (Auth)

#### 4.1.1 회원 가입
```http
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
```http
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
```http
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
```http
POST /api/auth/logout
Authorization: Bearer {JWT_ACCESS_TOKEN}
```
**Response 200 OK:**
```json
{ "success": true, "message": "요청이 성공적으로 처리되었습니다" }
```

---

### 4.2 게시글 API (Posts)

#### 4.2.1 게시글 목록 조회
`GET /api/posts?boardType={MATE|CREW|TRANSFER}&teamId={teamId}&status={OPEN|CLOSED|RESERVED}&page=0&size=9`
- **설명**: 게시판 타입별 목록을 조회합니다. 팀별 필터링과 모집 상태 필터링을 지원합니다.
- **Query Params**:
    - `boardType`: 게시판 타입 (기본값: CREW)
    - `teamId`: 팀 필터 (CREW는 응원팀, 나머지는 홈/어웨이 팀 포함 여부)
    - `status`: 모집 상태 필터
    - `page`, `size`: 페이징 파라미터

**Response 200 OK:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "authorNickname": "야구팬1",
        "title": "잠실 직관 크루 구합니다",
        "content": "같이 응원해요!",
        "boardType": "CREW",
        "status": "OPEN",
        "viewCount": 10,
        "createdAt": "2026-04-01T10:00:00",
        "supportTeamName": "LG 트윈스",
        "maxParticipants": 4,
        "currentParticipants": 1,
        "stadium": "잠실야구장",
        "matchDate": "2026-04-05",
        "matchTime": "18:30"
      }
    ],
    "page": { "number": 0, "size": 9, "totalElements": 1, "totalPages": 1 }
  }
}
```

---

#### 4.2.2 게시글 상세 조회
`GET /api/posts/{id}`
- **설명**: 게시글의 상세 정보를 조회합니다. `boardType`에 따라 응답 필드가 달라집니다.

**Response 200 OK (CREW 예시):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "authorNickname": "야구팬1",
    "title": "잠실 직관 크루 구합니다",
    "boardType": "CREW",
    "stadium": "잠실야구장",
    "matchDate": "2026-04-05",
    "matchTime": "18:30",
    "supportTeamName": "LG 트윈스",
    "tags": ["직관", "LG", "잠실"],
    "maxParticipants": 4,
    "currentParticipants": 1,
    "status": "OPEN",
    "viewCount": 11,
    "createdAt": "2026-04-01T10:00:00"
  }
}
```

---

#### 4.2.3 게시글 작성 (다형성 지원)
`POST /api/posts`
- **Authorization**: `Bearer {JWT_TOKEN}`
- **설명**: `boardType` 값에 따라 요청 구조가 달라집니다. (Jackson 다형성 적용)

**Request (CREW - 직관 크루):**
```json
{
  "boardType": "CREW",
  "title": "LG 직관 크루 모집",
  "content": "같이 응원하실 분!",
  "supportTeamId": 1,
  "maxParticipants": 5,
  "stadium": "잠실야구장",
  "matchDate": "2026-04-10",
  "matchTime": "18:30",
  "seatArea": "레드석",
  "tags": ["무적LG", "잠실"],
  "isPublic": true
}
```

**Request (MATE - 직관 메이트):**
```json
{
  "boardType": "MATE",
  "title": "잠실 메이트 한 분 구해요",
  "content": "심심하신 분 연락주세요",
  "matchDate": "2026-04-10",
  "homeTeamId": 1,
  "awayTeamId": 2
}
```

**Request (TRANSFER - 티켓 양도):**
```json
{
  "boardType": "TRANSFER",
  "title": "3루 블루석 양도합니다",
  "content": "사정상 못가게 되어 양도합니다",
  "matchDate": "2026-04-10",
  "homeTeamId": 1,
  "awayTeamId": 2,
  "seatArea": "3루 블루석 201블록",
  "ticketPrice": 25000
}
```

---

#### 4.2.4 게시글 수정
`PUT /api/posts/{id}`
- **Authorization**: `Bearer {JWT_TOKEN}`
- **설명**: 작성자 본인만 수정 가능하며, **제목과 내용**만 수정할 수 있습니다. (상태가 OPEN인 경우만 가능)

**Request Body:**
```json
{
  "title": "수정된 제목",
  "content": "수정된 상세 내용입니다."
}
```

---

#### 4.2.5 게시글 삭제
`DELETE /api/posts/{id}`
- **Authorization**: `Bearer {JWT_TOKEN}`
- **설명**: 작성자 본인만 삭제 가능합니다. (예약 중이거나 마감된 글은 삭제 불가)

---

#### 4.2.6 크루 참여 신청
`POST /api/posts/{id}/join`
- **Authorization**: `Bearer {JWT_TOKEN}`
- **설명**: 특정 크루에 참여 신청을 합니다.
- **제약 조건**:
    - `boardType`이 `CREW`인 게시글만 가능합니다.
    - 게시글 상태가 `OPEN`이어야 합니다.
    - 모집 인원이 가득 차지 않아야 합니다.
    - 이미 참여 중인 크루에는 중복 신청할 수 없습니다.

**Response 201 Created:**
```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다"
}
```

---

#### 4.2.7 크루 참여 멤버 조회
`GET /api/posts/{id}/members`
- **설명**: 특정 크루에 참여한 모든 멤버의 정보를 조회합니다.

**Response 200 OK:**
```json
{
  "success": true,
  "data": [
    {
      "nickname": "크루장닉네임",
      "mannerTemperature": 37.5,
      "isLeader": true
    },
    {
      "nickname": "참여자1",
      "mannerTemperature": 36.5,
      "isLeader": false
    }
  ]
}
```

---

#### 4.2.8 팀 전용 게시글 목록
`GET /api/posts/team/{teamId}`
- **설명**: 특정 팀 소속 회원들만 볼 수 있는 팀 전용 게시판 목록을 조회합니다.

---

### 4.3 티켓 양도 API (Transfers)

#### 4.3.1 양도 요청 (순서 1: 양수자)
```http
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
```http
POST /api/transfers/{transferId}/pay
Authorization: Bearer {JWT_TOKEN}
```
**설명:** 양수자가 에스크로 계좌(중간 지점)로 티켓 값을 결제합니다. 돈은 아직 판매자에게 전달되지 않습니다.
**Response 200 OK:**
```json
{ "success": true, "data": { "status": "PAID" } }
```

#### 4.3.3 티켓 전달 완료 (순서 3: 양도자)
```http
POST /api/transfers/{transferId}/ticket-sent
Authorization: Bearer {JWT_TOKEN}
```
**설명:** 입금을 확인한 양도자(판매자)가 실제 티켓(핀번호 등)을 전달하고 '전달 완료' 표시를 합니다.
**Response 200 OK:**
```json
{ "success": true, "data": { "status": "TICKET_SENT" } }
```

#### 4.3.4 인수 확정 및 정산 (순서 4: 양수자)
```http
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
```http
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
```http
PUT /api/members/me/team
Authorization: Bearer {JWT_TOKEN}
```
**Request Body:** `{ "teamId": 2 }`  
**Error 422 (MEM_004)**: 시즌 중 변경 횟수 초과 시

---

### 4.5 관리자 API (Admin)

#### 4.5.1 회원 상태 변경 (ADMIN)
```http
PATCH /api/admin/members/{id}/status
Authorization: Bearer {ADMIN_TOKEN}
```
**Request Body:** `{ "isActive": false }`

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

### 4.7 직관 다이어리 API (Attendances)

#### 4.7.1 직관 기록 추가
```http
POST /api/attendances
Content-Type: multipart/form-data
Authorization: Bearer {JWT_TOKEN}
```
- **설명**: 새로운 직관 기록과 크롭된 사진(2:3 비율)을 서버 로컬 폴더에 저장합니다.
- **Request (FormData)**:
    - `date` (String/Date): 직관 날짜 (예: `2026-03-28`) (필수)
    - `result` (String): 경기 결과 (`WIN`, `LOSE`, `DRAW`, `CANCEL`) (필수)
    - `image` (File): 업로드할 이미지 파일 (선택)
- **Response 200 OK**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "date": "2026-03-28",
    "result": "WIN",
    "imageUrl": "/uploads/550e8400-e29b-41d4-a716-446655440000_cropped.jpg"
  }
}
```

#### 4.7.2 내 직관 기록 전체 조회
```http
GET /api/attendances
Authorization: Bearer {JWT_TOKEN}
```
- **설명**: 로그인한 사용자의 모든 직관 기록을 최신순(날짜 내림차순)으로 조회합니다.
- **Response 200 OK**:
```json
{
  "success": true,
  "data": [
    {
      "id": 2,
      "date": "2026-03-28",
      "result": "WIN",
      "imageUrl": "/uploads/image_name_1.jpg"
    },
    {
      "id": 1,
      "date": "2026-03-24",
      "result": "LOSE",
      "imageUrl": "/uploads/image_name_2.jpg"
    }
  ]
}
```

#### 4.7.3 직관 기록 삭제
```http
DELETE /api/attendances/{id}
Authorization: Bearer {JWT_TOKEN}
```
- **설명**: 특정 직관 기록을 삭제하며, 서버 로컬에 저장된 실제 이미지 파일도 함께 물리적으로 삭제하여 용량을 관리합니다.
- **Response 200 OK**:
```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다"
}
```

---

## 5. API 전체 목록 요약

| 메서드 | 엔드포인트 | 설명 | 권한 |
|--------|-----------|------|------|
| `GET`  | `/api/posts` | 게시글 목록 조회 (Paged) | 공개 |
| `GET`  | `/api/posts/{id}` | 게시글 상세 조회 | 공개 |
| `POST` | `/api/posts` | 게시글 작성 (CREW, MATE, TRANSFER) | MEMBER |
| `PUT`  | `/api/posts/{id}` | 게시글 수정 (제목, 내용) | MEMBER |
| `DELETE`| `/api/posts/{id}` | 게시글 삭제 | MEMBER |
| `POST` | `/api/posts/{id}/join` | 크루 참여 신청 | MEMBER |
| `GET`  | `/api/posts/{id}/members` | 크루 참여 멤버 조회 | 공개 |
| `GET`  | `/api/posts/team/{teamId}` | 팀 전용 게시판 목록 조회 | MEMBER |
| `POST` | `/api/transfers/{postId}/request` | 티켓 양도 요청 | MEMBER |
| `POST` | `/api/transfers/{transferId}/pay` | 에스크로 결제 처리 | MEMBER |
| `POST` | `/api/transfers/{transferId}/confirm` | 인수 확정 및 정산 | MEMBER |
| `GET`  | `/api/members/me` | 내 정보 조회 | MEMBER |
| `GET`  | `/api/chat/rooms` | 내 채팅방 목록 조회 (Paged) | MEMBER |
| `POST` | `/api/chat/rooms` | 그룹 채팅방 생성 | MEMBER |
| `GET`  | `/api/chat/rooms/{roomId}` | 채팅방 상세 정보 조회 | MEMBER |
| `GET`  | `/api/chat/rooms/{roomId}/messages` | 채팅 메시지 내역 조회 (Infinite Scroll) | MEMBER |
| `PATCH`| `/api/admin/members/{id}/status` | [관리자] 회원 상태 변경 | ADMIN |
| `POST` | `/api/attendances` | 직관 기록 추가 (사진 포함) | MEMBER |
| `GET`  | `/api/attendances` | 내 직관 기록 전체 조회 | MEMBER |
| `DELETE`| `/api/attendances/{id}` | 직관 기록 삭제 | MEMBER |

---

## 6. 작성 체크리스트

- [x] 전역 페이징 응답 구조(`PagedResponse`) 적용
- [x] 채팅 메시지 커서 기반 무한 스크롤(`CursorResponse`) 적용
- [x] Swagger 인증 설정 반영 및 401 에러 조치
- [x] 모든 도메인의 페이징 일관성 확보
- [x] 직관 다이어리 기록 관리 API 개발 및 서버 로컬 이미지 업로드·매핑 로직 반영

---

## 7. 기타: 데이터베이스 스키마 및 서버 설정 (Attendances)

### 7.1 `Attendance` (직관 기록 테이블)
| 컬럼명 | 타입 | 제약 조건 | 설명 |
|---|---|---|---|
| `id` | Long | PK, Auto Increment | 기록 고유 ID |
| `member_id` | Long | FK | 작성자(Member) ID |
| `match_date` | Date | Not Null | 직관 날짜 |
| `result` | Enum | Not Null | `WIN`, `LOSE`, `DRAW`, `CANCEL` |
| `image_url` | String | | 로컬 이미지 접근 경로 (예: `/uploads/...`) |

### 7.2 서버 로컬 파일 매핑 설정 (WebConfig)
- 프론트엔드에서 데이터베이스의 `image_url`을 사용해 이미지를 화면에 렌더링할 수 있도록 정적 리소스 매핑 설정됨.
- `/uploads/**` 경로로 이미지 요청이 들어오면 서버 컴퓨터의 로컬 지정 폴더(예: `C:/fullcount/uploads/`) 내 실제 파일을 반환함.