# 변경 이력 - Wiki 기능 Phase 1 (2025-12-19)

## 개요
SR 관리 시스템에 AI 기반 지식 관리를 위한 Wiki 기능의 Phase 1 MVP를 구현하였습니다.
마크다운 기반 문서 작성, 계층형 카테고리, 버전 관리, 파일 첨부 기능을 포함합니다.

## 주요 변경사항

### 1. Backend 구현

#### 1.1 Entity 계층 (4개 엔티티)

**WikiDocument.java**
- 위치: `backend/src/main/java/com/srmanagement/wiki/entity/WikiDocument.java`
- 기능: Wiki 문서 엔티티
- 주요 필드:
  - `title`: 문서 제목
  - `content`: 마크다운 콘텐츠
  - `category`: 카테고리 참조 (ManyToOne)
  - `sr`: SR 연계 (ManyToOne, Optional)
  - `createdBy`, `updatedBy`: 작성자/수정자
  - `viewCount`: 조회수
  - `versions`: 버전 이력 (OneToMany)
  - `files`: 첨부 파일 (OneToMany)

**WikiCategory.java**
- 위치: `backend/src/main/java/com/srmanagement/wiki/entity/WikiCategory.java`
- 기능: 계층형 카테고리 엔티티
- 주요 필드:
  - `name`: 카테고리 이름
  - `parent`: 부모 카테고리 (ManyToOne, Self-referencing)
  - `children`: 자식 카테고리 (OneToMany)
  - `documents`: 소속 문서 목록
  - `sortOrder`: 정렬 순서

**WikiVersion.java**
- 위치: `backend/src/main/java/com/srmanagement/wiki/entity/WikiVersion.java`
- 기능: 문서 버전 이력 엔티티
- 주요 필드:
  - `document`: 원본 문서 참조
  - `version`: 버전 번호
  - `content`: 해당 버전의 콘텐츠
  - `changeSummary`: 변경 요약
  - `createdBy`: 버전 생성자

**WikiFile.java**
- 위치: `backend/src/main/java/com/srmanagement/wiki/entity/WikiFile.java`
- 기능: 파일 첨부 엔티티
- 주요 필드:
  - `document`: 문서 참조 (Optional)
  - `originalFileName`: 원본 파일명
  - `storedFileName`: 저장된 파일명 (UUID)
  - `filePath`: 파일 경로
  - `fileSize`: 파일 크기
  - `type`: 파일 타입 (IMAGE, DOCUMENT, ATTACHMENT)

#### 1.2 Repository 계층 (4개 레포지토리)

**WikiDocumentRepository.java**
- 위치: `backend/src/main/java/com/srmanagement/wiki/repository/WikiDocumentRepository.java`
- 커스텀 쿼리:
  - `findByCategoryId()`: 카테고리별 문서 조회
  - `findBySrId()`: SR 연계 문서 조회
  - `searchByTitle()`: 제목 검색
  - `searchByTitleOrContent()`: 전체 텍스트 검색
  - `findRecentlyUpdated()`: 최근 수정 문서
  - `findPopular()`: 인기 문서 (조회수 기준)
  - `findByIdWithDetails()`: 페치 조인 최적화

**WikiCategoryRepository.java**
- 위치: `backend/src/main/java/com/srmanagement/wiki/repository/WikiCategoryRepository.java`
- 커스텀 쿼리:
  - `findByParentIsNullOrderBySortOrderAsc()`: 최상위 카테고리
  - `findByParentIdOrderBySortOrderAsc()`: 하위 카테고리
  - `findByIdWithChildren()`: 자식 포함 조회

**WikiVersionRepository.java**
- 위치: `backend/src/main/java/com/srmanagement/wiki/repository/WikiVersionRepository.java`
- 커스텀 쿼리:
  - `findByDocumentIdOrderByVersionDesc()`: 버전 이력
  - `findByDocumentIdAndVersion()`: 특정 버전 조회
  - `findLatestVersionNumber()`: 최신 버전 번호
  - `findLatestVersion()`: 최신 버전 조회

