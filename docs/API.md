# API 명세서

## 개요

SR Management System의 RESTful API 명세서입니다.

> **최종 업데이트**: 2025-12-22 | Wiki API 추가

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
| search | string | No | - | 제목/설명/SR ID 검색 |

**Response (200 OK)**
```json
{
  "content": [
    {
      "id": 1,
      "srId": "SR-2412-0001",
      "title": "시스템 오류 수정 요청",
      "description": "로그인 시 오류가 발생합니다.",
      "status": "OPEN",
      "priority": "HIGH",
      "requester": {
        "id": 1,
        "username": "user1",
        "name": "홍길동",
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
  "srId": "SR-2412-0001",
  "title": "시스템 오류 수정 요청",
  "description": "로그인 시 오류가 발생합니다.",
  "processingDetails": "로그 확인 결과 DB 커넥션 풀 문제로 확인됨.",
  "status": "OPEN",
  "priority": "HIGH",
  "requester": {
    "id": 1,
    "username": "user1",
    "name": "홍길동",
    "email": "user1@example.com"
  },
  "assignee": null,
  "openApiSurveyId": null,
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
  "assigneeId": null,
  "openApiSurveyId": null
}
```

**Response (201 Created)**
```json
{
  "id": 1,
  "srId": "SR-2412-0001",
  "title": "시스템 오류 수정 요청",
  "description": "로그인 시 오류가 발생합니다.",
  "status": "OPEN",
  "priority": "MEDIUM",
  "requester": {
    "id": 1,
    "username": "user1",
    "name": "홍길동",
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
  "processingDetails": "string",
  "priority": "HIGH",
  "assigneeId": 2,
  "openApiSurveyId": null
}
```

**Response (200 OK)**
```json
{
  "id": 1,
  "srId": "SR-2412-0001",
  "title": "시스템 오류 수정 요청 (수정됨)",
  "description": "로그인 시 오류가 발생합니다. 긴급 수정 필요.",
  "processingDetails": "처리 중...",
  "status": "OPEN",
  "priority": "HIGH",
  "requester": {
    "id": 1,
    "username": "user1",
    "name": "홍길동",
    "email": "user1@example.com"
  },
  "assignee": {
    "id": 2,
    "username": "admin",
    "name": "관리자",
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

### SR 이력 조회

특정 SR의 변경 이력 및 댓글 조회

**Endpoint**
```
GET /api/sr/{id}/histories
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
[
  {
    "id": 1,
    "content": "상태가 OPEN에서 IN_PROGRESS로 변경되었습니다.",
    "historyType": "STATUS_CHANGE",
    "previousValue": "OPEN",
    "newValue": "IN_PROGRESS",
    "createdBy": {
      "id": 2,
      "username": "admin",
      "name": "관리자"
    },
    "createdAt": "2024-01-01T10:30:00"
  },
  {
    "id": 2,
    "content": "확인 중입니다.",
    "historyType": "COMMENT",
    "previousValue": null,
    "newValue": null,
    "createdBy": {
      "id": 2,
      "username": "admin",
      "name": "관리자"
    },
    "createdAt": "2024-01-01T10:35:00"
  }
]
```

---

### SR 댓글 등록

SR에 댓글(이력) 등록

**Endpoint**
```
POST /api/sr/{id}/histories
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
  "content": "추가 요청사항이 있습니다."
}
```

**Response (201 Created)**
```json
{
  "id": 3,
  "content": "추가 요청사항이 있습니다.",
  "historyType": "COMMENT",
  "createdBy": {
    "id": 1,
    "username": "user1",
    "name": "홍길동"
  },
  "createdAt": "2024-01-01T11:00:00"
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
  "name": "홍길동",
  "email": "user1@example.com",
  "role": "USER",
  "createdAt": "2024-01-01T00:00:00"
}
```

---

### 내 정보 수정

현재 로그인한 사용자 정보 수정

**Endpoint**
```
PUT /api/users/me
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Request Body**
```json
{
  "name": "홍길동",
  "email": "newemail@example.com"
}
```

**Response (200 OK)**
```json
{
  "id": 1,
  "username": "user1",
  "name": "홍길동",
  "email": "newemail@example.com",
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
      "name": "홍길동",
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

### 사용자 생성 (관리자 전용)

새로운 사용자 생성

**Endpoint**
```
POST /api/users
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Required Role**: ADMIN

