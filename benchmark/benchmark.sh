#!/bin/bash

# JSON-RPC 压测脚本
# 
# 使用方式:
#   ./benchmark.sh [选项]
#
# 选项:
#   -h, --host <host>       目标服务器地址 (默认: 127.0.0.1)
#   -p, --port <port>       目标服务器端口 (默认: 18080)
#   -c, --clients <num>     并发客户端数量 (默认: 10)
#   -n, --requests <num>    每个客户端请求数 (默认: 100)
#   --warmup                是否进行预热 (默认: true)
#   --help                  显示帮助信息

set -e

# 默认参数
HOST="127.0.0.1"
PORT=18080
CLIENTS=10
REQUESTS=100
WARMUP=true
PROJECT_DIR=$(dirname $(dirname $(realpath $0)))

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 打印帮助信息
print_usage() {
    echo "JSON-RPC 压测脚本"
    echo ""
    echo "使用方式: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -h, --host <host>       目标服务器地址 (默认: 127.0.0.1)"
    echo "  -p, --port <port>       目标服务器端口 (默认: 18080)"
    echo "  -c, --clients <num>     并发客户端数量 (默认: 10)"
    echo "  -n, --requests <num>    每个客户端请求数 (默认: 100)"
    echo "  --no-warmup             跳过预热阶段"
    echo "  --help                  显示帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 -c 50 -n 1000        # 50个并发客户端，每个发送1000个请求"
    echo "  $0 -h 192.168.1.100 -p 8080"
}

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--host)
            HOST="$2"
            shift 2
            ;;
        -p|--port)
            PORT="$2"
            shift 2
            ;;
        -c|--clients)
            CLIENTS="$2"
            shift 2
            ;;
        -n|--requests)
            REQUESTS="$2"
            shift 2
            ;;
        --no-warmup)
            WARMUP=false
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

# 检查服务器是否可达
check_server() {
    print_info "检查服务器 ${HOST}:${PORT} 是否可达..."
    
    if ! nc -z -w 3 "$HOST" "$PORT" 2>/dev/null; then
        print_error "无法连接到服务器 ${HOST}:${PORT}"
        print_info "请确保 JSON-RPC 服务器已启动"
        exit 1
    fi
    
    print_success "服务器可达"
}

# 编译项目
build_project() {
    print_info "编译项目..."
    cd "$PROJECT_DIR"
    
    if [ ! -f "pom.xml" ]; then
        print_error "未找到 pom.xml 文件"
        exit 1
    fi
    
    mvn clean package -DskipTests -q
    
    if [ $? -eq 0 ]; then
        print_success "项目编译完成"
    else
        print_error "项目编译失败"
        exit 1
    fi
}

# 运行预热
run_warmup() {
    if [ "$WARMUP" = true ]; then
        print_info "运行预热 (5个客户端，每个10个请求)..."
        
        java -cp "$PROJECT_DIR/target/classes:$PROJECT_DIR/target/dependency/*" \
            com.lixq.jsonrpc.benchmark.JsonRpcBenchmark \
            --host "$HOST" --port "$PORT" --clients 5 --requests 10 > /dev/null 2>&1 || true
        
        print_success "预热完成"
        sleep 1
    fi
}

# 运行压测
run_benchmark() {
    print_info "开始压测..."
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  测试配置"
    echo "═══════════════════════════════════════════════════════════════"
    echo "  目标服务器: ${HOST}:${PORT}"
    echo "  并发客户端: ${CLIENTS}"
    echo "  每客户端请求数: ${REQUESTS}"
    echo "  总请求数: $((CLIENTS * REQUESTS))"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    
    # 检查依赖是否存在
    DEPS_DIR="$PROJECT_DIR/target/dependency"
    if [ ! -d "$DEPS_DIR" ]; then
        print_info "下载依赖..."
        mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -q
    fi
    
    # 构建classpath
    CLASSPATH="$PROJECT_DIR/target/classes"
    for jar in "$DEPS_DIR"/*.jar; do
        if [ -f "$jar" ]; then
            CLASSPATH="$CLASSPATH:$jar"
        fi
    done
    
    # 运行压测
    java -cp "$CLASSPATH" \
        -Xms512m -Xmx1024m \
        com.lixq.jsonrpc.benchmark.JsonRpcBenchmark \
        --host "$HOST" --port "$PORT" --clients "$CLIENTS" --requests "$REQUESTS"
}

# 主流程
main() {
    echo ""
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║              JSON-RPC 自动化压测工具                         ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo ""
    
    check_server
    build_project
    run_warmup
    run_benchmark
    
    echo ""
    print_success "压测完成!"
}

main
