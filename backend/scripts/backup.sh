#!/bin/bash

# ============================================================
# SR 관리 시스템 - 데이터 백업 스크립트
# ============================================================
# 사용법: ./backup.sh [옵션]
#   옵션:
#     --db-only      데이터베이스만 백업
#     --files-only   파일만 백업
#     --full         전체 백업 (기본값)
#     --output DIR   백업 출력 디렉토리 지정
# ============================================================

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 기본 설정
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BACKUP_BASE_DIR="${PROJECT_ROOT}/backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_DIR="${BACKUP_BASE_DIR}/${TIMESTAMP}"

# 파일 저장 경로 (application.yml에서 가져와야 하지만 기본값 사용)
WIKI_FILES_DIR="${PROJECT_ROOT}/data/wiki-files"
WIKI_IMAGES_DIR="${PROJECT_ROOT}/data/wiki-images"

# H2 데이터베이스 경로
H2_DATA_DIR="${PROJECT_ROOT}/data"

# 옵션 파싱
BACKUP_DB=true
BACKUP_FILES=true

while [[ $# -gt 0 ]]; do
    case $1 in
        --db-only)
            BACKUP_FILES=false
            shift
            ;;
        --files-only)
            BACKUP_DB=false
            shift
            ;;
        --full)
            BACKUP_DB=true
            BACKUP_FILES=true
            shift
            ;;
        --output)
            BACKUP_BASE_DIR="$2"
            BACKUP_DIR="${BACKUP_BASE_DIR}/${TIMESTAMP}"
            shift 2
            ;;
        *)
            echo -e "${RED}알 수 없는 옵션: $1${NC}"
            exit 1
            ;;
    esac
done

# 로그 함수
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 백업 디렉토리 생성
create_backup_dir() {
    log_info "백업 디렉토리 생성: ${BACKUP_DIR}"
    mkdir -p "${BACKUP_DIR}"
}

# H2 데이터베이스 백업
backup_h2_database() {
    log_info "H2 데이터베이스 백업 시작..."

    if [ -d "${H2_DATA_DIR}" ]; then
        # H2 데이터 파일 복사
        if ls "${H2_DATA_DIR}"/*.mv.db 1> /dev/null 2>&1; then
            mkdir -p "${BACKUP_DIR}/database"
            cp "${H2_DATA_DIR}"/*.mv.db "${BACKUP_DIR}/database/" 2>/dev/null || true
            cp "${H2_DATA_DIR}"/*.trace.db "${BACKUP_DIR}/database/" 2>/dev/null || true
            log_info "H2 데이터베이스 백업 완료"
        else
            log_warn "H2 데이터 파일을 찾을 수 없습니다"
        fi
    else
        log_warn "H2 데이터 디렉토리가 존재하지 않습니다: ${H2_DATA_DIR}"
    fi
}

# 위키 파일 백업
backup_wiki_files() {
    log_info "위키 파일 백업 시작..."

    mkdir -p "${BACKUP_DIR}/files"

    # 위키 첨부 파일
    if [ -d "${WIKI_FILES_DIR}" ]; then
        cp -r "${WIKI_FILES_DIR}" "${BACKUP_DIR}/files/wiki-files"
        log_info "위키 첨부 파일 백업 완료"
    else
        log_warn "위키 파일 디렉토리가 존재하지 않습니다: ${WIKI_FILES_DIR}"
    fi

    # 위키 이미지
    if [ -d "${WIKI_IMAGES_DIR}" ]; then
        cp -r "${WIKI_IMAGES_DIR}" "${BACKUP_DIR}/files/wiki-images"
        log_info "위키 이미지 백업 완료"
    else
        log_warn "위키 이미지 디렉토리가 존재하지 않습니다: ${WIKI_IMAGES_DIR}"
    fi
}

# 설정 파일 백업
backup_config() {
    log_info "설정 파일 백업 시작..."

    mkdir -p "${BACKUP_DIR}/config"

    # application.yml 백업 (민감 정보 제외)
    if [ -f "${PROJECT_ROOT}/src/main/resources/application.yml" ]; then
        # 비밀번호 등 민감 정보 마스킹
        sed 's/password:.*/password: *****/g' \
            "${PROJECT_ROOT}/src/main/resources/application.yml" > \
            "${BACKUP_DIR}/config/application.yml.masked"
        log_info "설정 파일 백업 완료 (민감 정보 마스킹됨)"
    fi
}

# 백업 압축
compress_backup() {
    log_info "백업 파일 압축 중..."

    cd "${BACKUP_BASE_DIR}"
    tar -czf "${TIMESTAMP}.tar.gz" "${TIMESTAMP}"
    rm -rf "${TIMESTAMP}"

    log_info "백업 압축 완료: ${BACKUP_BASE_DIR}/${TIMESTAMP}.tar.gz"
}

# 오래된 백업 정리 (30일 이상)
cleanup_old_backups() {
    log_info "오래된 백업 정리 (30일 이상)..."

    find "${BACKUP_BASE_DIR}" -name "*.tar.gz" -mtime +30 -delete 2>/dev/null || true
    log_info "오래된 백업 정리 완료"
}

# 백업 요약 출력
print_summary() {
    echo ""
    echo "============================================================"
    echo -e "${GREEN}백업 완료!${NC}"
    echo "============================================================"
    echo "백업 파일: ${BACKUP_BASE_DIR}/${TIMESTAMP}.tar.gz"
    echo "백업 크기: $(du -h "${BACKUP_BASE_DIR}/${TIMESTAMP}.tar.gz" | cut -f1)"
    echo "백업 시간: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "============================================================"
}

# 메인 실행
main() {
    log_info "SR 관리 시스템 백업 시작..."
    log_info "프로젝트 루트: ${PROJECT_ROOT}"

    create_backup_dir

    if [ "$BACKUP_DB" = true ]; then
        backup_h2_database
    fi

    if [ "$BACKUP_FILES" = true ]; then
        backup_wiki_files
    fi

    backup_config
    compress_backup
    cleanup_old_backups
    print_summary
}

main