**Request Body**
```json
{
  "username": "newuser",
  "name": "신규사용자",
  "password": "password123",
  "email": "newuser@example.com",
  "role": "USER"
}
```

**Response (201 Created)**
```json
{
  "id": 2,
  "username": "newuser",
  "name": "신규사용자",
  "email": "newuser@example.com",
  "role": "USER",
  "createdAt": "2024-01-01T12:00:00"
}
```

---

### 사용자 수정 (관리자 전용)

사용자 정보 수정

**Endpoint**
```
PUT /api/users/{id}
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Required Role**: ADMIN

**Path Parameters**
| Parameter | Type | Description |
|-----------|------|-------------|
| id | long | 사용자 ID |

**Request Body**
```json
{
  "name": "수정된이름",
  "email": "updated@example.com",
  "role": "ADMIN"
}
```

**Response (200 OK)**
```json
{
  "id": 2,
  "username": "newuser",
  "name": "수정된이름",
  "email": "updated@example.com",
  "role": "ADMIN",
  "createdAt": "2024-01-01T12:00:00"
}
```

---

### 사용자 삭제 (관리자 전용)

사용자 삭제

**Endpoint**
```
DELETE /api/users/{id}
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Required Role**: ADMIN

**Path Parameters**
| Parameter | Type | Description |
|-----------|------|-------------|
| id | long | 사용자 ID |

**Response (204 No Content)**

---

## Open API 현황조사 API

### 현황조사 목록 조회

현황조사 목록을 페이지네이션과 함께 조회

**Endpoint**
```
GET /api/surveys
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
| keyword | string | No | - | 검색어 (기관명 등) |
| currentMethod | string | No | - | 현행 방식 필터 |
| desiredMethod | string | No | - | 희망 방식 필터 |

**Response (200 OK)**
```json
{
  "content": [
    {
      "id": 1,
      "organizationName": "한국정보화진흥원",
      "department": "디지털플랫폼정부본부",
      "contactName": "김담당",
      "contactPhone": "053-123-4567",
      "contactEmail": "kim@nia.or.kr",
      "systemName": "공공데이터포털",
      "currentMethod": "CENTRAL",
      "desiredMethod": "CENTRAL_IMPROVED",
      "createdAt": "2024-01-01T10:00:00",
      "updatedAt": "2024-01-01T10:00:00"
    }
  ],
  "totalElements": 50,
  "totalPages": 5,
  "size": 10,
  "number": 0
}
```

---

### 현황조사 상세 조회

특정 현황조사의 상세 정보 조회

**Endpoint**
```
GET /api/surveys/{id}
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| Parameter | Type | Description |
|-----------|------|-------------|
| id | long | 현황조사 ID |

**Response (200 OK)**
```json
{
  "id": 1,
  "organizationName": "한국정보화진흥원",
  "department": "디지털플랫폼정부본부",
  "contactName": "김담당",
  "contactPhone": "053-123-4567",
  "contactEmail": "kim@nia.or.kr",
  "receivedFileName": "공문.pdf",
  "receivedDate": "2024-01-01",
  "systemName": "공공데이터포털",
  "currentMethod": "CENTRAL",
  "desiredMethod": "CENTRAL_IMPROVED",
  "maintenanceOperation": "INTERNAL",
  "maintenanceLocation": "INTERNAL",
  "maintenanceNote": "담당자: 홍길동, 010-1234-5678",
  "operationEnv": "OPS",
  "webServerOs": "LINUX",
  "webServerType": "APACHE",
  "wasServerType": "TOMCAT",
  "dbServerType": "POSTGRESQL",
  "devLanguage": "JAVA",
  "devFramework": "SPRING_BOOT",
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

---

### 현황조사 등록

새로운 현황조사 생성

**Endpoint**
```
POST /api/surveys
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Request Body**
```json
{
  "organizationName": "한국정보화진흥원",
  "department": "디지털플랫폼정부본부",
  "contactName": "김담당",
  "contactPhone": "053-123-4567",
  "contactEmail": "kim@nia.or.kr",
  "receivedDate": "2024-01-01",
  "systemName": "공공데이터포털",
  "currentMethod": "CENTRAL",
  "desiredMethod": "CENTRAL_IMPROVED",
  "maintenanceOperation": "INTERNAL",
  "maintenanceLocation": "INTERNAL",
  "operationEnv": "OPS"
}
```

**Response (201 Created)**
```json
{
  "id": 1,
  "organizationName": "한국정보화진흥원",
  "createdAt": "2024-01-01T10:00:00"
}
```

