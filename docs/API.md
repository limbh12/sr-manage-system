# API 명세서

## 개요

SR Management System의 RESTful API 명세서입니다.

### Base URL
```
http://localhost:8080/api
```

### 인증
대부분의 API는 JWT 토큰 인증이 필요합니다. 요청 헤더에 다음과 같이 토큰을 포함해야 합니다:

```
Authorization: Bearer {access_token}
```

### 응답 형식
모든 응답은 JSON 형식입니다.

### HTTP 상태 코드
| 코드 | 설명 |
|------|------|
| 200 | 성공 |
| 201 | 생성 성공 |
| 400 | 잘못된 요청 |
| 401 | 인증 실패 |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 500 | 서버 오류 |

---

## 인증 API

### 로그인

사용자 로그인 및 JWT 토큰 발급

**Endpoint**
```
POST /api/auth/login
```

**Request Body**
```json
{
  "username": "string",
  "password": "string"
}
```

**Response (200 OK)**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR...",
  "tokenType": "Bearer",
  "expiresIn": 1800000
}
```

**Error Response (401 Unauthorized)**
```json
{
  "error": "UNAUTHORIZED",
  "message": "Invalid username or password"
}
```

---

### 회원가입

새로운 사용자 등록

**Endpoint**
```
POST /api/auth/register
```

**Request Body**
```json
{
  "username": "string",
  "password": "string",
  "email": "string"
}
```

**Response (201 Created)**
```json
{
  "id": 1,
  "username": "string",
  "email": "string",
  "role": "USER",
  "createdAt": "2024-01-01T00:00:00"
}
```

**Error Response (400 Bad Request)**
```json
{
  "error": "BAD_REQUEST",
  "message": "Username already exists"
}
```

---

### 토큰 갱신

Refresh Token을 사용하여 새로운 Access Token 발급

**Endpoint**
```
POST /api/auth/refresh
```

**Request Body**
```json
{
  "refreshToken": "string"
}
```

**Response (200 OK)**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR...",
  "tokenType": "Bearer",
  "expiresIn": 1800000
}
```

**Error Response (401 Unauthorized)**
```json
{
  "error": "UNAUTHORIZED",
  "message": "Invalid or expired refresh token"
}
```

---

### 로그아웃

현재 세션 종료 및 Refresh Token 무효화

**Endpoint**
```
POST /api/auth/logout
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Response (200 OK)**
```json
{
  "message": "Successfully logged out"
}
```

---

## SR 관리 API

### SR 목록 조회

SR 목록을 페이지네이션과 함께 조회

**Endpoint**
```
GET /api/sr
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Query Parameters**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| page | integer | No | 0 | 페이지 번호 |
| size | integer | No | 10 | 페이지 크기 |
| status | string | No | - | 상태 필터 (OPEN, IN_PROGRESS, RESOLVED, CLOSED) |
| priority | string | No | - | 우선순위 필터 (LOW, MEDIUM, HIGH, CRITICAL) |
| search | string | No | - | 제목/설명 검색 |

**Response (200 OK)**
```json
{
  "content": [
    {
      "id": 1,
      "title": "시스템 오류 수정 요청",
      "description": "로그인 시 오류가 발생합니다.",
      "status": "OPEN",
      "priority": "HIGH",
      "requester": {
        "id": 1,
        "username": "user1",
        "email": "user1@example.com"
      },
      "assignee": null,
      "createdAt": "2024-01-01T10:00:00",
      "updatedAt": "2024-01-01T10:00:00"
    }
  ],
  "totalElements": 100,
  "totalPages": 10,
  "size": 10,
  "number": 0,
  "first": true,
  "last": false
}
```

---

### SR 상세 조회

특정 SR의 상세 정보 조회

