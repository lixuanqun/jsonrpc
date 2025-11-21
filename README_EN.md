# JSON-RPC for Java

A high-performance, easy-to-use JSON-RPC framework based on Netty, supporting both native Java and Spring Boot usage.

## Features

### Core Features
- ✅ **Netty-based**: Implemented using the high-performance Netty networking framework
- ✅ **Protocol Support**: Supports multiple protocols including TCP, HTTP, WebSocket (currently TCP is the main focus)
- ✅ **JSON-RPC 2.0**: Fully compliant with JSON-RPC 2.0 specification
- ✅ **Batch Requests**: Supports batch JSON-RPC requests
- ✅ **Async Calls**: Client supports asynchronous calls with CompletableFuture
- ✅ **Auto Registration**: Automatic service discovery and registration in Spring Boot environment

### Spring Boot Integration
- ✅ **Out of the Box**: Zero configuration required via auto-configuration
- ✅ **Annotation-driven**: Use `@JsonRpcService` annotation to identify service classes
- ✅ **Configuration Properties**: Support configuration via `application.yml`
- ✅ **Service Scanning**: Automatically scans and registers annotated service methods

## Dependencies

### Core Dependencies
- **Netty 4.1.94.Final**: Network communication framework
- **Jackson 2.13.4**: JSON serialization and deserialization
  - jackson-databind
  - jackson-core
  - jackson-annotations
- **SLF4J 1.7.36**: Logging facade

### Optional Dependencies (Spring Boot Integration)
- **Spring Boot 2.7.14**: Auto-configuration support
  - spring-boot-autoconfigure
  - spring-boot-configuration-processor
- **Spring 5.3.31**:
  - spring-context

> Note: Spring Boot related dependencies are optional and only needed in Spring Boot projects.

## Usage

### Method 1: Native Java Usage

#### 1. Create Service Class

```java
import com.lixq.jsonrpc.core.JsonRpcMethod;

public class CalculatorService {
    
    @JsonRpcMethod("add")
    public Integer add(Integer a, Integer b) {
        return a + b;
    }
    
    @JsonRpcMethod("subtract")
    public Integer subtract(Integer a, Integer b) {
        return a - b;
    }
    
    @JsonRpcMethod("hello")
    public String hello(String name) {
        return "Hello, " + name + "!";
    }
}
```

#### 2. Start Server

```java
import com.lixq.jsonrpc.JsonRpcServer;
import com.lixq.jsonrpc.core.JsonRpcProtocol;

public class ServerExample {
    public static void main(String[] args) {
        // Create server
        JsonRpcServer server = new JsonRpcServer(
            JsonRpcProtocol.TCP,  // Protocol type
            "0.0.0.0",            // Listen address
            18080                 // Port
        );
        
        // Register service
        server.registerService(new CalculatorService());
        
        // Start server asynchronously (non-blocking)
        server.startAsync();
        
        // Keep program running
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            server.stop();
        }
    }
}
```

#### 3. Client Invocation

```java
import com.lixq.jsonrpc.JsonRpcClient;
import com.lixq.jsonrpc.core.RpcResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ClientExample {
    public static void main(String[] args) throws Exception {
        // Create client
        JsonRpcClient client = new JsonRpcClient("127.0.0.1", 18080);
        
        // Connect to server
        client.connect().get(5, TimeUnit.SECONDS);
        
        // Send request (async)
        CompletableFuture<RpcResponse> future = client.sendRequest("add", new Object[]{10, 20});
        
        // Handle response
        future.thenAccept(response -> {
            if (response.getError() != null) {
                System.err.println("Error: " + response.getError().getMessage());
            } else {
                System.out.println("Result: " + response.getResult());
            }
        });
        
        // Wait for response
        Thread.sleep(2000);
        
        // Close client
        client.close();
    }
}
```

### Method 2: Spring Boot Usage (Recommended)

#### 1. Add Dependency

Add the following to your Spring Boot project's `pom.xml`:

```xml
<dependency>
    <groupId>com.lixq.jsonrpc</groupId>
    <artifactId>jsonrpc</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

#### 2. Configure application.yml

```yaml
jsonrpc:
  enabled: true              # Enable JSON-RPC, default: true
  server:
    enabled: true            # Enable server, default: true
    protocol: TCP            # Protocol type: TCP, HTTP, WS
    host: 0.0.0.0           # Server address
    port: 18080             # Server port
  client:
    enabled: false          # Enable client, default: false
    host: 127.0.0.1        # Server address
    port: 18080            # Server port
    connect-timeout: 5     # Connection timeout (seconds)
```

#### 3. Create Service Class

```java
import com.lixq.jsonrpc.spring.JsonRpcService;
import com.lixq.jsonrpc.core.JsonRpcMethod;
import org.springframework.stereotype.Service;

@JsonRpcService  // Mark as JSON-RPC service
@Service
public class CalculatorService {
    
    @JsonRpcMethod("add")
    public Integer add(Integer a, Integer b) {
        return a + b;
    }
    
