-- =====================================================
-- Wiki 테이블 마이그레이션 스크립트 (CUBRID 10.x+)
-- 작성일: 2025-12-19
-- 설명: Wiki 기능 Phase 1 - 기본 Wiki 시스템 테이블 생성
-- 참고: CUBRID는 AUTO_INCREMENT 대신 시퀀스 사용
-- =====================================================

-- 시퀀스 생성
CREATE SERIAL wiki_category_id_seq START WITH 1;
CREATE SERIAL wiki_document_id_seq START WITH 1;
CREATE SERIAL wiki_version_id_seq START WITH 1;
CREATE SERIAL wiki_file_id_seq START WITH 1;

-- 1. Wiki 카테고리 테이블
CREATE TABLE IF NOT EXISTS wiki_category (
    id BIGINT DEFAULT wiki_category_id_seq.NEXT_VALUE PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wiki_category_parent FOREIGN KEY (parent_id) REFERENCES wiki_category(id) ON DELETE CASCADE
);

CREATE INDEX idx_wiki_category_parent ON wiki_category(parent_id);
CREATE INDEX idx_wiki_category_sort ON wiki_category(sort_order);

-- 2. Wiki 문서 테이블
CREATE TABLE IF NOT EXISTS wiki_document (
    id BIGINT DEFAULT wiki_document_id_seq.NEXT_VALUE PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content CLOB,
    category_id BIGINT,
    created_by BIGINT NOT NULL,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    view_count INT DEFAULT 0,
    CONSTRAINT fk_wiki_document_category FOREIGN KEY (category_id) REFERENCES wiki_category(id) ON DELETE SET NULL,
    CONSTRAINT fk_wiki_document_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_wiki_document_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_wiki_document_category ON wiki_document(category_id);
CREATE INDEX idx_wiki_document_created_by ON wiki_document(created_by);
CREATE INDEX idx_wiki_document_updated_at ON wiki_document(updated_at DESC);
CREATE INDEX idx_wiki_document_view_count ON wiki_document(view_count DESC);

-- 3. Wiki 버전 테이블
CREATE TABLE IF NOT EXISTS wiki_version (
    id BIGINT DEFAULT wiki_version_id_seq.NEXT_VALUE PRIMARY KEY,
    document_id BIGINT NOT NULL,
    version INT NOT NULL,
    content CLOB,
    change_summary VARCHAR(200),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wiki_version_document FOREIGN KEY (document_id) REFERENCES wiki_document(id) ON DELETE CASCADE,
    CONSTRAINT fk_wiki_version_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_wiki_version_document_version UNIQUE (document_id, version)
);

CREATE INDEX idx_wiki_version_document ON wiki_version(document_id);
CREATE INDEX idx_wiki_version_created_at ON wiki_version(created_at DESC);

-- 4. Wiki 파일 테이블
-- CUBRID는 ENUM을 지원하지 않으므로 VARCHAR + CHECK 제약조건 사용
CREATE TABLE IF NOT EXISTS wiki_file (
    id BIGINT DEFAULT wiki_file_id_seq.NEXT_VALUE PRIMARY KEY,
    document_id BIGINT,
    original_file_name VARCHAR(200) NOT NULL,
    stored_file_name VARCHAR(200) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(50),
    type VARCHAR(20) NOT NULL,
    uploaded_by BIGINT NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_wiki_file_type CHECK (type IN ('IMAGE', 'DOCUMENT', 'ATTACHMENT')),
    CONSTRAINT fk_wiki_file_document FOREIGN KEY (document_id) REFERENCES wiki_document(id) ON DELETE CASCADE,
    CONSTRAINT fk_wiki_file_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_wiki_file_document ON wiki_file(document_id);
CREATE INDEX idx_wiki_file_type ON wiki_file(type);
CREATE INDEX idx_wiki_file_uploaded_at ON wiki_file(uploaded_at DESC);

-- 5. SR-Wiki 연계 테이블 (Many-to-Many)
CREATE TABLE IF NOT EXISTS sr_wiki_document (
    sr_id BIGINT NOT NULL,
    wiki_document_id BIGINT NOT NULL,
    PRIMARY KEY (sr_id, wiki_document_id),
    CONSTRAINT fk_sr_wiki_sr FOREIGN KEY (sr_id) REFERENCES sr(id) ON DELETE CASCADE,
    CONSTRAINT fk_sr_wiki_document FOREIGN KEY (wiki_document_id) REFERENCES wiki_document(id) ON DELETE CASCADE
);

CREATE INDEX idx_sr_wiki_sr ON sr_wiki_document(sr_id);
CREATE INDEX idx_sr_wiki_document ON sr_wiki_document(wiki_document_id);

-- =====================================================
-- updated_at 자동 갱신 트리거 (CUBRID)
-- =====================================================
CREATE TRIGGER trigger_update_wiki_document_updated_at
    BEFORE UPDATE ON wiki_document
    EXECUTE
        UPDATE wiki_document SET updated_at = CURRENT_TIMESTAMP
        WHERE id = obj.id;

-- =====================================================
-- 마이그레이션 검증 쿼리
-- =====================================================
-- SELECT class_name FROM db_class
-- WHERE class_name IN ('wiki_category', 'wiki_document', 'wiki_version', 'wiki_file', 'sr_wiki_document');