**Endpoint**
```
GET /api/sr/{id}
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| Parameter | Type | Description |
|-----------|------|-------------|
| id | long | SR ID |

**Response (200 OK)**
```json
{
  "id": 1,
  "title": "시스템 오류 수정 요청",
  "description": "로그인 시 오류가 발생합니다.",
  "status": "OPEN",
  "priority": "HIGH",
  "requester": {
    "id": 1,
    "username": "user1",
    "email": "user1@example.com"
  },
  "assignee": null,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

**Error Response (404 Not Found)**
```json
{
  "error": "NOT_FOUND",
  "message": "SR not found with id: 1"
}
```

---

### SR 등록

새로운 SR 생성

**Endpoint**
```
POST /api/sr
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Request Body**
```json
{
  "title": "string (required)",
  "description": "string",
  "priority": "MEDIUM",
  "assigneeId": null
}
```

**Response (201 Created)**
```json
{
  "id": 1,
  "title": "시스템 오류 수정 요청",
  "description": "로그인 시 오류가 발생합니다.",
  "status": "OPEN",
  "priority": "MEDIUM",
  "requester": {
    "id": 1,
    "username": "user1",
    "email": "user1@example.com"
  },
  "assignee": null,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

---

### SR 수정

기존 SR 정보 수정

**Endpoint**
```
PUT /api/sr/{id}
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| Parameter | Type | Description |
|-----------|------|-------------|
| id | long | SR ID |

**Request Body**
```json
{
  "title": "string",
  "description": "string",
  "priority": "HIGH",
  "assigneeId": 2
}
```

**Response (200 OK)**
```json
{
  "id": 1,
  "title": "시스템 오류 수정 요청 (수정됨)",
  "description": "로그인 시 오류가 발생합니다. 긴급 수정 필요.",
  "status": "OPEN",
  "priority": "HIGH",
  "requester": {
    "id": 1,
    "username": "user1",
    "email": "user1@example.com"
  },
  "assignee": {
    "id": 2,
    "username": "admin",
    "email": "admin@example.com"
  },
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T12:00:00"
}
```

---

### SR 삭제

SR 삭제

**Endpoint**
```
DELETE /api/sr/{id}
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| Parameter | Type | Description |
|-----------|------|-------------|
| id | long | SR ID |

**Response (204 No Content)**

---

### SR 상태 변경

SR 상태 업데이트

**Endpoint**
```
PATCH /api/sr/{id}/status
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| Parameter | Type | Description |
|-----------|------|-------------|
| id | long | SR ID |

**Request Body**
```json
{
  "status": "IN_PROGRESS"
}
```

**Response (200 OK)**
```json
{
  "id": 1,
  "title": "시스템 오류 수정 요청",
  "description": "로그인 시 오류가 발생합니다.",
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "requester": {
    "id": 1,
    "username": "user1",
    "email": "user1@example.com"
  },
  "assignee": {
    "id": 2,
    "username": "admin",
    "email": "admin@example.com"
  },
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T14:00:00"
}
```

---

## 사용자 API

### 내 정보 조회

현재 로그인한 사용자 정보 조회

**Endpoint**
```
GET /api/users/me
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Response (200 OK)**
```json
{
  "id": 1,
  "username": "user1",
  "email": "user1@example.com",
  "role": "USER",
  "createdAt": "2024-01-01T00:00:00"
}
```

---

### 사용자 목록 조회 (관리자 전용)

모든 사용자 목록 조회

**Endpoint**
```
GET /api/users
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Required Role**: ADMIN

**Query Parameters**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| page | integer | No | 0 | 페이지 번호 |
| size | integer | No | 10 | 페이지 크기 |

**Response (200 OK)**
```json
{
  "content": [
    {
      "id": 1,
      "username": "user1",
      "email": "user1@example.com",
      "role": "USER",
      "createdAt": "2024-01-01T00:00:00"
    }
  ],
  "totalElements": 50,
  "totalPages": 5,
  "size": 10,
  "number": 0
}
```

---

## 에러 응답 형식

모든 에러 응답은 다음 형식을 따릅니다:

```json
{
  "error": "ERROR_CODE",
  "message": "Human readable error message",
  "timestamp": "2024-01-01T00:00:00",
  "path": "/api/sr/1"
}
```

### 에러 코드 목록

| 에러 코드 | HTTP 상태 | 설명 |
|-----------|-----------|------|
| BAD_REQUEST | 400 | 잘못된 요청 |
| UNAUTHORIZED | 401 | 인증 필요 |
| FORBIDDEN | 403 | 접근 권한 없음 |
| NOT_FOUND | 404 | 리소스를 찾을 수 없음 |
| CONFLICT | 409 | 리소스 충돌 |
| INTERNAL_SERVER_ERROR | 500 | 서버 내부 오류 |
