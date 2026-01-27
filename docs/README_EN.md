# JSON-RPC for Java

[← Back to Main](../README.md) | **English** | [中文](README_ZH.md) | [日本語](README_JA.md) | [Deutsch](README_DE.md) | [हिन्दी](README_HI.md)

---

A high-performance, easy-to-use JSON-RPC 2.0 framework based on Netty, supporting both native Java and Spring Boot.

## Features

### Core Features
- ✅ **Netty-based**: High-performance networking framework
- ✅ **Protocol Support**: TCP, HTTP, WebSocket
- ✅ **JSON-RPC 2.0**: Fully compliant with specification
- ✅ **Batch Requests**: Native batch request support
- ✅ **Async Calls**: CompletableFuture-based async API
- ✅ **Auto Registration**: Automatic service discovery in Spring Boot

### Spring Boot Integration
- ✅ **Zero Configuration**: Auto-configuration support
- ✅ **Annotation-driven**: `@JsonRpcService` annotation
- ✅ **Properties**: Configuration via `application.yml`

## Installation

### Maven

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/lixuanqun/jsonrpc</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.lixq.jsonrpc</groupId>
    <artifactId>jsonrpc</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

### 1. Create Service Class

```java
import com.lixq.jsonrpc.core.JsonRpcMethod;

public class CalculatorService {
    
    @JsonRpcMethod("add")
    public Integer add(Integer a, Integer b) {
        return a + b;
    }
    
    @JsonRpcMethod("hello")
    public String hello(String name) {
        return "Hello, " + name + "!";
    }
}
```

### 2. Start Server

```java
import com.lixq.jsonrpc.JsonRpcServer;
import com.lixq.jsonrpc.core.JsonRpcProtocol;

public class ServerExample {
    public static void main(String[] args) {
        JsonRpcServer server = new JsonRpcServer(
            JsonRpcProtocol.TCP,
            "0.0.0.0",
            18080
        );
        
        server.registerService(new CalculatorService());
        server.startAsync();
        
        // Keep running
        Thread.sleep(Long.MAX_VALUE);
    }
}
```

### 3. Client Call

```java
import com.lixq.jsonrpc.JsonRpcClient;
import com.lixq.jsonrpc.core.RpcResponse;

public class ClientExample {
    public static void main(String[] args) throws Exception {
        JsonRpcClient client = new JsonRpcClient("127.0.0.1", 18080);
        client.connect().get(5, TimeUnit.SECONDS);
        
        // Async call
        CompletableFuture<RpcResponse> future = client.sendRequest("add", new Object[]{10, 20});
        
        future.thenAccept(response -> {
            System.out.println("Result: " + response.getResult()); // 30
        });
        
        Thread.sleep(2000);
        client.close();
    }
}
```

## Spring Boot Usage

### 1. Configure application.yml

```yaml
jsonrpc:
  enabled: true
  server:
    enabled: true
    protocol: TCP
    host: 0.0.0.0
    port: 18080
```

### 2. Create Service

```java
import com.lixq.jsonrpc.spring.JsonRpcService;
import com.lixq.jsonrpc.core.JsonRpcMethod;

@JsonRpcService
@Service
public class CalculatorService {
    
    @JsonRpcMethod("add")
    public Integer add(Integer a, Integer b) {
        return a + b;
    }
}
```

### 3. Start Application

Just start your Spring Boot application. The JSON-RPC server will start automatically.

## Benchmark Results

| Scenario | Clients | Requests | Throughput | P50 | P99 |
|----------|---------|----------|------------|-----|-----|
| Light    | 10      | 1,000    | 1,771 QPS  | 1ms | 73ms |
| Medium   | 50      | 10,000   | 7,229 QPS  | 3ms | 32ms |
| Heavy    | 100     | 50,000   | 13,915 QPS | 4ms | 21ms |
| Extreme  | 200     | 200,000  | 31,808 QPS | 3ms | 26ms |

## Project Structure

```
src/main/java/com/lixq/jsonrpc/
├── core/                    # Core classes
│   ├── JsonRpcMethod.java   # Method annotation
│   ├── RpcRequest.java      # Request object
│   ├── RpcResponse.java     # Response object
│   └── RpcErrorEnums.java   # Error codes
├── JsonRpcServer.java       # Server
├── JsonRpcClient.java       # Client
└── spring/                  # Spring Boot integration
    ├── JsonRpcAutoConfiguration.java
    ├── JsonRpcService.java
    └── JsonRpcServiceScanner.java
```

## Building

```bash
mvn clean compile      # Compile
mvn clean package      # Package
mvn clean install      # Install to local repo
```

## License

[MIT License](../LICENSE)

## Contributing

Issues and Pull Requests are welcome!