---

### 현황조사 수정

기존 현황조사 정보 수정

**Endpoint**
```
PUT /api/surveys/{id}
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| Parameter | Type | Description |
|-----------|------|-------------|
| id | long | 현황조사 ID |

**Request Body**
```json
{
  "organizationName": "한국정보화진흥원",
  "department": "디지털플랫폼정부본부",
  "contactName": "김담당",
  "contactPhone": "053-123-4567",
  "contactEmail": "kim@nia.or.kr",
  "receivedFileName": "수정된_공문.pdf",
  "receivedDate": "2024-01-01",
  "systemName": "공공데이터포털",
  "currentMethod": "CENTRAL",
  "desiredMethod": "CENTRAL_IMPROVED",
  "maintenanceOperation": "PROFESSIONAL_RESIDENT",
  "maintenanceLocation": "INTERNAL",
  "maintenanceNote": "담당자 변경",
  "operationEnv": "OPS"
}
```

**Response (200 OK)**
```json
{
  "id": 1,
  "organizationName": "한국정보화진흥원",
  "updatedAt": "2024-01-01T12:00:00"
}
```

---

### 현황조사 파일 다운로드

현황조사 첨부 파일 다운로드

**Endpoint**
```
GET /api/surveys/{id}/download
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| Parameter | Type | Description |
|-----------|------|-------------|
| id | long | 현황조사 ID |

**Response (200 OK)**
- Content-Type: application/octet-stream
- Content-Disposition: attachment; filename="filename.ext"

---

## 행정기관 API

### 기관 검색

행정표준코드 또는 기관명으로 기관 검색

**Endpoint**
```
GET /api/organizations
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Query Parameters**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| keyword | string | Yes | - | 검색어 (기관명 또는 코드) |

**Response (200 OK)**
```json
[
  {
    "code": "1741000",
    "name": "행정안전부"
  },
  {
    "code": "3000000",
    "name": "서울특별시"
  }
]
```

---

## Wiki API

### 문서 관리

#### 문서 목록 조회

Wiki 문서 목록을 조회합니다.

**Endpoint**
```
GET /api/wiki/documents
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Query Parameters**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| categoryId | long | No | - | 카테고리 ID로 필터링 |

**Response (200 OK)**
```json
[
  {
    "id": 1,
    "title": "API 가이드",
    "content": "# API 가이드\n\n...",
    "category": {
      "id": 1,
      "name": "기술문서"
    },
    "createdBy": {
      "id": 1,
      "username": "admin",
      "name": "관리자"
    },
    "createdAt": "2024-12-20T10:00:00",
    "updatedAt": "2024-12-20T12:00:00"
  }
]
```

---

#### 문서 상세 조회

특정 문서의 상세 정보를 조회합니다.

**Endpoint**
```
GET /api/wiki/documents/{id}
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| Parameter | Type | Description |
|-----------|------|-------------|
| id | long | 문서 ID |

**Response (200 OK)**
```json
{
  "id": 1,
  "title": "API 가이드",
  "content": "# API 가이드\n\n마크다운 내용...",
  "category": {
    "id": 1,
    "name": "기술문서"
  },
  "files": [
    {
      "id": 1,
      "originalFilename": "diagram.png",
      "fileSize": 102400,
      "mimeType": "image/png"
    }
  ],
  "linkedSrs": [
    {
      "id": 1,
      "srId": "SR-2412-0001",
      "title": "API 오류 수정"
    }
  ],
  "createdBy": {
    "id": 1,
    "username": "admin",
    "name": "관리자"
  },
  "createdAt": "2024-12-20T10:00:00",
  "updatedAt": "2024-12-20T12:00:00"
}
```

---

#### 문서 생성

새 Wiki 문서를 생성합니다.

**Endpoint**
```
POST /api/wiki/documents
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Request Body**
```json
{
  "title": "새 문서 제목",
  "content": "# 마크다운 내용",
  "categoryId": 1,
  "srIds": [1, 2]
}
```

**Response (201 Created)**
```json
{
  "id": 2,
  "title": "새 문서 제목",
  "createdAt": "2024-12-20T15:00:00"
}
```

---

#### 문서 수정

기존 문서를 수정합니다. 수정 시 자동으로 버전이 생성됩니다.

