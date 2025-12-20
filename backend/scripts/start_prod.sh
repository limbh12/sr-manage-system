#!/bin/bash

# 스크립트 파일의 절대 경로
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# 백엔드 루트 경로
BACKEND_DIR="$(dirname "$SCRIPT_DIR")"
LOG_DIR="$BACKEND_DIR/logs"
JAR_FILE="$BACKEND_DIR/target/sr-management-0.0.1-SNAPSHOT.jar"

# 로그 디렉토리 생성
if [ ! -d "$LOG_DIR" ]; then
    mkdir -p "$LOG_DIR"
fi

# JAR 파일 확인
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found at $JAR_FILE"
    echo "Please build the project first using: mvn clean package"
    exit 1
fi

# 이미 실행 중인지 확인 (포트 8080)
PID=$(lsof -t -i:8080)
if [ -n "$PID" ]; then
    echo "Error: Application is already running on port 8080 (PID: $PID)"
    exit 1
fi

echo "Starting SR Management System (Production Environment)..."
echo "Logs will be written to: $LOG_DIR/server.log"
echo "Log rotation is handled by the application (server-yyyymmdd.log)"

# 백엔드 디렉토리로 이동 (로그 파일 상대 경로 생성을 위해)
cd "$BACKEND_DIR"

# 서버 실행
# -Dspring.profiles.active=prod: 운영 프로필 사용
# 표준 출력은 버리고(/dev/null), 애플리케이션 내부 로깅 설정(application.yml)에 따라 logs/server.log에 기록
nohup java -Xmx2g -Xms1g -jar -Dspring.profiles.active=prod "$JAR_FILE" > /dev/null 2>&1 &

NEW_PID=$!
echo "Server started successfully with PID: $NEW_PID"
