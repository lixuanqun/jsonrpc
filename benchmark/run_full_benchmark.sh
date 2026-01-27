#!/bin/bash

# JSON-RPC 完整压测脚本
# 运行多轮不同配置的压测，生成对比报告
#
# 使用方式:
#   ./run_full_benchmark.sh [选项]

set -e

# 默认参数
HOST="127.0.0.1"
PORT=18080
OUTPUT_DIR="benchmark_results"
PROJECT_DIR=$(dirname $(dirname $(realpath $0)))

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

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

print_header() {
    echo -e "${CYAN}$1${NC}"
}

# 打印帮助信息
print_usage() {
    echo "JSON-RPC 完整压测脚本"
    echo ""
    echo "使用方式: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -h, --host <host>       目标服务器地址 (默认: 127.0.0.1)"
    echo "  -p, --port <port>       目标服务器端口 (默认: 18080)"
    echo "  -o, --output <dir>      结果输出目录 (默认: benchmark_results)"
    echo "  --start-server          自动启动服务器"
    echo "  --help                  显示帮助信息"
}

# 解析命令行参数
START_SERVER=false
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
        -o|--output)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        --start-server)
            START_SERVER=true
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

# 创建输出目录
mkdir -p "$OUTPUT_DIR"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_FILE="$OUTPUT_DIR/benchmark_report_${TIMESTAMP}.txt"
CSV_FILE="$OUTPUT_DIR/benchmark_results_${TIMESTAMP}.csv"

# 编译项目
build_project() {
    print_info "编译项目..."
    cd "$PROJECT_DIR"
    mvn clean package -DskipTests -q
    
    if [ ! -d "target/dependency" ]; then
        mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -q
    fi
    
    print_success "编译完成"
}

# 启动服务器
start_server() {
    if [ "$START_SERVER" = true ]; then
        print_info "启动 JSON-RPC 服务器..."
        
        CLASSPATH="$PROJECT_DIR/target/classes"
        for jar in "$PROJECT_DIR/target/dependency"/*.jar; do
            if [ -f "$jar" ]; then
                CLASSPATH="$CLASSPATH:$jar"
            fi
        done
        
        java -cp "$CLASSPATH" -Xms256m -Xmx512m \
            com.lixq.jsonrpc.JsonRpcServer > /dev/null 2>&1 &
        SERVER_PID=$!
        
        # 等待服务器启动
        sleep 3
        
        if ! ps -p $SERVER_PID > /dev/null 2>&1; then
            print_error "服务器启动失败"
            exit 1
        fi
        
        print_success "服务器已启动 (PID: $SERVER_PID)"
    fi
}

# 停止服务器
stop_server() {
    if [ "$START_SERVER" = true ] && [ ! -z "$SERVER_PID" ]; then
        print_info "停止服务器..."
        kill $SERVER_PID 2>/dev/null || true
        wait $SERVER_PID 2>/dev/null || true
        print_success "服务器已停止"
    fi
}

# 检查服务器
check_server() {
    print_info "检查服务器 ${HOST}:${PORT}..."
    
    if ! nc -z -w 3 "$HOST" "$PORT" 2>/dev/null; then
        print_error "无法连接到服务器 ${HOST}:${PORT}"
        return 1
    fi
    
    print_success "服务器可达"
    return 0
}

# 运行单次压测
run_benchmark() {
    local clients=$1
    local requests=$2
    local label=$3
    
    print_info "运行压测: $label (${clients} 客户端, ${requests} 请求/客户端)"
    
    CLASSPATH="$PROJECT_DIR/target/classes"
    for jar in "$PROJECT_DIR/target/dependency"/*.jar; do
        if [ -f "$jar" ]; then
            CLASSPATH="$CLASSPATH:$jar"
        fi
    done
    
    # 运行并捕获输出
    java -cp "$CLASSPATH" -Xms512m -Xmx1024m \
        com.lixq.jsonrpc.benchmark.JsonRpcBenchmark \
        --host "$HOST" --port "$PORT" --clients "$clients" --requests "$requests"
}

# 生成报告
generate_report() {
    print_info "生成压测报告..."
    
    {
        echo "═══════════════════════════════════════════════════════════════════════════════"
        echo "                         JSON-RPC 压测报告"
        echo "═══════════════════════════════════════════════════════════════════════════════"
        echo ""
        echo "测试时间: $(date)"
        echo "目标服务器: ${HOST}:${PORT}"
        echo ""
        echo "测试配置:"
        echo "  - 轻负载: 10 客户端 × 100 请求/客户端 = 1,000 请求"
        echo "  - 中负载: 50 客户端 × 200 请求/客户端 = 10,000 请求"
        echo "  - 高负载: 100 客户端 × 500 请求/客户端 = 50,000 请求"
        echo "  - 极限测试: 200 客户端 × 1000 请求/客户端 = 200,000 请求"
        echo ""
        echo "═══════════════════════════════════════════════════════════════════════════════"
    } > "$REPORT_FILE"
    
    # CSV 头
    echo "test_name,clients,requests_per_client,total,success,fail,duration_s,throughput,min_ms,max_ms,avg_ms,p50_ms,p95_ms,p99_ms" > "$CSV_FILE"
    
    print_success "报告将保存到: $REPORT_FILE"
}

# 主流程
main() {
    trap stop_server EXIT
    
    echo ""
    print_header "╔══════════════════════════════════════════════════════════════╗"
    print_header "║              JSON-RPC 完整压测工具                           ║"
    print_header "╚══════════════════════════════════════════════════════════════╝"
    echo ""
    
    build_project
    start_server
    
    # 等待服务器完全启动
    sleep 2
    
    if ! check_server; then
        exit 1
    fi
    
    generate_report
    
    echo ""
    print_header "═══════════════════════════════════════════════════════════════"
    print_header "                     开始压测                                   "
    print_header "═══════════════════════════════════════════════════════════════"
    echo ""
    
    # 预热
    print_info "运行预热..."
    run_benchmark 5 20 "预热" > /dev/null 2>&1 || true
    sleep 2
    
    # 轻负载测试
    echo ""
    echo "【轻负载测试】" | tee -a "$REPORT_FILE"
    run_benchmark 10 100 "轻负载" | tee -a "$REPORT_FILE"
    sleep 2
    
    # 中负载测试
    echo ""
    echo "【中负载测试】" | tee -a "$REPORT_FILE"
    run_benchmark 50 200 "中负载" | tee -a "$REPORT_FILE"
    sleep 2
    
    # 高负载测试
    echo ""
    echo "【高负载测试】" | tee -a "$REPORT_FILE"
    run_benchmark 100 500 "高负载" | tee -a "$REPORT_FILE"
    sleep 2
    
    # 极限测试
    echo ""
    echo "【极限测试】" | tee -a "$REPORT_FILE"
    run_benchmark 200 1000 "极限" | tee -a "$REPORT_FILE"
    
    echo ""
    print_header "═══════════════════════════════════════════════════════════════"
    print_header "                     压测完成                                   "
    print_header "═══════════════════════════════════════════════════════════════"
    echo ""
    
    print_success "完整报告已保存到: $REPORT_FILE"
    print_success "CSV 数据已保存到: $CSV_FILE"
}

main
