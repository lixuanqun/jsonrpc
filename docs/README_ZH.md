# JSON-RPC for Java

[← 返回主页](../README.md) | [English](README_EN.md) | **中文** | [日本語](README_JA.md) | [Deutsch](README_DE.md) | [हिन्दी](README_HI.md)

---

基于 Netty 实现的高性能、易于使用的 JSON-RPC 2.0 框架，支持原生 Java 和 Spring Boot。

## 功能特性

### 核心特性
- ✅ **基于 Netty**：高性能网络通信框架
- ✅ **协议支持**：TCP、HTTP、WebSocket
- ✅ **JSON-RPC 2.0**：完全符合规范
- ✅ **批量请求**：原生支持批量请求
- ✅ **异步调用**：基于 CompletableFuture 的异步 API
- ✅ **自动注册**：Spring Boot 环境下自动发现和注册服务

### Spring Boot 集成
- ✅ **零配置**：开箱即用的自动配置
- ✅ **注解驱动**：使用 `@JsonRpcService` 注解
- ✅ **配置属性**：通过 `application.yml` 配置

## 安装

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

## 快速开始

### 1. 创建服务类

```java
import com.lixq.jsonrpc.core.JsonRpcMethod;

public class CalculatorService {
    
    @JsonRpcMethod("add")
    public Integer add(Integer a, Integer b) {
        return a + b;
    }
    
    @JsonRpcMethod("hello")
    public String hello(String name) {
        return "你好, " + name + "!";
    }
}
```

### 2. 启动服务器

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
        
        // 保持运行
        Thread.sleep(Long.MAX_VALUE);
    }
}
```

### 3. 客户端调用

```java
import com.lixq.jsonrpc.JsonRpcClient;
import com.lixq.jsonrpc.core.RpcResponse;

public class ClientExample {
    public static void main(String[] args) throws Exception {
        JsonRpcClient client = new JsonRpcClient("127.0.0.1", 18080);
        client.connect().get(5, TimeUnit.SECONDS);
        
        // 异步调用
        CompletableFuture<RpcResponse> future = client.sendRequest("add", new Object[]{10, 20});
        
        future.thenAccept(response -> {
            System.out.println("结果: " + response.getResult()); // 30
        });
        
        Thread.sleep(2000);
        client.close();
    }
}
```

## Spring Boot 使用

### 1. 配置 application.yml

```yaml
jsonrpc:
  enabled: true
  server:
    enabled: true
    protocol: TCP
    host: 0.0.0.0
    port: 18080
```

### 2. 创建服务

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

### 3. 启动应用

直接启动 Spring Boot 应用，JSON-RPC 服务器会自动启动。

## 压测基准

| 场景 | 并发 | 请求数 | 吞吐量 | P50 | P99 |
|------|------|--------|--------|-----|-----|
| 轻负载 | 10 | 1,000 | 1,771 QPS | 1ms | 73ms |
| 中负载 | 50 | 10,000 | 7,229 QPS | 3ms | 32ms |
| 高负载 | 100 | 50,000 | 13,915 QPS | 4ms | 21ms |
| 极限 | 200 | 200,000 | 31,808 QPS | 3ms | 26ms |

## 项目结构

```
src/main/java/com/lixq/jsonrpc/
├── core/                    # 核心类
│   ├── JsonRpcMethod.java   # 方法注解
│   ├── RpcRequest.java      # 请求对象
│   ├── RpcResponse.java     # 响应对象
│   └── RpcErrorEnums.java   # 错误码枚举
├── JsonRpcServer.java       # 服务器
├── JsonRpcClient.java       # 客户端
└── spring/                  # Spring Boot 集成
    ├── JsonRpcAutoConfiguration.java
    ├── JsonRpcService.java
    └── JsonRpcServiceScanner.java
```

## 构建项目

```bash
mvn clean compile      # 编译
mvn clean package      # 打包
mvn clean install      # 安装到本地仓库
```

## 许可证

[MIT License](../LICENSE)

## 贡献

欢迎提交 Issue 和 Pull Request！
