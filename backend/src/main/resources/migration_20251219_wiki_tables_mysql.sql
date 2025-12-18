-- =====================================================
-- Wiki 테이블 마이그레이션 스크립트 (MySQL 8.x)
-- 작성일: 2025-12-19
-- 설명: Wiki 기능 Phase 1 - 기본 Wiki 시스템 테이블 생성
-- =====================================================

-- 1. Wiki 카테고리 테이블
CREATE TABLE IF NOT EXISTS wiki_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_wiki_category_parent (parent_id),
    INDEX idx_wiki_category_sort (sort_order),
    CONSTRAINT fk_wiki_category_parent FOREIGN KEY (parent_id) REFERENCES wiki_category(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Wiki 문서 테이블
CREATE TABLE IF NOT EXISTS wiki_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    category_id BIGINT,
    created_by BIGINT NOT NULL,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    view_count INT DEFAULT 0,
    INDEX idx_wiki_document_category (category_id),
    INDEX idx_wiki_document_created_by (created_by),
    INDEX idx_wiki_document_updated_at (updated_at DESC),
    INDEX idx_wiki_document_view_count (view_count DESC),
    CONSTRAINT fk_wiki_document_category FOREIGN KEY (category_id) REFERENCES wiki_category(id) ON DELETE SET NULL,
    CONSTRAINT fk_wiki_document_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_wiki_document_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Full-text 인덱스 추가 (MySQL 5.6+)
ALTER TABLE wiki_document ADD FULLTEXT INDEX idx_wiki_document_fulltext (title, content);

-- 3. Wiki 버전 테이블
CREATE TABLE IF NOT EXISTS wiki_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    version INT NOT NULL,
    content TEXT,
    change_summary VARCHAR(200),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_wiki_version_document (document_id),
    INDEX idx_wiki_version_created_at (created_at DESC),
    UNIQUE KEY uk_wiki_version_document_version (document_id, version),
    CONSTRAINT fk_wiki_version_document FOREIGN KEY (document_id) REFERENCES wiki_document(id) ON DELETE CASCADE,
    CONSTRAINT fk_wiki_version_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Wiki 파일 테이블
CREATE TABLE IF NOT EXISTS wiki_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT,
    original_file_name VARCHAR(200) NOT NULL,
    stored_file_name VARCHAR(200) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(50),
    type ENUM('IMAGE', 'DOCUMENT', 'ATTACHMENT') NOT NULL,
    uploaded_by BIGINT NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_wiki_file_document (document_id),
    INDEX idx_wiki_file_type (type),
    INDEX idx_wiki_file_uploaded_at (uploaded_at DESC),
    CONSTRAINT fk_wiki_file_document FOREIGN KEY (document_id) REFERENCES wiki_document(id) ON DELETE CASCADE,
    CONSTRAINT fk_wiki_file_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. SR-Wiki 연계 테이블 (Many-to-Many)
CREATE TABLE IF NOT EXISTS sr_wiki_document (
    sr_id BIGINT NOT NULL,
    wiki_document_id BIGINT NOT NULL,
    PRIMARY KEY (sr_id, wiki_document_id),
    INDEX idx_sr_wiki_sr (sr_id),
    INDEX idx_sr_wiki_document (wiki_document_id),
    CONSTRAINT fk_sr_wiki_sr FOREIGN KEY (sr_id) REFERENCES sr(id) ON DELETE CASCADE,
    CONSTRAINT fk_sr_wiki_document FOREIGN KEY (wiki_document_id) REFERENCES wiki_document(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 마이그레이션 검증 쿼리
-- =====================================================
-- SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
-- WHERE TABLE_SCHEMA = DATABASE()
-- AND TABLE_NAME IN ('wiki_category', 'wiki_document', 'wiki_version', 'wiki_file', 'sr_wiki_document');
