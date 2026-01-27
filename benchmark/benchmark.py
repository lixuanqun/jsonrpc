#!/usr/bin/env python3
"""
JSON-RPC 压测工具 (Python版本)

使用方式:
    python benchmark.py [选项]

选项:
    -h, --host <host>       目标服务器地址 (默认: 127.0.0.1)
    -p, --port <port>       目标服务器端口 (默认: 18080)
    -c, --clients <num>     并发客户端数量 (默认: 10)
    -n, --requests <num>    每个客户端请求数 (默认: 100)
    --timeout <sec>         请求超时时间 (默认: 30)
    --help                  显示帮助信息

依赖:
    无需额外依赖，使用标准库

示例:
    python benchmark.py -c 50 -n 1000
    python benchmark.py -h 192.168.1.100 -p 8080 -c 100 -n 500
"""

import argparse
import json
import socket
import threading
import time
import uuid
import statistics
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from typing import List, Optional


@dataclass
class BenchmarkResult:
    """压测结果"""
    concurrent_clients: int
    requests_per_client: int
    total_requests: int
    success_count: int
    fail_count: int
    duration_seconds: float
    throughput: float
    min_latency_ms: float
    max_latency_ms: float
    avg_latency_ms: float
    p50_latency_ms: float
    p95_latency_ms: float
    p99_latency_ms: float
    
    def __str__(self):
        success_rate = (self.success_count / self.total_requests * 100) if self.total_requests > 0 else 0
        
        return f"""
╔══════════════════════════════════════════════════════════════╗
║               JSON-RPC Benchmark Results (Python)            ║
╠══════════════════════════════════════════════════════════════╣
║ Concurrent Clients:     {self.concurrent_clients:10d}                           ║
║ Requests Per Client:    {self.requests_per_client:10d}                           ║
║ Total Requests:         {self.total_requests:10d}                           ║
╠══════════════════════════════════════════════════════════════╣
║ Success Count:          {self.success_count:10d}                           ║
║ Fail Count:             {self.fail_count:10d}                           ║
║ Success Rate:           {success_rate:9.2f}%                           ║
╠══════════════════════════════════════════════════════════════╣
║ Duration:               {self.duration_seconds:10.2f} seconds                   ║
║ Throughput:             {self.throughput:10.2f} requests/sec              ║
╠══════════════════════════════════════════════════════════════╣
║                    Latency Statistics                        ║
╠══════════════════════════════════════════════════════════════╣
║ Min Latency:            {self.min_latency_ms:10.2f} ms                         ║
║ Max Latency:            {self.max_latency_ms:10.2f} ms                         ║
║ Avg Latency:            {self.avg_latency_ms:10.2f} ms                         ║
║ P50 Latency:            {self.p50_latency_ms:10.2f} ms                         ║
║ P95 Latency:            {self.p95_latency_ms:10.2f} ms                         ║
║ P99 Latency:            {self.p99_latency_ms:10.2f} ms                         ║
╚══════════════════════════════════════════════════════════════╝
"""
    
    def to_csv(self) -> str:
        return f"{self.concurrent_clients},{self.requests_per_client},{self.total_requests}," \
               f"{self.success_count},{self.fail_count},{self.duration_seconds:.2f},{self.throughput:.2f}," \
               f"{self.min_latency_ms:.2f},{self.max_latency_ms:.2f},{self.avg_latency_ms:.2f}," \
               f"{self.p50_latency_ms:.2f},{self.p95_latency_ms:.2f},{self.p99_latency_ms:.2f}"
    
    @staticmethod
    def csv_header() -> str:
        return "concurrent_clients,requests_per_client,total_requests," \
               "success_count,fail_count,duration_sec,throughput," \
               "min_latency_ms,max_latency_ms,avg_latency_ms," \
               "p50_latency_ms,p95_latency_ms,p99_latency_ms"


class JsonRpcClient:
    """JSON-RPC TCP 客户端"""
    
    def __init__(self, host: str, port: int, timeout: int = 30):
        self.host = host
        self.port = port
        self.timeout = timeout
        self.socket: Optional[socket.socket] = None
    
    def connect(self):
        """连接到服务器"""
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.settimeout(self.timeout)
        self.socket.connect((self.host, self.port))
    
    def close(self):
        """关闭连接"""
        if self.socket:
            try:
                self.socket.close()
            except:
                pass
    
    def send_request(self, method: str, params, request_id: str = None) -> dict:
        """发送 JSON-RPC 请求"""
        if request_id is None:
            request_id = str(uuid.uuid4())
        
        request = {
            "jsonrpc": "2.0",
            "method": method,
            "params": params,
            "id": request_id
        }
        
        # 发送请求（带换行符分隔）
        request_json = json.dumps(request) + "\n"
        self.socket.sendall(request_json.encode('utf-8'))
        
        # 接收响应
        response_data = b""
        while True:
            chunk = self.socket.recv(4096)
            if not chunk:
                break
            response_data += chunk
            if b"\n" in response_data:
                break
        
        # 解析响应
        response_json = response_data.decode('utf-8').strip()
        return json.loads(response_json)