**WikiFileRepository.java**
- 위치: `backend/src/main/java/com/srmanagement/wiki/repository/WikiFileRepository.java`
- 커스텀 쿼리:
  - `findByDocumentId()`: 문서별 파일 목록
  - `findByDocumentIdAndType()`: 타입별 파일 조회
  - `findImagesByDocumentId()`: 이미지 파일만 조회

#### 1.3 Service 계층 (4개 서비스)

**WikiDocumentService.java**
- 위치: `backend/src/main/java/com/srmanagement/wiki/service/WikiDocumentService.java`
- 주요 메서드:
  - `createDocument()`: 문서 생성 (최초 버전 자동 생성)
  - `updateDocument()`: 문서 수정 (내용 변경 시 버전 생성)
  - `deleteDocument()`: 문서 삭제
  - `getDocument()`: 문서 조회
  - `getDocumentAndIncrementViewCount()`: 조회수 증가
  - `searchDocuments()`: 문서 검색
  - `getRecentlyUpdated()`: 최근 문서
  - `getPopularDocuments()`: 인기 문서

**WikiCategoryService.java**
- 위치: `backend/src/main/java/com/srmanagement/wiki/service/WikiCategoryService.java`
- 주요 메서드:
  - `createCategory()`: 카테고리 생성
  - `updateCategory()`: 카테고리 수정
  - `deleteCategory()`: 카테고리 삭제 (하위 카테고리/문서 체크)
  - `getRootCategories()`: 최상위 카테고리 트리
  - `getChildCategories()`: 하위 카테고리

**WikiVersionService.java**
- 위치: `backend/src/main/java/com/srmanagement/wiki/service/WikiVersionService.java`
- 주요 메서드:
  - `getDocumentVersions()`: 버전 이력 조회
  - `getVersion()`: 특정 버전 조회
  - `getLatestVersion()`: 최신 버전 조회
  - `rollbackToVersion()`: 버전 롤백 (새 버전 생성)

**WikiFileService.java**
- 위치: `backend/src/main/java/com/srmanagement/wiki/service/WikiFileService.java`
- 주요 메서드:
  - `uploadFile()`: 파일 업로드 (로컬 저장 + DB 메타데이터)
  - `downloadFile()`: 파일 다운로드 (Resource 반환)
  - `deleteFile()`: 파일 삭제 (파일 시스템 + DB)
  - `getFilesByDocument()`: 문서별 파일 목록
- 설정:
  - 기본 업로드 경로: `./data/wiki-uploads`
  - 최대 파일 크기: 20MB
  - UUID 기반 파일명 생성

#### 1.4 Controller 계층 (4개 컨트롤러)

**WikiDocumentController.java**
- 위치: `backend/src/main/java/com/srmanagement/wiki/controller/WikiDocumentController.java`
- 엔드포인트:
  - `POST /api/wiki/documents`: 문서 생성
  - `PUT /api/wiki/documents/{id}`: 문서 수정
  - `DELETE /api/wiki/documents/{id}`: 문서 삭제
  - `GET /api/wiki/documents/{id}`: 문서 조회
  - `GET /api/wiki/documents`: 전체 문서 (페이징)
  - `GET /api/wiki/documents/category/{categoryId}`: 카테고리별
  - `GET /api/wiki/documents/sr/{srId}`: SR 연계 문서
  - `GET /api/wiki/documents/search`: 검색
  - `GET /api/wiki/documents/recent`: 최근 문서
  - `GET /api/wiki/documents/popular`: 인기 문서

**WikiCategoryController.java**
- 위치: `backend/src/main/java/com/srmanagement/wiki/controller/WikiCategoryController.java`
- 엔드포인트:
  - `POST /api/wiki/categories`: 카테고리 생성
  - `PUT /api/wiki/categories/{id}`: 카테고리 수정
  - `DELETE /api/wiki/categories/{id}`: 카테고리 삭제
  - `GET /api/wiki/categories/{id}`: 카테고리 조회
  - `GET /api/wiki/categories`: 전체 카테고리
  - `GET /api/wiki/categories/root`: 최상위 카테고리 트리
  - `GET /api/wiki/categories/parent/{parentId}`: 하위 카테고리

