#!/bin/bash

# 8080 포트를 사용하는 프로세스 ID 찾기
PID=$(lsof -t -i:8080)

if [ -z "$PID" ]; then
    echo "No application running on port 8080."
    exit 0
fi

echo "Stopping application on port 8080 (PID: $PID)..."
kill $PID

# 종료 대기
sleep 2

# 강제 종료 확인
if lsof -t -i:8080 > /dev/null; then
    echo "Application did not stop gracefully. Forcing kill..."
    kill -9 $PID
fi

echo "Application stopped."