class JsonRpcBenchmark:
    """JSON-RPC 压测工具"""
    
    def __init__(self, host: str, port: int, concurrent_clients: int, 
                 requests_per_client: int, timeout: int = 30):
        self.host = host
        self.port = port
        self.concurrent_clients = concurrent_clients
        self.requests_per_client = requests_per_client
        self.timeout = timeout
        
        # 统计数据（使用锁保护）
        self.lock = threading.Lock()
        self.success_count = 0
        self.fail_count = 0
        self.latencies: List[float] = []
    
    def run_client(self, client_id: int, start_event: threading.Event) -> None:
        """运行单个客户端"""
        client = JsonRpcClient(self.host, self.port, self.timeout)
        
        try:
            client.connect()
            
            # 等待所有客户端准备就绪
            start_event.wait()
            
            for i in range(self.requests_per_client):
                start_time = time.perf_counter()
                
                try:
                    message = f"benchmark_{client_id}_{i}"
                    response = client.send_request("echo", message)
                    
                    latency = (time.perf_counter() - start_time) * 1000  # 毫秒
                    
                    if "error" not in response or response.get("error") is None:
                        with self.lock:
                            self.success_count += 1
                            self.latencies.append(latency)
                    else:
                        with self.lock:
                            self.fail_count += 1
                            
                except Exception as e:
                    with self.lock:
                        self.fail_count += 1
        
        except Exception as e:
            # 连接失败时，所有请求都算失败
            with self.lock:
                self.fail_count += self.requests_per_client
        
        finally:
            client.close()
    
    def run(self) -> BenchmarkResult:
        """运行压测"""
        print(f"[INFO] Starting benchmark: {self.concurrent_clients} concurrent clients, "
              f"{self.requests_per_client} requests per client")
        print(f"[INFO] Target server: {self.host}:{self.port}")
        
        start_event = threading.Event()
        
        with ThreadPoolExecutor(max_workers=self.concurrent_clients) as executor:
            # 提交所有客户端任务
            futures = [
                executor.submit(self.run_client, i, start_event)
                for i in range(self.concurrent_clients)
            ]
            
            # 记录开始时间并启动所有客户端
            time.sleep(0.1)  # 等待所有客户端就绪
            start_time = time.perf_counter()
            start_event.set()
            
            # 等待所有任务完成
            for future in as_completed(futures):
                try:
                    future.result()
                except Exception as e:
                    print(f"[ERROR] Client error: {e}")
        
        end_time = time.perf_counter()
        duration = end_time - start_time
        
        return self._calculate_result(duration)
    
    def _calculate_result(self, duration: float) -> BenchmarkResult:
        """计算统计结果"""
        total = self.success_count + self.fail_count
        throughput = total / duration if duration > 0 else 0
        
        if self.latencies:
            sorted_latencies = sorted(self.latencies)
            size = len(sorted_latencies)
            
            min_latency = sorted_latencies[0]
            max_latency = sorted_latencies[-1]
            avg_latency = statistics.mean(sorted_latencies)
            p50_latency = sorted_latencies[int(size * 0.5)]
            p95_latency = sorted_latencies[int(size * 0.95)]
            p99_latency = sorted_latencies[int(size * 0.99)]
        else:
            min_latency = max_latency = avg_latency = 0
            p50_latency = p95_latency = p99_latency = 0
        
        return BenchmarkResult(
            concurrent_clients=self.concurrent_clients,
            requests_per_client=self.requests_per_client,
            total_requests=total,
            success_count=self.success_count,
            fail_count=self.fail_count,
            duration_seconds=duration,
            throughput=throughput,
            min_latency_ms=min_latency,
            max_latency_ms=max_latency,
            avg_latency_ms=avg_latency,
            p50_latency_ms=p50_latency,
            p95_latency_ms=p95_latency,
            p99_latency_ms=p99_latency
        )


def check_server(host: str, port: int) -> bool:
    """检查服务器是否可达"""
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(3)
        result = sock.connect_ex((host, port))
        sock.close()
        return result == 0
    except:
        return False


def main():
    parser = argparse.ArgumentParser(
        description='JSON-RPC 压测工具 (Python版本)',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog='''
示例:
    python benchmark.py -c 50 -n 1000
    python benchmark.py -h 192.168.1.100 -p 8080 -c 100 -n 500
'''
    )
    
    parser.add_argument('-H', '--host', default='127.0.0.1',
                        help='目标服务器地址 (默认: 127.0.0.1)')
    parser.add_argument('-p', '--port', type=int, default=18080,
                        help='目标服务器端口 (默认: 18080)')
    parser.add_argument('-c', '--clients', type=int, default=10,
                        help='并发客户端数量 (默认: 10)')
    parser.add_argument('-n', '--requests', type=int, default=100,
                        help='每个客户端请求数 (默认: 100)')
    parser.add_argument('--timeout', type=int, default=30,
                        help='请求超时时间 (默认: 30)')
    parser.add_argument('--csv', action='store_true',
                        help='输出 CSV 格式结果')
    parser.add_argument('--warmup', action='store_true',
                        help='进行预热')
    
    args = parser.parse_args()
    
    print("╔══════════════════════════════════════════════════════════════╗")
    print("║              JSON-RPC 压测工具 (Python版本)                  ║")
    print("╚══════════════════════════════════════════════════════════════╝")
    print()
    
    # 检查服务器
    print(f"[INFO] 检查服务器 {args.host}:{args.port} 是否可达...")
    if not check_server(args.host, args.port):
        print(f"[ERROR] 无法连接到服务器 {args.host}:{args.port}")
        print("[INFO] 请确保 JSON-RPC 服务器已启动")
        return 1
    print("[SUCCESS] 服务器可达")
    
    # 预热
    if args.warmup:
        print("[INFO] 运行预热 (5个客户端，每个10个请求)...")
        warmup = JsonRpcBenchmark(args.host, args.port, 5, 10, args.timeout)
        warmup.run()
        print("[SUCCESS] 预热完成")
        time.sleep(1)
    
    # 运行压测
    benchmark = JsonRpcBenchmark(
        args.host, args.port, 
        args.clients, args.requests, 
        args.timeout
    )
    
    result = benchmark.run()
    
    if args.csv:
        print(BenchmarkResult.csv_header())
        print(result.to_csv())
    else:
        print(result)
    
    print("[SUCCESS] 压测完成!")
    return 0


if __name__ == '__main__':
    exit(main())