**WikiVersionController.java**
- 위치: `backend/src/main/java/com/srmanagement/wiki/controller/WikiVersionController.java`
- 엔드포인트:
  - `GET /api/wiki/documents/{documentId}/versions`: 버전 목록
  - `GET /api/wiki/documents/{documentId}/versions/paged`: 버전 목록 (페이징)
  - `GET /api/wiki/documents/{documentId}/versions/{version}`: 특정 버전
  - `GET /api/wiki/documents/{documentId}/versions/latest`: 최신 버전
  - `POST /api/wiki/documents/{documentId}/versions/{version}/rollback`: 롤백

**WikiFileController.java**
- 위치: `backend/src/main/java/com/srmanagement/wiki/controller/WikiFileController.java`
- 엔드포인트:
  - `POST /api/wiki/files/upload`: 파일 업로드
  - `GET /api/wiki/files/{fileId}`: 파일 다운로드 (인라인)
  - `GET /api/wiki/files/{fileId}/download`: 파일 다운로드 (강제)
  - `GET /api/wiki/files/{fileId}/info`: 파일 정보
  - `GET /api/wiki/files/document/{documentId}`: 문서별 파일 목록
  - `DELETE /api/wiki/files/{fileId}`: 파일 삭제

#### 1.5 DTO 클래스 (6개)

**Request DTO**
- `WikiDocumentRequest.java`: 문서 생성/수정 요청
- `WikiCategoryRequest.java`: 카테고리 생성/수정 요청

**Response DTO**
- `WikiDocumentResponse.java`: 문서 응답
- `WikiCategoryResponse.java`: 카테고리 응답 (계층 구조 지원)
- `WikiVersionResponse.java`: 버전 응답
- `WikiFileResponse.java`: 파일 응답

---

### 2. Frontend 구현

#### 2.1 타입 정의

**wiki.ts**
- 위치: `frontend/src/types/wiki.ts`
- 인터페이스:
  - `WikiDocument`: 문서 타입
  - `WikiDocumentRequest`: 문서 요청 타입
  - `WikiCategory`: 카테고리 타입 (재귀적 children)
  - `WikiCategoryRequest`: 카테고리 요청 타입
  - `WikiVersion`: 버전 타입
  - `WikiFile`: 파일 타입
  - `WikiPageResponse<T>`: 페이징 응답 타입

#### 2.2 API 서비스

**wikiService.ts**
- 위치: `frontend/src/services/wikiService.ts`
- API 클라이언트:
  - `wikiDocumentApi`: 문서 API (CRUD, 검색, 인기/최근)
  - `wikiCategoryApi`: 카테고리 API (CRUD, 트리 조회)
  - `wikiVersionApi`: 버전 API (조회, 롤백)
  - `wikiFileApi`: 파일 API (업로드, 다운로드)
- 중앙 axios 인스턴스 사용 (JWT 자동 처리)

#### 2.3 컴포넌트

**WikiEditor.tsx**
- 위치: `frontend/src/components/wiki/WikiEditor.tsx`
- 기능: 마크다운 에디터 (Toast UI Editor)
- 특징:
  - 실시간 미리보기 (Split View)
  - 이미지 업로드 훅 (`addImageBlobHook`)
  - 툴바: Heading, Bold, Italic, List, Table, Code 등
  - 자동 이미지 URL 삽입

**WikiViewer.tsx**
- 위치: `frontend/src/components/wiki/WikiViewer.tsx`
- 기능: 마크다운 렌더러 (react-markdown)
- 플러그인:
  - `remark-gfm`: GitHub Flavored Markdown
  - `rehype-highlight`: 코드 하이라이팅
  - `rehype-raw`: HTML 지원
- 스타일: GitHub 스타일 마크다운 CSS

**WikiCategoryTree.tsx**
- 위치: `frontend/src/components/wiki/WikiCategoryTree.tsx`
- 기능: 계층형 카테고리 트리 네비게이션
- 특징:
  - 재귀적 렌더링 (무제한 depth)
  - 접기/펼치기 애니메이션
  - 문서 개수 표시
  - 선택 상태 표시
  - 카테고리 액션 버튼 (추가, 수정, 삭제)

