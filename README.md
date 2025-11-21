
**English Version**: See [README_EN.md](README_EN.md)

# JSON-RPC for Java

一个基于 Netty 实现的高性能、易于使用的 JSON-RPC 框架，支持原生 Java 和 Spring Boot 两种使用方式。

## 功能特性

### 核心特性
- ✅ **基于 Netty**：使用高性能的 Netty 网络框架实现
- ✅ **协议支持**：支持 TCP、HTTP、WebSocket 等多种协议（当前主要支持 TCP）
- ✅ **JSON-RPC 2.0**：完全符合 JSON-RPC 2.0 规范
- ✅ **批量请求**：支持批量 JSON-RPC 请求
- ✅ **异步调用**：客户端支持异步调用，返回 CompletableFuture
- ✅ **自动注册**：Spring Boot 环境下自动发现和注册服务

### Spring Boot 集成
- ✅ **开箱即用**：通过自动配置，无需手动配置即可使用
- ✅ **注解驱动**：使用 `@JsonRpcService` 注解标识服务类
- ✅ **配置属性**：支持通过 `application.yml` 进行配置
- ✅ **服务扫描**：自动扫描并注册带注解的服务方法

## 依赖说明

### 核心依赖
- **Netty 4.1.94.Final**：网络通信框架
- **Jackson 2.13.4**：JSON 序列化和反序列化
  - jackson-databind
  - jackson-core
  - jackson-annotations
- **SLF4J 1.7.36**：日志门面

### 可选依赖（Spring Boot 集成）
- **Spring Boot 2.7.14**：自动配置支持
  - spring-boot-autoconfigure
  - spring-boot-configuration-processor
- **Spring 5.3.31**：
  - spring-context

> 注意：Spring Boot 相关依赖为可选依赖（optional），只在 Spring Boot 项目中需要。

## 使用方式

### 方式一：原生 Java 使用

#### 1. 创建服务类

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

#### 2. 启动服务器

```java
import com.lixq.jsonrpc.JsonRpcServer;
import com.lixq.jsonrpc.core.JsonRpcProtocol;

public class ServerExample {
    public static void main(String[] args) {
        // 创建服务器
        JsonRpcServer server = new JsonRpcServer(
            JsonRpcProtocol.TCP,  // 协议类型
            "0.0.0.0",            // 监听地址
            18080                 // 端口
        );
        
        // 注册服务
        server.registerService(new CalculatorService());
        
        // 异步启动服务器（不阻塞主线程）
        server.startAsync();
        
        // 保持程序运行
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            server.stop();
        }
    }
}
```

#### 3. 客户端调用

```java
import com.lixq.jsonrpc.JsonRpcClient;
import com.lixq.jsonrpc.core.RpcResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ClientExample {
    public static void main(String[] args) throws Exception {
        // 创建客户端
        JsonRpcClient client = new JsonRpcClient("127.0.0.1", 18080);
        
        // 连接到服务器
        client.connect().get(5, TimeUnit.SECONDS);
        
        // 发送请求（异步）
        CompletableFuture<RpcResponse> future = client.sendRequest("add", new Object[]{10, 20});
        
        // 处理响应
        future.thenAccept(response -> {
            if (response.getError() != null) {
                System.err.println("Error: " + response.getError().getMessage());
            } else {
                System.out.println("Result: " + response.getResult());
            }
        });
        
        // 等待响应
        Thread.sleep(2000);
        
        // 关闭客户端
        client.close();
    }
}
```

### 方式二：Spring Boot 使用（推荐）

#### 1. 添加依赖

在你的 Spring Boot 项目的 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.lixq.jsonrpc</groupId>
    <artifactId>jsonrpc</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

#### 2. 配置 application.yml

```yaml
jsonrpc:
  enabled: true              # 是否启用 JSON-RPC，默认为 true
  server:
    enabled: true            # 是否启用服务器，默认为 true
    protocol: TCP            # 协议类型: TCP, HTTP, WS
    host: 0.0.0.0           # 服务器地址
    port: 18080             # 服务器端口
  client:
    enabled: false          # 是否启用客户端，默认为 false
    host: 127.0.0.1        # 服务器地址
    port: 18080            # 服务器端口
    connect-timeout: 5     # 连接超时时间（秒）
```

#### 3. 创建服务类

