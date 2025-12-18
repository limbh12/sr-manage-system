#!/bin/bash

# 스크립트 파일의 절대 경로
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# 백엔드 루트 경로 (scripts 폴더의 상위)
BACKEND_DIR="$(dirname "$SCRIPT_DIR")"
# 프로젝트 루트 경로 (backend 폴더의 상위)
PROJECT_ROOT="$(dirname "$BACKEND_DIR")"
FRONTEND_DIR="$PROJECT_ROOT/frontend"
LOG_DIR="$BACKEND_DIR/logs"
LOG_FILE="$LOG_DIR/server.log"
JAR_FILE="$BACKEND_DIR/target/sr-management-0.0.1-SNAPSHOT.jar"
FRONTEND_DIST="$FRONTEND_DIR/dist"
BACKEND_STATIC="$BACKEND_DIR/src/main/resources/static"

# 로그 디렉토리 생성
if [ ! -d "$LOG_DIR" ]; then
    mkdir -p "$LOG_DIR"
    echo "Created log directory: $LOG_DIR"
fi

# 프론트엔드 빌드
echo "======================================"
echo "Building Frontend..."
echo "======================================"

if [ ! -d "$FRONTEND_DIR" ]; then
    echo "Error: Frontend directory not found at $FRONTEND_DIR"
    exit 1
fi

cd "$FRONTEND_DIR"

# node_modules가 없으면 npm install 실행
if [ ! -d "node_modules" ]; then
    echo "Installing frontend dependencies..."
    npm install
    if [ $? -ne 0 ]; then
        echo "Frontend dependency installation failed."
        exit 1
    fi
fi

# 프론트엔드 빌드 실행
echo "Building frontend application..."
npm run build
if [ $? -ne 0 ]; then
    echo "Frontend build failed."
    exit 1
fi

# 빌드된 파일을 백엔드 static 디렉토리로 복사
echo "Copying frontend build to backend static directory..."

# 기존 static 디렉토리 백업 (선택사항)
if [ -d "$BACKEND_STATIC" ]; then
    echo "Removing old static files..."
    rm -rf "$BACKEND_STATIC"/*
fi

# static 디렉토리 생성
mkdir -p "$BACKEND_STATIC"

# 빌드 파일 복사
if [ -d "$FRONTEND_DIST" ]; then
    cp -r "$FRONTEND_DIST"/* "$BACKEND_STATIC"/
    echo "Frontend build files copied successfully."
else
    echo "Error: Frontend build directory not found at $FRONTEND_DIST"
    exit 1
fi

# 백엔드 빌드
echo ""
echo "======================================"
echo "Building Backend..."
echo "======================================"

cd "$BACKEND_DIR"

# JAR 파일이 없거나 프론트엔드 빌드 후에는 항상 재빌드
echo "Building backend application..."
if [ -f "./mvnw" ]; then
    ./mvnw clean package -DskipTests
else
    mvn clean package -DskipTests
fi

if [ $? -ne 0 ]; then
    echo "Backend build failed."
    exit 1
fi

echo "Backend build completed successfully."

# 서버 시작
echo ""
echo "======================================"
echo "Starting Backend Server..."
echo "======================================"

# 이미 실행 중인지 확인
PID=$(lsof -t -i:8080)
if [ -n "$PID" ]; then
    echo "Application is already running on port 8080 (PID: $PID)"
    exit 1
fi

echo "Logs will be written to: $LOG_FILE"

# 백엔드 실행 (nohup 사용, 로그 리다이렉션)
nohup java -jar "$JAR_FILE" > "$LOG_FILE" 2>&1 &

NEW_PID=$!
echo ""
echo "======================================"
echo "Server started successfully!"
echo "======================================"
echo "PID: $NEW_PID"
echo "Log file: $LOG_FILE"
echo "Backend API: http://localhost:8080"
echo "Frontend: http://localhost:8080"
echo ""
echo "To stop the server, run: backend/scripts/stop.sh"
echo "To view logs, run: tail -f $LOG_FILE"
echo "======================================"