**Endpoint**
```
PUT /api/wiki/documents/{id}
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Request Body**
```json
{
  "title": "수정된 제목",
  "content": "# 수정된 내용",
  "categoryId": 2,
  "srIds": [1]
}
```

**Response (200 OK)**
```json
{
  "id": 1,
  "title": "수정된 제목",
  "updatedAt": "2024-12-20T16:00:00"
}
```

---

#### 문서 삭제

문서를 삭제합니다.

**Endpoint**
```
DELETE /api/wiki/documents/{id}
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Response (204 No Content)**

---

### 문서 버전 관리

#### 버전 이력 조회

문서의 버전 이력을 조회합니다.

**Endpoint**
```
GET /api/wiki/documents/{id}/versions
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Response (200 OK)**
```json
[
  {
    "id": 3,
    "versionNumber": 3,
    "title": "현재 제목",
    "createdBy": {
      "id": 1,
      "username": "admin",
      "name": "관리자"
    },
    "createdAt": "2024-12-20T16:00:00"
  },
  {
    "id": 2,
    "versionNumber": 2,
    "title": "이전 제목",
    "createdBy": {
      "id": 1,
      "username": "admin",
      "name": "관리자"
    },
    "createdAt": "2024-12-20T14:00:00"
  }
]
```

---

#### 특정 버전 조회

특정 버전의 문서 내용을 조회합니다.

**Endpoint**
```
GET /api/wiki/documents/{id}/versions/{versionId}
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Response (200 OK)**
```json
{
  "id": 2,
  "versionNumber": 2,
  "title": "이전 제목",
  "content": "# 이전 내용",
  "createdBy": {
    "id": 1,
    "username": "admin",
    "name": "관리자"
  },
  "createdAt": "2024-12-20T14:00:00"
}
```

---

#### 버전 롤백

특정 버전으로 문서를 롤백합니다.

**Endpoint**
```
POST /api/wiki/documents/{id}/versions/{versionId}/rollback
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Response (200 OK)**
```json
{
  "id": 1,
  "title": "롤백된 제목",
  "message": "버전 2로 롤백되었습니다."
}
```

---

### 카테고리 관리

#### 카테고리 트리 조회

계층형 카테고리 트리를 조회합니다.

**Endpoint**
```
GET /api/wiki/categories/tree
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Response (200 OK)**
```json
[
  {
    "id": 1,
    "name": "기술문서",
    "parentId": null,
    "orderIndex": 0,
    "children": [
      {
        "id": 2,
        "name": "API 문서",
        "parentId": 1,
        "orderIndex": 0,
        "children": []
      },
      {
        "id": 3,
        "name": "설치 가이드",
        "parentId": 1,
        "orderIndex": 1,
        "children": []
      }
    ]
  }
]
```

---

#### 카테고리 생성

**Endpoint**
```
POST /api/wiki/categories
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Request Body**
```json
{
  "name": "새 카테고리",
  "parentId": 1,
  "orderIndex": 2
}
```

**Response (201 Created)**
```json
{
  "id": 4,
  "name": "새 카테고리",
  "parentId": 1,
  "orderIndex": 2
}
```

---

#### 카테고리 수정

**Endpoint**
```
PUT /api/wiki/categories/{id}
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Request Body**
```json
{
  "name": "수정된 카테고리명",
  "parentId": null,
  "orderIndex": 0
}
```

**Response (200 OK)**
```json
{
  "id": 4,
  "name": "수정된 카테고리명",
  "parentId": null,
  "orderIndex": 0
}
```

---

#### 카테고리 삭제

**Endpoint**
```
DELETE /api/wiki/categories/{id}
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Response (204 No Content)**

---

### 파일 관리

#### 파일 업로드

Wiki 문서에 첨부할 파일을 업로드합니다.

**Endpoint**
```
POST /api/wiki/files/upload
```

**Headers**
```
Authorization: Bearer {access_token}
Content-Type: multipart/form-data
```

**Request**
- `file`: 업로드할 파일 (multipart)
- `documentId`: 연결할 문서 ID (optional)

**Response (201 Created)**
```json
{
  "id": 1,
  "originalFilename": "diagram.png",
  "storedFilename": "a1b2c3d4-e5f6-7890-abcd-ef1234567890.png",
  "fileSize": 102400,
  "mimeType": "image/png",
  "uploadedAt": "2024-12-20T10:00:00"
}
```

---

#### 파일 다운로드

**Endpoint**
```
GET /api/wiki/files/{id}/download
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Response**
- Content-Type: 파일의 MIME 타입
- Content-Disposition: attachment; filename="원본파일명"