    @JsonRpcMethod("subtract")
    public Integer subtract(Integer a, Integer b) {
        return a - b;
    }
}
```

#### 4. Inject Client (Optional)

If you need to use the client:

```java
import com.lixq.jsonrpc.JsonRpcClient;
import com.lixq.jsonrpc.core.RpcResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;

@Component
public class CalculatorClient {
    
    @Autowired
    private JsonRpcClient jsonRpcClient;
    
    public CompletableFuture<RpcResponse> add(Integer a, Integer b) {
        return jsonRpcClient.sendRequest("add", new Object[]{a, b});
    }
}
```

#### 5. Start Application

Simply start your Spring Boot application, and the JSON-RPC server will start automatically. All services annotated with `@JsonRpcService` will be automatically registered.

## Development Guide

### Project Structure

```
src/main/java/com/lixq/jsonrpc/
├── core/                    # Core classes
│   ├── JsonRpcProtocol.java    # Protocol enum
│   ├── JsonRpcMethod.java      # Method annotation
│   ├── RpcRequest.java         # Request object
│   ├── RpcResponse.java        # Response object
│   └── RpcErrorEnums.java      # Error enum
├── JsonRpcServer.java       # Server
├── JsonRpcClient.java       # Client
├── JsonRpcServerHandler.java # Server handler
├── JsonRpcClientHandler.java # Client handler
├── JsonRpcServiceRegistry.java # Service registry
└── spring/                  # Spring Boot integration
    ├── JsonRpcAutoConfiguration.java # Auto-configuration
    ├── JsonRpcProperties.java        # Configuration properties
    ├── JsonRpcService.java           # Service annotation
    └── JsonRpcServiceScanner.java    # Service scanner
```

### Extending Protocol Support

To support new protocols (e.g., HTTP, WebSocket):

1. **Modify `JsonRpcServer`**: Add new protocol handling logic
2. **Create New Handler**: Create corresponding handlers for different protocols
3. **Implement Protocol-Specific Codecs**: Add appropriate codecs in the Netty Pipeline

Example: Supporting HTTP Protocol

```java
// Add HTTP support in JsonRpcServer.java
if (JsonRpcProtocol.HTTP.equals(protocol)) {
    // Use HttpServerCodec and other HTTP-related codecs
    pipeline.addLast(new HttpServerCodec());
    pipeline.addLast(new HttpObjectAggregator(65536));
    pipeline.addLast(new JsonRpcHttpServerHandler(serviceRegistry));
}
```

### Custom Handlers

You can extend `JsonRpcServerHandler` or `JsonRpcClientHandler` to customize handling logic:

```java
public class CustomJsonRpcServerHandler extends JsonRpcServerHandler {
    public CustomJsonRpcServerHandler(JsonRpcServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // Custom handling logic
        // e.g., add authentication, logging, etc.
        super.channelRead(ctx, msg);
    }
}
```

### Extending Service Registration

You can extend service registration by implementing custom service discovery mechanisms:

```java
// Implement custom service discovery
public class CustomServiceRegistry extends JsonRpcServiceRegistry {
    // Add service discovery logic
    public void discoverServices() {
        // Discover services from configuration center, registry, etc.
    }
}
```

### Adding Middleware Support

You can add middleware mechanisms in handlers to support interceptors, filters, etc.:

```java
public interface JsonRpcInterceptor {
    boolean preHandle(RpcRequest request);
    void postHandle(RpcRequest request, RpcResponse response);
}

// Use in JsonRpcServerHandler
private List<JsonRpcInterceptor> interceptors;

public RpcResponse handleRequest(RpcRequest request) {
    // Execute pre-interceptors
    for (JsonRpcInterceptor interceptor : interceptors) {
        if (!interceptor.preHandle(request)) {
            return createErrorResponse(...);
        }
    }
    
    // Process request
    RpcResponse response = ...;
    
    // Execute post-interceptors
    for (JsonRpcInterceptor interceptor : interceptors) {
        interceptor.postHandle(request, response);
    }
    
    return response;
}
```

### Performance Optimization

1. **Connection Pool**: Implement connection pooling for clients to reuse connections
2. **Thread Pool**: Use custom thread pools for servers
3. **Serialization Optimization**: Use more efficient serialization frameworks (e.g., Kryo, Protobuf)
4. **Batch Processing**: Optimize batch request processing performance

### Monitoring and Diagnostics

You can add monitoring capabilities:

```java
public class JsonRpcMetrics {
    private final Counter requestCounter;
    private final Histogram requestLatency;
    
    public void recordRequest(String method, long latency) {
        requestCounter.increment();
        requestLatency.update(latency);
    }
}
```

## Building the Project

```bash
# Compile the project
mvn clean compile

# Package the project
mvn clean package

# Install to local Maven repository
mvn clean install
```

## License

See the [LICENSE](LICENSE) file for details.

## Contributing

Issues and Pull Requests are welcome!

---

**中文版本**: 参见 [README.md](README.md)