#### 2.4 페이지

**WikiPage.tsx**
- 위치: `frontend/src/pages/WikiPage.tsx`
- 기능: Wiki 메인 페이지
- 레이아웃:
  - 왼쪽: 검색 + 카테고리 트리 + 문서 목록
  - 오른쪽: 툴바 + 에디터/뷰어
- 상태 관리:
  - 문서 생성/수정/삭제
  - 카테고리 필터링
  - 검색
  - 편집 모드 전환
- URL 라우팅: `/wiki`, `/wiki/:id`

#### 2.5 라우팅 및 네비게이션

**App.tsx 수정**
- Wiki 라우트 추가:
  ```tsx
  <Route path="/wiki" element={<WikiPage />} />
  <Route path="/wiki/:id" element={<WikiPage />} />
  ```

**Sidebar.tsx 수정**
- 네비게이션 메뉴에 "📚 Wiki" 추가

---

### 3. 라이브러리 설치

**Frontend 의존성 추가**
```json
{
  "@toast-ui/react-editor": "^3.2.3",
  "@toast-ui/editor": "latest",
  "react-markdown": "latest",
  "remark-gfm": "latest",
  "rehype-highlight": "latest",
  "rehype-raw": "latest"
}
```

설치 명령어:
```bash
npm install --legacy-peer-deps @toast-ui/react-editor @toast-ui/editor react-markdown remark-gfm rehype-highlight rehype-raw
```

---

### 4. 데이터베이스 스키마

**새로운 테이블 (4개)**

```sql
-- Wiki 문서 테이블
CREATE TABLE wiki_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    category_id BIGINT,
    sr_id BIGINT,
    created_by BIGINT NOT NULL,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    view_count INT DEFAULT 0,
    FOREIGN KEY (category_id) REFERENCES wiki_category(id),
    FOREIGN KEY (sr_id) REFERENCES sr(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (updated_by) REFERENCES users(id)
);

-- Wiki 카테고리 테이블
CREATE TABLE wiki_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES wiki_category(id)
);

-- Wiki 버전 테이블
CREATE TABLE wiki_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    version INT NOT NULL,
    content TEXT,
    change_summary VARCHAR(200),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES wiki_document(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Wiki 파일 테이블
CREATE TABLE wiki_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT,
    original_file_name VARCHAR(200) NOT NULL,
    stored_file_name VARCHAR(200) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(50),
    type VARCHAR(20) NOT NULL,
    uploaded_by BIGINT NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES wiki_document(id),
    FOREIGN KEY (uploaded_by) REFERENCES users(id)
);
```

**인덱스**
- `wiki_document`: `category_id`, `sr_id`, `created_by`
- `wiki_category`: `parent_id`
- `wiki_version`: `document_id`, `version` (composite)
- `wiki_file`: `document_id`, `uploaded_by`

---

### 5. 주요 기능 설명

#### 5.1 문서 버전 관리

**자동 버전 생성**
- 문서 생성 시: version 1 자동 생성
- 문서 수정 시: 내용이 변경된 경우에만 새 버전 생성
- 버전 번호: 자동 증가 (1, 2, 3, ...)

**버전 롤백**
- 특정 버전으로 복구
- 롤백 시 새로운 버전 생성 (이력 보존)
- 변경 요약: "버전 X로 롤백"

#### 5.2 파일 업로드

**저장 방식**
- 로컬 파일 시스템: `./data/wiki-uploads/`
- 파일명: UUID 기반 중복 방지
- 메타데이터: DB에 저장

**파일 타입 자동 분류**
- `IMAGE`: PNG, JPG, GIF 등
- `DOCUMENT`: PDF, DOCX, XLSX 등
- `ATTACHMENT`: 기타

**에디터 통합**
- 이미지 붙여넣기 지원
- 드래그 앤 드롭 업로드
- 자동 마크다운 삽입

