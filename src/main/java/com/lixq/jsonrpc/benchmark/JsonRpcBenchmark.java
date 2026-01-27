package com.lixq.jsonrpc.benchmark;

import com.lixq.jsonrpc.JsonRpcClient;
import com.lixq.jsonrpc.core.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JSON-RPC 压测工具
 * 
 * 支持配置：
 * - 并发客户端数量
 * - 每个客户端请求数
 * - 目标服务器地址和端口
 * - 请求超时时间
 */
public class JsonRpcBenchmark {
    private static final Logger log = LoggerFactory.getLogger(JsonRpcBenchmark.class);
    
    private final String host;
    private final int port;
    private final int concurrentClients;
    private final int requestsPerClient;
    private final int connectTimeoutSeconds;
    private final int requestTimeoutSeconds;
    
    // 统计数据
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failCount = new AtomicInteger(0);
    private final AtomicLong totalLatencyNanos = new AtomicLong(0);
    private final ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();

    public JsonRpcBenchmark(String host, int port, int concurrentClients, int requestsPerClient) {
        this(host, port, concurrentClients, requestsPerClient, 10, 30);
    }

    public JsonRpcBenchmark(String host, int port, int concurrentClients, int requestsPerClient,
                           int connectTimeoutSeconds, int requestTimeoutSeconds) {
        this.host = host;
        this.port = port;
        this.concurrentClients = concurrentClients;
        this.requestsPerClient = requestsPerClient;
        this.connectTimeoutSeconds = connectTimeoutSeconds;
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    /**
     * 运行压测
     */
    public BenchmarkResult run() throws InterruptedException {
        log.info("Starting benchmark: {} concurrent clients, {} requests per client", 
                concurrentClients, requestsPerClient);
        log.info("Target server: {}:{}", host, port);
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrentClients);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(concurrentClients);
        
        List<Future<?>> futures = new ArrayList<>();
        
        // 启动所有客户端线程
        for (int i = 0; i < concurrentClients; i++) {
            final int clientId = i;
            Future<?> future = executor.submit(() -> {
                try {
                    runClient(clientId, startLatch);
                } catch (Exception e) {
                    log.error("Client {} error", clientId, e);
                } finally {
                    endLatch.countDown();
                }
            });
            futures.add(future);
        }
        
        // 记录开始时间并启动所有客户端
        long startTime = System.nanoTime();
        startLatch.countDown();
        
        // 等待所有客户端完成
        endLatch.await();
        long endTime = System.nanoTime();
        
        // 关闭线程池
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        // 计算统计结果
        return calculateResult(endTime - startTime);
    }

    private void runClient(int clientId, CountDownLatch startLatch) throws Exception {
        JsonRpcClient client = new JsonRpcClient(host, port);
        
        try {
            // 连接到服务器
            client.connect().get(connectTimeoutSeconds, TimeUnit.SECONDS);
            
            // 等待所有客户端准备就绪
            startLatch.await();
            
            // 发送请求
            for (int i = 0; i < requestsPerClient; i++) {
                long requestStart = System.nanoTime();
                
                try {
                    // 发送 echo 请求
                    String message = "benchmark_" + clientId + "_" + i;
                    RpcResponse response = client.sendRequest("echo", message)
                            .get(requestTimeoutSeconds, TimeUnit.SECONDS);
                    
                    long latency = System.nanoTime() - requestStart;
                    
                    if (response.isSuccess()) {
                        successCount.incrementAndGet();
                        totalLatencyNanos.addAndGet(latency);
                        latencies.add(latency);
                    } else {
                        failCount.incrementAndGet();
                        log.debug("Request failed: {}", response.getError());
                    }
                } catch (TimeoutException e) {
                    failCount.incrementAndGet();
                    log.debug("Request timeout for client {}", clientId);
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.debug("Request error for client {}: {}", clientId, e.getMessage());
                }
            }
        } finally {
            client.close();
        }
    }

    private BenchmarkResult calculateResult(long totalTimeNanos) {
        int total = successCount.get() + failCount.get();
        double durationSeconds = totalTimeNanos / 1_000_000_000.0;
        double throughput = total / durationSeconds;
        
        // 计算延迟统计
        List<Long> sortedLatencies = new ArrayList<>(latencies);
        sortedLatencies.sort(Long::compareTo);
        
        long minLatency = 0, maxLatency = 0, avgLatency = 0, p50Latency = 0, p95Latency = 0, p99Latency = 0;
        
        if (!sortedLatencies.isEmpty()) {
            LongSummaryStatistics stats = sortedLatencies.stream()
                    .mapToLong(Long::longValue)
                    .summaryStatistics();
            
            minLatency = stats.getMin() / 1_000_000; // 转换为毫秒
            maxLatency = stats.getMax() / 1_000_000;
            avgLatency = (long) (stats.getAverage() / 1_000_000);
            
            int size = sortedLatencies.size();
            p50Latency = sortedLatencies.get(size / 2) / 1_000_000;
            p95Latency = sortedLatencies.get((int) (size * 0.95)) / 1_000_000;
            p99Latency = sortedLatencies.get((int) (size * 0.99)) / 1_000_000;
        }
        
        return new BenchmarkResult(
                concurrentClients,
                requestsPerClient,
                total,
                successCount.get(),
                failCount.get(),
                durationSeconds,
                throughput,
                minLatency,
                maxLatency,
                avgLatency,
                p50Latency,
                p95Latency,
                p99Latency
        );
    }

