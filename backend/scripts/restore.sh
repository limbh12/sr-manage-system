#!/bin/bash

# ============================================================
# SR 관리 시스템 - 데이터 복원 스크립트
# ============================================================
# 사용법: ./restore.sh <백업파일.tar.gz> [옵션]
#   옵션:
#     --db-only      데이터베이스만 복원
#     --files-only   파일만 복원
#     --no-confirm   확인 없이 복원 실행
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

# 파일 저장 경로
WIKI_FILES_DIR="${PROJECT_ROOT}/data/wiki-files"
WIKI_IMAGES_DIR="${PROJECT_ROOT}/data/wiki-images"
H2_DATA_DIR="${PROJECT_ROOT}/data"

# 옵션 파싱
RESTORE_DB=true
RESTORE_FILES=true
NO_CONFIRM=false
BACKUP_FILE=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --db-only)
            RESTORE_FILES=false
            shift
            ;;
        --files-only)
            RESTORE_DB=false
            shift
            ;;
        --no-confirm)
            NO_CONFIRM=true
            shift
            ;;
        *.tar.gz)
            BACKUP_FILE="$1"
            shift
            ;;
        *)
            echo -e "${RED}알 수 없는 옵션: $1${NC}"
            exit 1
            ;;
    esac
done

# 백업 파일 확인
if [ -z "$BACKUP_FILE" ]; then
    echo -e "${RED}사용법: $0 <백업파일.tar.gz> [옵션]${NC}"
    exit 1
fi

if [ ! -f "$BACKUP_FILE" ]; then
    echo -e "${RED}백업 파일을 찾을 수 없습니다: $BACKUP_FILE${NC}"
    exit 1
fi

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

# 서버 실행 중인지 확인
check_server_status() {
    if pgrep -f "sr-management" > /dev/null 2>&1; then
        log_error "서버가 실행 중입니다. 복원 전에 서버를 중지해주세요."
        log_info "실행: ./stop.sh"
        exit 1
    fi
}

# 확인 프롬프트
confirm_restore() {
    if [ "$NO_CONFIRM" = false ]; then
        echo ""
        echo -e "${YELLOW}경고: 이 작업은 기존 데이터를 덮어씁니다!${NC}"
        echo "백업 파일: $BACKUP_FILE"
        echo ""
        read -p "계속하시겠습니까? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "복원이 취소되었습니다."
            exit 0
        fi
    fi
}

# 백업 파일 압축 해제
extract_backup() {
    log_info "백업 파일 압축 해제 중..."

    TEMP_DIR=$(mktemp -d)
    tar -xzf "$BACKUP_FILE" -C "$TEMP_DIR"

    # 백업 폴더 이름 추출
    BACKUP_FOLDER=$(ls "$TEMP_DIR" | head -1)
    EXTRACTED_DIR="${TEMP_DIR}/${BACKUP_FOLDER}"

    log_info "압축 해제 완료: ${EXTRACTED_DIR}"
}

# H2 데이터베이스 복원
restore_h2_database() {
    log_info "H2 데이터베이스 복원 시작..."

    if [ -d "${EXTRACTED_DIR}/database" ]; then
        # 기존 데이터 백업 (임시)
        if ls "${H2_DATA_DIR}"/*.mv.db 1> /dev/null 2>&1; then
            mkdir -p "${H2_DATA_DIR}/.backup_before_restore"
            cp "${H2_DATA_DIR}"/*.mv.db "${H2_DATA_DIR}/.backup_before_restore/" 2>/dev/null || true
        fi

        # 복원
        cp "${EXTRACTED_DIR}/database"/*.mv.db "${H2_DATA_DIR}/" 2>/dev/null || true
        cp "${EXTRACTED_DIR}/database"/*.trace.db "${H2_DATA_DIR}/" 2>/dev/null || true

        log_info "H2 데이터베이스 복원 완료"
    else
        log_warn "백업에 데이터베이스가 포함되어 있지 않습니다"
    fi
}

# 위키 파일 복원
restore_wiki_files() {
    log_info "위키 파일 복원 시작..."

    if [ -d "${EXTRACTED_DIR}/files" ]; then
        # 위키 첨부 파일
        if [ -d "${EXTRACTED_DIR}/files/wiki-files" ]; then
            mkdir -p "${WIKI_FILES_DIR}"
            cp -r "${EXTRACTED_DIR}/files/wiki-files"/* "${WIKI_FILES_DIR}/" 2>/dev/null || true
            log_info "위키 첨부 파일 복원 완료"
        fi

        # 위키 이미지
        if [ -d "${EXTRACTED_DIR}/files/wiki-images" ]; then
            mkdir -p "${WIKI_IMAGES_DIR}"
            cp -r "${EXTRACTED_DIR}/files/wiki-images"/* "${WIKI_IMAGES_DIR}/" 2>/dev/null || true
            log_info "위키 이미지 복원 완료"
        fi
    else
        log_warn "백업에 파일이 포함되어 있지 않습니다"
    fi
}

# 임시 파일 정리
cleanup() {
    log_info "임시 파일 정리 중..."
    rm -rf "$TEMP_DIR"
}

# 복원 요약 출력
print_summary() {
    echo ""
    echo "============================================================"
    echo -e "${GREEN}복원 완료!${NC}"
    echo "============================================================"
    echo "복원된 백업: ${BACKUP_FILE}"
    echo "복원 시간: $(date '+%Y-%m-%d %H:%M:%S')"
    echo ""
    echo "다음 단계:"
    echo "  1. 서버 시작: ./start.sh"
    echo "  2. 웹 브라우저에서 확인: http://localhost:8080"
    echo "============================================================"
}

# 메인 실행
main() {
    log_info "SR 관리 시스템 복원 시작..."

    check_server_status
    confirm_restore
    extract_backup

    if [ "$RESTORE_DB" = true ]; then
        restore_h2_database
    fi

    if [ "$RESTORE_FILES" = true ]; then
        restore_wiki_files
    fi

    cleanup
    print_summary
}

main
