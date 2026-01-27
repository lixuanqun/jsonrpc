**言語 / Languages**: [简体中文](README.md) | [English](README_EN.md) | [日本語](README_JA.md) | [Deutsch](README_DE.md) | [हिन्दी](README_HI.md)

# Java 向け JSON-RPC

Netty を基盤にした高性能で使いやすい JSON-RPC フレームワークです。ネイティブ Java と Spring Boot の両方に対応しています。

## 機能

### コア機能
- ✅ **Netty ベース**: 高性能な Netty ネットワークフレームワークを使用
- ✅ **プロトコル対応**: TCP / HTTP / WebSocket（現在は TCP が主）
- ✅ **JSON-RPC 2.0**: JSON-RPC 2.0 仕様に準拠
- ✅ **バッチリクエスト**: JSON-RPC のバッチ要求をサポート
- ✅ **非同期呼び出し**: クライアントは CompletableFuture による非同期呼び出しに対応
- ✅ **自動登録**: Spring Boot 環境でのサービス自動検出と登録

### Spring Boot 連携
- ✅ **すぐに使える**: 自動設定により追加設定が不要
- ✅ **アノテーション駆動**: `@JsonRpcService` でサービスクラスを識別
- ✅ **設定プロパティ**: `application.yml` での設定をサポート
- ✅ **サービススキャン**: アノテーション付きメソッドを自動登録

## 依存関係

### コア依存関係
- **Netty 4.1.115.Final**: 通信フレームワーク
- **Jackson 2.13.4**: JSON シリアライズ/デシリアライズ
  - jackson-databind
  - jackson-core
  - jackson-annotations
- **SLF4J 1.7.36**: ログファサード

### オプション依存（Spring Boot 連携）
- **Spring Boot 2.7.14**: 自動設定
  - spring-boot-autoconfigure
  - spring-boot-configuration-processor
- **Spring 5.3.31**:
  - spring-context

> 注: Spring Boot 関連依存は optional で、Spring Boot プロジェクトのみで必要です。

## 使い方

### 方法1: ネイティブ Java

#### 1. サービスクラスを作成

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

#### 2. サーバーを起動

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

#### 3. クライアント呼び出し

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

### 方法2: Spring Boot（推奨）

#### 1. 依存追加

Spring Boot プロジェクトの `pom.xml` に追加します:

```xml
<dependency>
    <groupId>com.lixq.jsonrpc</groupId>
    <artifactId>jsonrpc</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

#### 2. application.yml の設定

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

#### 3. サービスクラス作成

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

#### 4. クライアント注入（任意）

クライアントを利用する場合:

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

#### 5. アプリ起動

Spring Boot アプリを起動するだけで JSON-RPC サーバーが自動起動し、`@JsonRpcService` 付きのサービスが自動登録されます。

## 開発ガイド

### プロジェクト構成

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

### プロトコル拡張

HTTP などのプロトコル対応を追加する場合:

1. **`JsonRpcServer` を修正**: 新しいプロトコル処理を追加
2. **新しい Handler を作成**: プロトコル別の Handler を追加
3. **プロトコル用コーデックを追加**: Netty Pipeline に追加

HTTP 例:

```java
// Add HTTP support in JsonRpcServer.java
if (JsonRpcProtocol.HTTP.equals(protocol)) {
    // Use HttpServerCodec and other HTTP-related codecs
    pipeline.addLast(new HttpServerCodec());
    pipeline.addLast(new HttpObjectAggregator(65536));
    pipeline.addLast(new JsonRpcHttpServerHandler(serviceRegistry));
}
```

### カスタム Handler

`JsonRpcServerHandler` / `JsonRpcClientHandler` を継承して独自処理を追加できます:

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

### サービス登録の拡張

独自のサービス発見を追加できます:

```java
// Implement custom service discovery
public class CustomServiceRegistry extends JsonRpcServiceRegistry {
    // Add service discovery logic
    public void discoverServices() {
        // Discover services from configuration center, registry, etc.
    }
}
```

### ミドルウェア対応

Handler にインターセプターなどを追加できます:

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

### 性能最適化

1. **接続プール**: クライアント側で接続を再利用
2. **スレッドプール**: サーバーで専用スレッドプールを使用
3. **シリアライズ最適化**: Kryo/Protobuf など
4. **バッチ処理最適化**: バッチ処理性能向上

### 監視・診断

監視機能の追加例:

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

## ビルド

```bash
# Compile the project
mvn clean compile

# Package the project
mvn clean package

# Install to local Maven repository
mvn clean install
```

## 圧測スクリプト

TCP の JSON-RPC 圧測スクリプト（改行区切り JSON）:

```bash
python scripts/jsonrpc_load_test.py \
  --host 127.0.0.1 \
  --port 18080 \
  --method add \
  --params "[1,2]" \
  --connections 50 \
  --requests-per-connection 1000
```

通知（レスポンス不要）:

```bash
python scripts/jsonrpc_load_test.py \
  --host 127.0.0.1 \
  --port 18080 \
  --method hello \
  --params "[\"world\"]" \
  --connections 20 \
  --requests-per-connection 500 \
  --notification
```

> 注: 現在の TCP + 改行区切りプロトコルを対象としています。`JsonRpcServer` を先に起動してください。

## ライセンス

[LICENSE](LICENSE) を参照してください。

## コントリビュート

Issue と Pull Request を歓迎します。

---

**中国語版**: [README.md](README.md)