---

#### PDF 업로드 및 마크다운 변환

PDF 파일을 업로드하고 마크다운으로 변환합니다.

**Endpoint**
```
POST /api/wiki/files/upload-pdf
```

**Headers**
```
Authorization: Bearer {access_token}
Content-Type: multipart/form-data
```

**Request**
- `file`: PDF 파일 (multipart)

**Response (200 OK)**
```json
{
  "success": true,
  "markdown": "# 추출된 제목\n\n추출된 본문 내용...",
  "pageCount": 5,
  "extractedAt": "2024-12-20T10:00:00"
}
```

---

#### PDF 업로드 (AI 구조 보정)

PDF 파일을 업로드하고 AI를 사용하여 표/수식 구조를 보정합니다.

**Endpoint**
```
POST /api/wiki/files/upload-pdf-enhanced
```

**Headers**
```
Authorization: Bearer {access_token}
Content-Type: multipart/form-data
```

**Request**
- `file`: PDF 파일 (multipart)

**Response (200 OK)**
```json
{
  "success": true,
  "markdown": "# 제목\n\n| 컬럼1 | 컬럼2 |\n|-------|-------|\n| 값1 | 값2 |\n\n수식: $E = mc^2$",
  "pageCount": 5,
  "enhanced": true,
  "tablesDetected": 3,
  "formulasDetected": 2,
  "extractedAt": "2024-12-20T10:00:00"
}
```

---

### AI 검색

#### 시맨틱 검색

자연어 질의로 Wiki 문서를 검색합니다 (임베딩 기반 유사도 검색).

**Endpoint**
```
POST /api/wiki/search/semantic
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Request Body**
```json
{
  "query": "API 인증 방법은?",
  "topK": 5
}
```

**Response (200 OK)**
```json
{
  "results": [
    {
      "documentId": 1,
      "title": "API 가이드",
      "content": "# API 가이드\n\nJWT 토큰을 사용하여...",
      "similarity": 0.92,
      "category": "기술문서"
    },
    {
      "documentId": 3,
      "title": "인증 시스템 설명",
      "content": "# 인증 시스템\n\nAccess Token과 Refresh Token...",
      "similarity": 0.85,
      "category": "기술문서"
    }
  ],
  "queryEmbeddingTime": 45,
  "searchTime": 12
}
```

---

#### 통합 AI 검색

Wiki, SR, 설문조사를 통합하여 검색합니다.

**Endpoint**
```
POST /api/wiki/search/unified
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Request Body**
```json
{
  "query": "로그인 오류 해결 방법",
  "topK": 10,
  "sources": ["wiki", "sr", "survey"]
}
```

**Response (200 OK)**
```json
{
  "results": [
    {
      "source": "wiki",
      "sourceId": 1,
      "title": "로그인 문제 해결 가이드",
      "content": "...",
      "similarity": 0.89
    },
    {
      "source": "sr",
      "sourceId": 5,
      "title": "SR-2412-0005: 로그인 오류",
      "content": "...",
      "similarity": 0.82
    }
  ],
  "totalResults": 8
}
```

---

#### 임베딩 재생성

문서의 임베딩을 수동으로 재생성합니다.

**Endpoint**
```
POST /api/wiki/search/reindex/{documentId}
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Response (200 OK)**
```json
{
  "success": true,
  "documentId": 1,
  "chunksCreated": 5,
  "message": "임베딩이 성공적으로 재생성되었습니다."
}
```

---

#### 전체 임베딩 재생성

모든 문서의 임베딩을 재생성합니다 (관리자 전용).

**Endpoint**
```
POST /api/wiki/search/reindex-all
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Required Role**: ADMIN

**Response (200 OK)**
```json
{
  "success": true,
  "documentsProcessed": 45,
  "totalChunksCreated": 230,
  "processingTime": 12500
}
```

---

#### 검색 기록 조회

사용자의 AI 검색 기록을 조회합니다.

**Endpoint**
```
GET /api/wiki/search/history
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Query Parameters**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| page | integer | No | 0 | 페이지 번호 |
| size | integer | No | 20 | 페이지 크기 |

**Response (200 OK)**
```json
{
  "content": [
    {
      "id": 1,
      "query": "API 인증 방법",
      "resultCount": 5,
      "searchedAt": "2024-12-20T15:30:00"
    }
  ],
  "totalElements": 50,
  "totalPages": 3
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
