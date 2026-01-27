package com.lixq.jsonrpc.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lixq.jsonrpc.JsonRpcClient;
import com.lixq.jsonrpc.JsonRpcServer;
import com.lixq.jsonrpc.core.JsonRpcProtocol;
import com.lixq.jsonrpc.core.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class JsonRpcBenchmark {
    private static final Logger log = LoggerFactory.getLogger(JsonRpcBenchmark.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        Config config = Config.fromArgs(args);
        JsonRpcServer server = new JsonRpcServer(JsonRpcProtocol.TCP, config.host, config.port);
        server.registerService(new JsonRpcService());
        server.startAsync();

        Thread.sleep(config.serverWarmupMs);

        BenchmarkResult result = runBenchmark(config);
        server.stop();

        printSummary(config, result);
    }

    private static BenchmarkResult runBenchmark(Config config) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(config.connections);
        List<Future<ConnectionResult>> futures = new ArrayList<>();
        long start = System.nanoTime();
        for (int i = 0; i < config.connections; i++) {
            futures.add(executor.submit(new ConnectionTask(config)));
        }

        int totalSent = 0;
        int totalOk = 0;
        int totalErrors = 0;
        List<Long> allLatencies = new ArrayList<>();

        for (Future<ConnectionResult> future : futures) {
            try {
                ConnectionResult result = future.get();
                totalSent += result.sent;
                totalOk += result.ok;
                totalErrors += result.errors;
                allLatencies.addAll(result.latenciesNanos);
            } catch (ExecutionException e) {
                totalErrors += config.requestsPerConnection;
                log.error("Connection task failed", e.getCause());
            }
        }

        long elapsedNanos = System.nanoTime() - start;
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        return new BenchmarkResult(totalSent, totalOk, totalErrors, allLatencies, elapsedNanos);
    }

    private static void printSummary(Config config, BenchmarkResult result) {
        double elapsedSeconds = result.elapsedNanos / 1_000_000_000.0;
        System.out.println("=== JSON-RPC Benchmark Summary ===");
        System.out.println("Target: " + config.host + ":" + config.port + "  Method: " + config.method);
        System.out.println("Connections: " + config.connections);
        System.out.println("Requests per connection: " + config.requestsPerConnection);
        System.out.println("Elapsed: " + String.format("%.3f", elapsedSeconds) + "s");
        System.out.println("Sent: " + result.sent + "  OK: " + result.ok + "  Errors: " + result.errors);

        if (!result.latenciesNanos.isEmpty()) {
            List<Long> sorted = new ArrayList<>(result.latenciesNanos);
            Collections.sort(sorted);
            long min = sorted.get(0);
            long max = sorted.get(sorted.size() - 1);
            long p50 = percentile(sorted, 0.50);
            long p95 = percentile(sorted, 0.95);
            long p99 = percentile(sorted, 0.99);
            double avg = average(sorted);

            System.out.println("--- Latency (ms) ---");
            System.out.println("min/avg/median/max: "
                + formatMillis(min) + " / "
                + String.format("%.2f", avg / 1_000_000.0) + " / "
                + formatMillis(p50) + " / "
                + formatMillis(max));
            System.out.println("p95/p99: " + formatMillis(p95) + " / " + formatMillis(p99));
        }

        if (elapsedSeconds > 0) {
            double throughput = result.ok / elapsedSeconds;
            System.out.println("Throughput: " + String.format("%.2f", throughput) + " req/s");
        }
    }

    private static long percentile(List<Long> sorted, double percentile) {
        if (sorted.isEmpty()) {
            return 0L;
        }
        double index = percentile * (sorted.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) {
            return sorted.get(lower);
        }
        double weight = index - lower;
        return (long) (sorted.get(lower) + (sorted.get(upper) - sorted.get(lower)) * weight);
    }

    private static double average(List<Long> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        long sum = 0L;
        for (Long value : values) {
            sum += value;
        }
        return sum / (double) values.size();
    }

    private static String formatMillis(long nanos) {
        return String.format("%.2f", nanos / 1_000_000.0);
    }

    private static class ConnectionTask implements Callable<ConnectionResult> {
        private final Config config;

        private ConnectionTask(Config config) {
            this.config = config;
        }

        @Override
        public ConnectionResult call() throws Exception {
            JsonRpcClient client = new JsonRpcClient(config.host, config.port);
            client.connect().get(config.connectTimeoutMs, TimeUnit.MILLISECONDS);

            int sent = 0;
            int ok = 0;
            int errors = 0;
            List<Long> latencies = new ArrayList<>();

            try {
                for (int i = 0; i < config.requestsPerConnection; i++) {
                    sent++;
                    long start = System.nanoTime();
                    CompletableFuture<RpcResponse> future = client.sendRequest(config.method, config.params);
                    RpcResponse response;
                    try {
                        response = future.get(config.requestTimeoutMs, TimeUnit.MILLISECONDS);
                    } catch (TimeoutException e) {
                        errors++;
                        continue;
                    }
                    long elapsed = System.nanoTime() - start;
                    latencies.add(elapsed);

                    if (response == null || response.getError() != null) {
                        errors++;
                        continue;
                    }

                    if (config.expectedResult != null
                        && !resultMatches(config.expectedResult, response.getResult())) {
                        errors++;
                        continue;
                    }

                    ok++;
                }
            } finally {
                client.close();
            }

            return new ConnectionResult(sent, ok, errors, latencies);
        }

        private boolean resultMatches(Object expected, Object actual) {
            if (expected == null && actual == null) {
                return true;
            }
            if (expected == null || actual == null) {
                return false;
            }
            if (expected instanceof Number && actual instanceof Number) {
                double a = ((Number) expected).doubleValue();
                double b = ((Number) actual).doubleValue();
                return Double.compare(a, b) == 0;
            }
            return expected.equals(actual);
        }
    }

    private static class ConnectionResult {
        private final int sent;
        private final int ok;
        private final int errors;
        private final List<Long> latenciesNanos;

        private ConnectionResult(int sent, int ok, int errors, List<Long> latenciesNanos) {
            this.sent = sent;
            this.ok = ok;
            this.errors = errors;
            this.latenciesNanos = latenciesNanos;
        }
    }

    private static class BenchmarkResult {
        private final int sent;
        private final int ok;
        private final int errors;
        private final List<Long> latenciesNanos;
        private final long elapsedNanos;

        private BenchmarkResult(int sent, int ok, int errors, List<Long> latenciesNanos, long elapsedNanos) {
            this.sent = sent;
            this.ok = ok;
            this.errors = errors;
            this.latenciesNanos = latenciesNanos;
            this.elapsedNanos = elapsedNanos;
        }
    }

    private static class Config {
        private final String host;
        private final int port;
        private final String method;
        private final Object params;
        private final Object expectedResult;
        private final int connections;
        private final int requestsPerConnection;
        private final long requestTimeoutMs;
        private final long connectTimeoutMs;
        private final long serverWarmupMs;

        private Config(
            String host,
            int port,
            String method,
            Object params,
            Object expectedResult,
            int connections,
            int requestsPerConnection,
            long requestTimeoutMs,
            long connectTimeoutMs,
            long serverWarmupMs
        ) {
            this.host = host;
            this.port = port;
            this.method = method;
            this.params = params;
            this.expectedResult = expectedResult;
            this.connections = connections;
            this.requestsPerConnection = requestsPerConnection;
            this.requestTimeoutMs = requestTimeoutMs;
            this.connectTimeoutMs = connectTimeoutMs;
            this.serverWarmupMs = serverWarmupMs;
        }

        private static Config fromArgs(String[] args) throws Exception {
            Map<String, String> map = new HashMap<>();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.startsWith("--")) {
                    String key = arg.substring(2);
                    String value = "true";
                    if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                        value = args[++i];
                    }
                    map.put(key, value);
                }
            }

            String host = map.getOrDefault("host", "127.0.0.1");
            int port = Integer.parseInt(map.getOrDefault("port", "18080"));
            String method = map.getOrDefault("method", "add");
            String paramsJson = map.getOrDefault("params", "[1,2]");
            Object params = objectMapper.readValue(paramsJson, Object.class);

            Object expected = null;
            if (map.containsKey("expected")) {
                expected = objectMapper.readValue(map.get("expected"), Object.class);
            } else if ("add".equals(method)) {
                expected = 3;
            }

            int connections = Integer.parseInt(map.getOrDefault("connections", "10"));
            int requestsPerConnection = Integer.parseInt(map.getOrDefault("requests-per-connection", "100"));
            long requestTimeoutMs = Long.parseLong(map.getOrDefault("request-timeout-ms", "5000"));
            long connectTimeoutMs = Long.parseLong(map.getOrDefault("connect-timeout-ms", "5000"));
            long serverWarmupMs = Long.parseLong(map.getOrDefault("server-warmup-ms", "200"));

            return new Config(
                host,
                port,
                method,
                params,
                expected,
                connections,
                requestsPerConnection,
                requestTimeoutMs,
                connectTimeoutMs,
                serverWarmupMs
            );
        }
    }
}