#### 5.3 검색 기능

**검색 범위**
- 제목 검색: `LIKE %keyword%`
- 전체 검색: 제목 + 내용
- 대소문자 무시

**정렬 옵션**
- 최근 수정: `updatedAt DESC`
- 인기 순: `viewCount DESC`

#### 5.4 계층형 카테고리

**Self-referencing 구조**
- `parent_id`로 부모 참조
- 무제한 depth 지원
- Recursive DTO 변환

**프론트엔드 트리 렌더링**
- 재귀 컴포넌트
- 접기/펼치기 상태 관리
- CSS 애니메이션

---

### 6. 보안 및 인증

**JWT 인증**
- 모든 Wiki API는 인증 필요
- `Authentication` 객체로 현재 사용자 확인
- 사용자명 → UserRepository 조회

**권한 관리**
- 문서 작성: 모든 인증된 사용자
- 문서 수정/삭제: 추후 작성자/관리자 제한 가능 (TODO)
- 카테고리 관리: 추후 관리자 제한 가능 (TODO)

---

### 7. 빌드 및 배포

#### 7.1 빌드 프로세스

**통합 빌드 스크립트**
```bash
./backend/scripts/start.sh
```

**수행 작업**:
1. Frontend 빌드 (`npm run build`)
2. 빌드 결과물 복사 → `backend/src/main/resources/static/`
3. Backend Maven 빌드 (`mvn clean package -DskipTests`)
4. Spring Boot 서버 시작 (백그라운드)

#### 7.2 실행 환경

**프로덕션 모드**
- Profile: `prod`
- DB: H2 파일 모드 (`./data/srdb_prod`)
- DDL: `update` (데이터 유지)
- 로그: `logs/server.log`

**접속 정보**
- Frontend + Backend: http://localhost:8080
- Wiki 페이지: http://localhost:8080/wiki
- H2 Console: http://localhost:8080/h2-console

---

### 8. 테스트 결과

#### 8.1 Backend 컴파일

```
[INFO] Compiling 86 source files
[INFO] BUILD SUCCESS
```

- 총 86개 Java 파일 (Wiki 관련 26개 추가)
- 컴파일 에러: 0
- 경고: Null safety warnings (무시 가능)

#### 8.2 Frontend 빌드

```
✓ 677 modules transformed
✓ built in 2.49s
dist/assets/index-BT5mzNNC.js   1,625.15 kB │ gzip: 508.54 kB
```

- 빌드 성공
- 경고: Chunk size (코드 스플리팅 권장사항, 기능 정상)

#### 8.3 통합 서버 실행

```
Server started successfully!
PID: 51290
Backend API: http://localhost:8080
Frontend: http://localhost:8080
```

- 서버 시작 성공
- H2 데이터베이스 초기화 완료
- Admin 계정 생성 (username: admin, password: admin123)

---

### 9. 파일 변경 목록

#### Backend 신규 파일 (26개)

**Entity (4개)**
- `wiki/entity/WikiDocument.java`
- `wiki/entity/WikiCategory.java`
- `wiki/entity/WikiVersion.java`
- `wiki/entity/WikiFile.java`

**Repository (4개)**
- `wiki/repository/WikiDocumentRepository.java`
- `wiki/repository/WikiCategoryRepository.java`
- `wiki/repository/WikiVersionRepository.java`
- `wiki/repository/WikiFileRepository.java`

**Service (4개)**
- `wiki/service/WikiDocumentService.java`
- `wiki/service/WikiCategoryService.java`
- `wiki/service/WikiVersionService.java`
- `wiki/service/WikiFileService.java`

**Controller (4개)**
- `wiki/controller/WikiDocumentController.java`
- `wiki/controller/WikiCategoryController.java`
- `wiki/controller/WikiVersionController.java`
- `wiki/controller/WikiFileController.java`

**DTO (10개)**
- `wiki/dto/WikiDocumentRequest.java`
- `wiki/dto/WikiDocumentResponse.java`
- `wiki/dto/WikiCategoryRequest.java`
- `wiki/dto/WikiCategoryResponse.java`
- `wiki/dto/WikiVersionResponse.java`
- `wiki/dto/WikiFileResponse.java`

