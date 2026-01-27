#!/bin/bash

# 停止 JSON-RPC 服务器脚本

PROJECT_DIR=$(dirname $(dirname $(realpath $0)))
PID_FILE="$PROJECT_DIR/benchmark/server.pid"

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

if [ ! -f "$PID_FILE" ]; then
    print_error "未找到 PID 文件，服务器可能未运行"
    exit 1
fi

PID=$(cat "$PID_FILE")

if ps -p $PID > /dev/null 2>&1; then
    print_info "停止服务器 (PID: $PID)..."
    kill $PID
    
    # 等待进程结束
    for i in {1..10}; do
        if ! ps -p $PID > /dev/null 2>&1; then
            break
        fi
        sleep 1
    done
    
    # 如果还没有结束，强制杀死
    if ps -p $PID > /dev/null 2>&1; then
        print_info "强制停止服务器..."
        kill -9 $PID
    fi
    
    rm -f "$PID_FILE"
    print_success "服务器已停止"
else
    print_info "服务器进程已不存在"
    rm -f "$PID_FILE"
fi
