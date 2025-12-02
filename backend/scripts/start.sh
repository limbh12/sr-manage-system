#!/bin/bash

# 스크립트 파일의 절대 경로
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# 백엔드 루트 경로 (scripts 폴더의 상위)
BACKEND_DIR="$(dirname "$SCRIPT_DIR")"
LOG_DIR="$BACKEND_DIR/logs"
LOG_FILE="$LOG_DIR/server.log"
JAR_FILE="$BACKEND_DIR/target/sr-management-0.0.1-SNAPSHOT.jar"

# 로그 디렉토리 생성
if [ ! -d "$LOG_DIR" ]; then
    mkdir -p "$LOG_DIR"
    echo "Created log directory: $LOG_DIR"
fi

# JAR 파일 확인 및 빌드
if [ ! -f "$JAR_FILE" ]; then
    echo "JAR file not found at $JAR_FILE"
    echo "Building project..."
    cd "$BACKEND_DIR"
    # mvn wrapper가 있다면 ./mvnw 사용 권장, 여기서는 시스템 mvn 사용 가정
    if [ -f "./mvnw" ]; then
        ./mvnw clean package -DskipTests
    else
        mvn clean package -DskipTests
    fi
    
    if [ $? -ne 0 ]; then
        echo "Build failed."
        exit 1
    fi
fi

# 이미 실행 중인지 확인
PID=$(lsof -t -i:8080)
if [ -n "$PID" ]; then
    echo "Application is already running on port 8080 (PID: $PID)"
    exit 1
fi

echo "Starting backend server..."
echo "Logs will be written to: $LOG_FILE"

# 백엔드 실행 (nohup 사용, 로그 리다이렉션)
nohup java -jar "$JAR_FILE" > "$LOG_FILE" 2>&1 &

NEW_PID=$!
echo "Server started with PID: $NEW_PID"