```java
import com.lixq.jsonrpc.spring.JsonRpcService;
import com.lixq.jsonrpc.core.JsonRpcMethod;
import org.springframework.stereotype.Service;

@JsonRpcService  // 标识为 JSON-RPC 服务
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

#### 4. 注入客户端（可选）

如果需要使用客户端：

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

#### 5. 启动应用

直接启动 Spring Boot 应用，JSON-RPC 服务器会自动启动，所有带 `@JsonRpcService` 注解的服务都会被自动注册。

## 二次开发指南

### 项目结构

```
src/main/java/com/lixq/jsonrpc/
├── core/                    # 核心类
│   ├── JsonRpcProtocol.java    # 协议枚举
│   ├── JsonRpcMethod.java      # 方法注解
│   ├── RpcRequest.java         # 请求对象
│   ├── RpcResponse.java        # 响应对象
│   └── RpcErrorEnums.java      # 错误枚举
├── JsonRpcServer.java       # 服务器
├── JsonRpcClient.java       # 客户端
├── JsonRpcServerHandler.java # 服务器处理器
├── JsonRpcClientHandler.java # 客户端处理器
├── JsonRpcServiceRegistry.java # 服务注册表
└── spring/                  # Spring Boot 集成
    ├── JsonRpcAutoConfiguration.java # 自动配置
    ├── JsonRpcProperties.java        # 配置属性
    ├── JsonRpcService.java           # 服务注解
    └── JsonRpcServiceScanner.java    # 服务扫描器
```

### 扩展协议支持

如果需要支持新的协议（如 HTTP、WebSocket），可以：

1. **修改 `JsonRpcServer`**：添加新的协议处理逻辑
2. **创建新的 Handler**：针对不同协议创建对应的 Handler
3. **实现协议特定的编解码器**：在 Netty Pipeline 中添加相应的编解码器

示例：支持 HTTP 协议

```java
// 在 JsonRpcServer.java 中添加 HTTP 支持
if (JsonRpcProtocol.HTTP.equals(protocol)) {
    // 使用 HttpServerCodec 等 HTTP 相关的编解码器
    pipeline.addLast(new HttpServerCodec());
    pipeline.addLast(new HttpObjectAggregator(65536));
    pipeline.addLast(new JsonRpcHttpServerHandler(serviceRegistry));
}
```

### 自定义 Handler

可以继承 `JsonRpcServerHandler` 或 `JsonRpcClientHandler` 来自定义处理逻辑：

```java
public class CustomJsonRpcServerHandler extends JsonRpcServerHandler {
    public CustomJsonRpcServerHandler(JsonRpcServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // 自定义处理逻辑
        // 例如：添加认证、日志记录等
        super.channelRead(ctx, msg);
    }
}
```

### 扩展服务注册

可以通过实现自定义的服务发现机制来扩展服务注册：

```java
// 实现自定义的服务发现
public class CustomServiceRegistry extends JsonRpcServiceRegistry {
    // 添加服务发现逻辑
    public void discoverServices() {
        // 从配置中心、注册中心等发现服务
    }
}
```

### 添加中间件支持

可以在 Handler 中添加中间件机制，支持拦截器、过滤器等：

```java
public interface JsonRpcInterceptor {
    boolean preHandle(RpcRequest request);
    void postHandle(RpcRequest request, RpcResponse response);
}

// 在 JsonRpcServerHandler 中使用
private List<JsonRpcInterceptor> interceptors;

public RpcResponse handleRequest(RpcRequest request) {
    // 执行前置拦截器
    for (JsonRpcInterceptor interceptor : interceptors) {
        if (!interceptor.preHandle(request)) {
            return createErrorResponse(...);
        }
    }
    
    // 处理请求
    RpcResponse response = ...;
    
    // 执行后置拦截器
    for (JsonRpcInterceptor interceptor : interceptors) {
        interceptor.postHandle(request, response);
    }
    
    return response;
}
```

### 性能优化

1. **连接池**：为客户端实现连接池，复用连接
2. **线程池**：为服务器使用自定义线程池
3. **序列化优化**：使用更高效的序列化框架（如 Kryo、Protobuf）
4. **批量处理**：优化批量请求的处理性能

### 监控和诊断

可以添加监控功能：

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

## 构建项目

```bash
# 编译项目
mvn clean compile

# 打包项目
mvn clean package

# 安装到本地 Maven 仓库
mvn clean install
```

## 许可证

查看 [LICENSE](LICENSE) 文件了解详情。

## 贡献

欢迎提交 Issue 和 Pull Request！
---