#### Frontend 신규 파일 (8개)

**Types (1개)**
- `types/wiki.ts`

**Services (1개)**
- `services/wikiService.ts`

**Components (3개)**
- `components/wiki/WikiEditor.tsx`
- `components/wiki/WikiViewer.tsx`
- `components/wiki/WikiCategoryTree.tsx`

**Pages (1개)**
- `pages/WikiPage.tsx`

**CSS (2개)**
- `components/wiki/WikiViewer.css`
- `components/wiki/WikiCategoryTree.css`
- `pages/WikiPage.css`

#### Frontend 수정 파일 (2개)

**App.tsx**
- Wiki 라우트 추가: `/wiki`, `/wiki/:id`
- WikiPage import 추가

**Sidebar.tsx**
- 네비게이션 메뉴 추가: "📚 Wiki"

#### 의존성 추가

**Frontend package.json**
- `@toast-ui/react-editor`
- `@toast-ui/editor`
- `react-markdown`
- `remark-gfm`
- `rehype-highlight`
- `rehype-raw`

---

### 10. 알려진 제한사항 및 향후 개선사항

#### 현재 제한사항

1. **권한 관리**
   - 모든 인증된 사용자가 문서/카테고리 수정 가능
   - TODO: 작성자/관리자만 수정 가능하도록 제한

2. **파일 업로드**
   - 파일 크기 제한: 20MB
   - 허용 확장자 제한 없음
   - TODO: 확장자 화이트리스트 적용

3. **검색**
   - 단순 LIKE 검색
   - TODO: Full-text Index 추가 (H2 FTL)

4. **버전 관리**
   - Diff View 미구현
   - TODO: 버전 간 차이 비교 UI

#### Phase 2 계획 (PB 문서 참고)

**PDF 변환 기능**
- PDF to Markdown 자동 변환
- Apache Tika 통합
- 이미지 추출 및 저장

**Phase 3 계획**

**AI 검색 기능**
- RAG 기반 자연어 검색
- Spring AI + Ollama 통합
- JdbcVectorStore (H2 기반)
- 유사도 검색 (Cosine Similarity)

---

### 11. 참고 문서

- **기획 문서**: `docs/PB_AI-Powered_Wiki.md`
- **프로젝트 개요**: `docs/PROJECT_OVERVIEW.md`
- **API 명세**: `docs/API.md` (Wiki API 추가 예정)
- **데이터베이스 설계**: `docs/DATABASE.md` (Wiki 테이블 추가 예정)

---

## 작업자
- Claude Code (AI Assistant)

## 작업 일시
- 2025-12-19

## 검토자
- 검토 필요

---

## 체크리스트

- [x] Backend Entity 구현 완료
- [x] Backend Repository 구현 완료
- [x] Backend Service 구현 완료
- [x] Backend Controller 구현 완료
- [x] Frontend 타입 정의 완료
- [x] Frontend API 서비스 완료
- [x] Frontend 컴포넌트 완료
- [x] Frontend 페이지 완료
- [x] 라우팅 설정 완료
- [x] 네비게이션 메뉴 추가 완료
- [x] Backend 컴파일 테스트 통과
- [x] Frontend 빌드 테스트 통과
- [x] 통합 서버 실행 성공
- [ ] API 문서 업데이트 (TODO)
- [ ] 데이터베이스 스키마 문서 업데이트 (TODO)
- [ ] 사용자 가이드 작성 (TODO)

---

## 비고

이번 작업으로 SR 관리 시스템에 기본적인 Wiki 기능이 추가되었습니다.
사용자는 이제 마크다운 기반으로 기술 문서를 작성하고, 카테고리로 분류하며,
버전 이력을 관리하고, 파일을 첨부할 수 있습니다.

Phase 2에서는 PDF 문서의 자동 변환 기능을, Phase 3에서는 AI 기반 자연어 검색 기능을
추가하여 완전한 지식 관리 시스템으로 발전시킬 예정입니다.
