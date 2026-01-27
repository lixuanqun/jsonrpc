# JSON-RPC for Java

[← メインに戻る](../README.md) | [English](README_EN.md) | [中文](README_ZH.md) | **日本語** | [Deutsch](README_DE.md) | [हिन्दी](README_HI.md)

---

Nettyベースの高性能で使いやすいJSON-RPC 2.0フレームワーク。ネイティブJavaとSpring Bootの両方をサポート。

## 特徴

### コア機能
- ✅ **Nettyベース**：高性能ネットワーキングフレームワーク
- ✅ **プロトコルサポート**：TCP、HTTP、WebSocket
- ✅ **JSON-RPC 2.0**：仕様に完全準拠
- ✅ **バッチリクエスト**：ネイティブバッチリクエストサポート
- ✅ **非同期呼び出し**：CompletableFutureベースの非同期API
- ✅ **自動登録**：Spring Bootでの自動サービス検出

### Spring Boot統合
- ✅ **ゼロ設定**：自動設定サポート
- ✅ **アノテーション駆動**：`@JsonRpcService`アノテーション
- ✅ **プロパティ**：`application.yml`での設定

## インストール

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

## クイックスタート

### 1. サービスクラスを作成

```java
import com.lixq.jsonrpc.core.JsonRpcMethod;

public class CalculatorService {
    
    @JsonRpcMethod("add")
    public Integer add(Integer a, Integer b) {
        return a + b;
    }
    
    @JsonRpcMethod("hello")
    public String hello(String name) {
        return "こんにちは、" + name + "！";
    }
}
```

### 2. サーバーを起動

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
        
        // 実行を維持
        Thread.sleep(Long.MAX_VALUE);
    }
}
```

### 3. クライアント呼び出し

```java
import com.lixq.jsonrpc.JsonRpcClient;
import com.lixq.jsonrpc.core.RpcResponse;

public class ClientExample {
    public static void main(String[] args) throws Exception {
        JsonRpcClient client = new JsonRpcClient("127.0.0.1", 18080);
        client.connect().get(5, TimeUnit.SECONDS);
        
        // 非同期呼び出し
        CompletableFuture<RpcResponse> future = client.sendRequest("add", new Object[]{10, 20});
        
        future.thenAccept(response -> {
            System.out.println("結果: " + response.getResult()); // 30
        });
        
        Thread.sleep(2000);
        client.close();
    }
}
```

## Spring Bootでの使用

### 1. application.ymlを設定

```yaml
jsonrpc:
  enabled: true
  server:
    enabled: true
    protocol: TCP
    host: 0.0.0.0
    port: 18080
```

### 2. サービスを作成

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

### 3. アプリケーションを起動

Spring Bootアプリケーションを起動するだけです。JSON-RPCサーバーは自動的に起動します。

## ベンチマーク結果

| シナリオ | 同時接続 | リクエスト | スループット | P50 | P99 |
|----------|----------|------------|--------------|-----|-----|
| 軽負荷 | 10 | 1,000 | 1,771 QPS | 1ms | 73ms |
| 中負荷 | 50 | 10,000 | 7,229 QPS | 3ms | 32ms |
| 高負荷 | 100 | 50,000 | 13,915 QPS | 4ms | 21ms |
| 極限 | 200 | 200,000 | 31,808 QPS | 3ms | 26ms |

## プロジェクト構造

```
src/main/java/com/lixq/jsonrpc/
├── core/                    # コアクラス
│   ├── JsonRpcMethod.java   # メソッドアノテーション
│   ├── RpcRequest.java      # リクエストオブジェクト
│   ├── RpcResponse.java     # レスポンスオブジェクト
│   └── RpcErrorEnums.java   # エラーコード
├── JsonRpcServer.java       # サーバー
├── JsonRpcClient.java       # クライアント
└── spring/                  # Spring Boot統合
    ├── JsonRpcAutoConfiguration.java
    ├── JsonRpcService.java
    └── JsonRpcServiceScanner.java
```

## ビルド

```bash
mvn clean compile      # コンパイル
mvn clean package      # パッケージ
mvn clean install      # ローカルリポジトリにインストール
```

## ライセンス

[MIT License](../LICENSE)

## 貢献

IssueやPull Requestを歓迎します！
