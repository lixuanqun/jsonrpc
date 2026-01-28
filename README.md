# JSON-RPC for Java

[English](#english) | [中文 (Chinese)](#中文-chinese) | [日本語 (Japanese)](#日本語-japanese) | [Deutsch (German)](#deutsch-german) | [हिन्दी (Hindi)](#हिन्दी-hindi)

---

<a name="english"></a>
## 🇺🇸 English

A high-performance, easy-to-use JSON-RPC framework implemented based on Netty, supporting both native Java and Spring Boot usage modes.

### Features
- ✅ **Netty-based**: Built on the high-performance Netty network framework.
- ✅ **Protocol Support**: Supports TCP, HTTP, WebSocket (currently primarily TCP).
- ✅ **JSON-RPC 2.0**: Fully compliant with the JSON-RPC 2.0 specification.
- ✅ **Batch Requests**: Supports batch JSON-RPC requests.
- ✅ **Asynchronous Calls**: Client supports asynchronous calls returning `CompletableFuture`.
- ✅ **Auto-Registration**: Automatic service discovery and registration in Spring Boot environments.

### Usage (Spring Boot)
1. Add dependency:
```xml
<dependency>
    <groupId>com.lixq.jsonrpc</groupId>
    <artifactId>jsonrpc</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

2. Configure `application.yml`:
```yaml
jsonrpc:
  server:
    enabled: true
    protocol: HTTP
    port: 8080
```

3. Annotate service:
```java
@JsonRpcService
@Service
public class MyService {
    @JsonRpcMethod("hello")
    public String hello(String name) { return "Hello " + name; }
}
```

---

<a name="中文-chinese"></a>
## 🇨🇳 中文 (Chinese)

一个基于 Netty 实现的高性能、易于使用的 JSON-RPC 框架，支持原生 Java 和 Spring Boot 两种使用方式。

### 功能特性
- ✅ **基于 Netty**：使用高性能的 Netty 网络框架实现。
- ✅ **协议支持**：支持 TCP、HTTP、WebSocket 等多种协议。
- ✅ **JSON-RPC 2.0**：完全符合 JSON-RPC 2.0 规范。
- ✅ **批量请求**：支持批量 JSON-RPC 请求。
- ✅ **异步调用**：客户端支持异步调用，返回 `CompletableFuture`。
- ✅ **自动注册**：Spring Boot 环境下自动发现和注册服务。

### 使用方式 (Spring Boot)
1. 添加依赖：
```xml
<dependency>
    <groupId>com.lixq.jsonrpc</groupId>
    <artifactId>jsonrpc</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

2. 配置 `application.yml`：
```yaml
jsonrpc:
  server:
    enabled: true
    protocol: HTTP
    port: 8080
```

3. 编写服务：
```java
@JsonRpcService
@Service
public class MyService {
    @JsonRpcMethod("hello")
    public String hello(String name) { return "Hello " + name; }
}
```

---

<a name="日本語-japanese"></a>
## 🇯🇵 日本語 (Japanese)

Nettyに基づいた高性能で使いやすいJSON-RPCフレームワークです。ネイティブJavaとSpring Bootの両方の使用モードをサポートしています。

### 特徴
- ✅ **Nettyベース**: 高性能なNettyネットワークフレームワーク上に構築されています。
- ✅ **プロトコルサポート**: TCP、HTTP、WebSocketをサポートしています。
- ✅ **JSON-RPC 2.0**: JSON-RPC 2.0仕様に完全準拠しています。
- ✅ **バッチリクエスト**: JSON-RPCリクエストのバッチ処理をサポートしています。
- ✅ **非同期呼び出し**: クライアントは`CompletableFuture`を返す非同期呼び出しをサポートしています。
- ✅ **自動登録**: Spring Boot環境でのサービスの自動検出と登録に対応しています。

### 使用方法 (Spring Boot)
1. 依存関係を追加：
```xml
<dependency>
    <groupId>com.lixq.jsonrpc</groupId>
    <artifactId>jsonrpc</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

2. `application.yml`を設定：
```yaml
jsonrpc:
  server:
    enabled: true
    protocol: HTTP
    port: 8080
```

3. サービスを作成：
```java
@JsonRpcService
@Service
public class MyService {
    @JsonRpcMethod("hello")
    public String hello(String name) { return "Hello " + name; }
}
```

---

<a name="deutsch-german"></a>
## 🇩🇪 Deutsch (German)

Ein leistungsstarkes, einfach zu bedienendes JSON-RPC-Framework basierend auf Netty, das sowohl native Java- als auch Spring Boot-Nutzungsmodi unterstützt.

### Funktionen
- ✅ **Netty-basiert**: Aufgebaut auf dem leistungsstarken Netty-Netzwerkframework.
- ✅ **Protokollunterstützung**: Unterstützt TCP, HTTP, WebSocket.
- ✅ **JSON-RPC 2.0**: Vollständig konform mit der JSON-RPC 2.0-Spezifikation.
- ✅ **Batch-Anfragen**: Unterstützt Batch-JSON-RPC-Anfragen.
- ✅ **Asynchrone Aufrufe**: Client unterstützt asynchrone Aufrufe, die `CompletableFuture` zurückgeben.
- ✅ **Automatische Registrierung**: Automatische Serviceerkennung und -registrierung in Spring Boot-Umgebungen.

### Verwendung (Spring Boot)
1. Abhängigkeit hinzufügen:
```xml
<dependency>
    <groupId>com.lixq.jsonrpc</groupId>
    <artifactId>jsonrpc</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

2. `application.yml` konfigurieren:
```yaml
jsonrpc:
  server:
    enabled: true
    protocol: HTTP
    port: 8080
```

3. Service erstellen:
```java
@JsonRpcService
@Service
public class MyService {
    @JsonRpcMethod("hello")
    public String hello(String name) { return "Hello " + name; }
}
```

---

<a name="हिन्दी-hindi"></a>
## 🇮🇳 हिन्दी (Hindi)

नेटी (Netty) पर आधारित एक उच्च-प्रदर्शन और उपयोग में आसान JSON-RPC फ्रेमवर्क, जो नेटिव जावा और स्प्रिंग बूट (Spring Boot) दोनों मोड का समर्थन करता है।

### विशेषताएँ
- ✅ **नेटी (Netty) आधारित**: उच्च-प्रदर्शन वाले नेटी नेटवर्क फ्रेमवर्क पर बनाया गया है।
- ✅ **प्रोटोकॉल समर्थन**: TCP, HTTP, WebSocket का समर्थन करता है।
- ✅ **JSON-RPC 2.0**: JSON-RPC 2.0 विनिर्देश का पूरी तरह से पालन करता है।
- ✅ **बैच अनुरोध**: बैच JSON-RPC अनुरोधों का समर्थन करता है।
- ✅ **अतुल्यकालिक कॉल (Asynchronous Calls)**: क्लाइंट अतुल्यकालिक कॉल का समर्थन करता है जो `CompletableFuture` लौटाता है।
- ✅ **स्वचालित पंजीकरण**: स्प्रिंग बूट वातावरण में सेवाओं की स्वचालित खोज और पंजीकरण।

### उपयोग (Spring Boot)
1. निर्भरता (Dependency) जोड़ें:
```xml
<dependency>
    <groupId>com.lixq.jsonrpc</groupId>
    <artifactId>jsonrpc</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

2. `application.yml` कॉन्फ़िगर करें:
```yaml
jsonrpc:
  server:
    enabled: true
    protocol: HTTP
    port: 8080
```

3. सर्विस बनाएँ:
```java
@JsonRpcService
@Service
public class MyService {
    @JsonRpcMethod("hello")
    public String hello(String name) { return "Hello " + name; }
}
```
