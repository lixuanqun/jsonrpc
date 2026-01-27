#!/bin/bash

# 启动 JSON-RPC 服务器脚本
#
# 使用方式:
#   ./start_server.sh [选项]
#
# 选项:
#   -p, --port <port>    服务器端口 (默认: 18080)
#   --background         后台运行
#   --help               显示帮助信息

set -e

PORT=18080
BACKGROUND=false
PROJECT_DIR=$(dirname $(dirname $(realpath $0)))
PID_FILE="$PROJECT_DIR/benchmark/server.pid"
LOG_FILE="$PROJECT_DIR/benchmark/server.log"

# 颜色定义
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

print_usage() {
    echo "启动 JSON-RPC 服务器脚本"
    echo ""
    echo "使用方式: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -p, --port <port>    服务器端口 (默认: 18080)"
    echo "  --background         后台运行"
    echo "  --help               显示帮助信息"
}

# 解析参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -p|--port)
            PORT="$2"
            shift 2
            ;;
        --background)
            BACKGROUND=true
            shift
            ;;
        --help)
            print_usage
            exit 0
            ;;
        *)
            print_error "未知选项: $1"
            print_usage
            exit 1
            ;;
    esac
done

# 编译项目
build_project() {
    print_info "编译项目..."
    cd "$PROJECT_DIR"
    mvn clean package -DskipTests -q
    
    # 下载依赖
    if [ ! -d "target/dependency" ]; then
        mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -q
    fi
    
    print_success "编译完成"
}

# 启动服务器
start_server() {
    print_info "启动 JSON-RPC 服务器 (端口: $PORT)..."
    
    # 构建classpath
    CLASSPATH="$PROJECT_DIR/target/classes"
    for jar in "$PROJECT_DIR/target/dependency"/*.jar; do
        if [ -f "$jar" ]; then
            CLASSPATH="$CLASSPATH:$jar"
        fi
    done
    
    if [ "$BACKGROUND" = true ]; then
        nohup java -cp "$CLASSPATH" \
            -Xms256m -Xmx512m \
            com.lixq.jsonrpc.JsonRpcServer > "$LOG_FILE" 2>&1 &
        
        echo $! > "$PID_FILE"
        print_success "服务器已在后台启动 (PID: $(cat $PID_FILE))"
        print_info "日志文件: $LOG_FILE"
    else
        java -cp "$CLASSPATH" \
            -Xms256m -Xmx512m \
            com.lixq.jsonrpc.JsonRpcServer
    fi
}

main() {
    build_project
    start_server
}

main