    /**
     * 压测结果
     */
    public static class BenchmarkResult {
        public final int concurrentClients;
        public final int requestsPerClient;
        public final int totalRequests;
        public final int successCount;
        public final int failCount;
        public final double durationSeconds;
        public final double throughput; // 请求/秒
        public final long minLatencyMs;
        public final long maxLatencyMs;
        public final long avgLatencyMs;
        public final long p50LatencyMs;
        public final long p95LatencyMs;
        public final long p99LatencyMs;

        public BenchmarkResult(int concurrentClients, int requestsPerClient, int totalRequests,
                              int successCount, int failCount, double durationSeconds, double throughput,
                              long minLatencyMs, long maxLatencyMs, long avgLatencyMs,
                              long p50LatencyMs, long p95LatencyMs, long p99LatencyMs) {
            this.concurrentClients = concurrentClients;
            this.requestsPerClient = requestsPerClient;
            this.totalRequests = totalRequests;
            this.successCount = successCount;
            this.failCount = failCount;
            this.durationSeconds = durationSeconds;
            this.throughput = throughput;
            this.minLatencyMs = minLatencyMs;
            this.maxLatencyMs = maxLatencyMs;
            this.avgLatencyMs = avgLatencyMs;
            this.p50LatencyMs = p50LatencyMs;
            this.p95LatencyMs = p95LatencyMs;
            this.p99LatencyMs = p99LatencyMs;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n");
            sb.append("╔══════════════════════════════════════════════════════════════╗\n");
            sb.append("║               JSON-RPC Benchmark Results                     ║\n");
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            sb.append(String.format("║ Concurrent Clients:     %10d                           ║\n", concurrentClients));
            sb.append(String.format("║ Requests Per Client:    %10d                           ║\n", requestsPerClient));
            sb.append(String.format("║ Total Requests:         %10d                           ║\n", totalRequests));
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            sb.append(String.format("║ Success Count:          %10d                           ║\n", successCount));
            sb.append(String.format("║ Fail Count:             %10d                           ║\n", failCount));
            sb.append(String.format("║ Success Rate:           %9.2f%%                           ║\n", 
                    totalRequests > 0 ? (successCount * 100.0 / totalRequests) : 0));
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            sb.append(String.format("║ Duration:               %10.2f seconds                   ║\n", durationSeconds));
            sb.append(String.format("║ Throughput:             %10.2f requests/sec              ║\n", throughput));
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            sb.append("║                    Latency Statistics                        ║\n");
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            sb.append(String.format("║ Min Latency:            %10d ms                         ║\n", minLatencyMs));
            sb.append(String.format("║ Max Latency:            %10d ms                         ║\n", maxLatencyMs));
            sb.append(String.format("║ Avg Latency:            %10d ms                         ║\n", avgLatencyMs));
            sb.append(String.format("║ P50 Latency:            %10d ms                         ║\n", p50LatencyMs));
            sb.append(String.format("║ P95 Latency:            %10d ms                         ║\n", p95LatencyMs));
            sb.append(String.format("║ P99 Latency:            %10d ms                         ║\n", p99LatencyMs));
            sb.append("╚══════════════════════════════════════════════════════════════╝\n");
            return sb.toString();
        }
        
        /**
         * 输出 CSV 格式
         */
        public String toCsv() {
            return String.format("%d,%d,%d,%d,%d,%.2f,%.2f,%d,%d,%d,%d,%d,%d",
                    concurrentClients, requestsPerClient, totalRequests,
                    successCount, failCount, durationSeconds, throughput,
                    minLatencyMs, maxLatencyMs, avgLatencyMs,
                    p50LatencyMs, p95LatencyMs, p99LatencyMs);
        }
        
        public static String csvHeader() {
            return "concurrent_clients,requests_per_client,total_requests," +
                   "success_count,fail_count,duration_sec,throughput," +
                   "min_latency_ms,max_latency_ms,avg_latency_ms," +
                   "p50_latency_ms,p95_latency_ms,p99_latency_ms";
        }
    }

    /**
     * 命令行入口
     */
    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 18080;
        int clients = 10;
        int requests = 100;
        
        // 解析命令行参数
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                case "--host":
                    host = args[++i];
                    break;
                case "-p":
                case "--port":
                    port = Integer.parseInt(args[++i]);
                    break;
                case "-c":
                case "--clients":
                    clients = Integer.parseInt(args[++i]);
                    break;
                case "-n":
                case "--requests":
                    requests = Integer.parseInt(args[++i]);
                    break;
                case "--help":
                    printUsage();
                    return;
            }
        }
        
        log.info("JSON-RPC Benchmark Tool");
        log.info("========================");
        
        try {
            JsonRpcBenchmark benchmark = new JsonRpcBenchmark(host, port, clients, requests);
            BenchmarkResult result = benchmark.run();
            System.out.println(result);
        } catch (Exception e) {
            log.error("Benchmark failed", e);
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.out.println("JSON-RPC Benchmark Tool");
        System.out.println("Usage: java -cp <classpath> com.lixq.jsonrpc.benchmark.JsonRpcBenchmark [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h, --host <host>       Target host (default: 127.0.0.1)");
        System.out.println("  -p, --port <port>       Target port (default: 18080)");
        System.out.println("  -c, --clients <num>     Number of concurrent clients (default: 10)");
        System.out.println("  -n, --requests <num>    Number of requests per client (default: 100)");
        System.out.println("  --help                  Show this help message");
    }
}
