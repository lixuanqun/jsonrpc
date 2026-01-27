# JSON-RPC for Java

<div align="center">

[![Build](https://github.com/lixuanqun/jsonrpc/actions/workflows/maven-build.yml/badge.svg)](https://github.com/lixuanqun/jsonrpc/actions)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://www.oracle.com/java/)

**A high-performance JSON-RPC 2.0 framework based on Netty**

[English](docs/README_EN.md) | [中文](docs/README_ZH.md) | [日本語](docs/README_JA.md) | [Deutsch](docs/README_DE.md) | [हिन्दी](docs/README_HI.md)

</div>

---

## ✨ Features

- 🚀 **High Performance** - Built on Netty for excellent throughput (30,000+ QPS)
- 📋 **JSON-RPC 2.0** - Fully compliant with specification
- 🔌 **Multi-Protocol** - TCP, HTTP, WebSocket support
- ⚡ **Async** - CompletableFuture-based async API
- 🌱 **Spring Boot** - Auto-configuration support
- 📦 **Batch Requests** - Native batch request support

## 🚀 Quick Start

### Maven

```xml
<dependency>
    <groupId>com.lixq.jsonrpc</groupId>
    <artifactId>jsonrpc</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Server

```java
@JsonRpcMethod("hello")
public String hello(String name) {
    return "Hello, " + name + "!";
}

JsonRpcServer server = new JsonRpcServer(JsonRpcProtocol.TCP, "0.0.0.0", 18080);
server.registerService(new MyService());
server.startAsync();
```

### Client

```java
JsonRpcClient client = new JsonRpcClient("127.0.0.1", 18080);
client.connect().get();

RpcResponse response = client.sendRequest("hello", "World").get();
System.out.println(response.getResult()); // Hello, World!
```

## 📊 Benchmark

| Scenario | Requests | Throughput | P99 Latency |
|----------|----------|------------|-------------|
| Light    | 1,000    | 1,771 QPS  | 73 ms       |
| Medium   | 10,000   | 7,229 QPS  | 32 ms       |
| Heavy    | 50,000   | 13,915 QPS | 21 ms       |
| Extreme  | 200,000  | 31,808 QPS | 26 ms       |

## 📖 Documentation

| Language | Link |
|----------|------|
| English | [README_EN.md](docs/README_EN.md) |
| 简体中文 | [README_ZH.md](docs/README_ZH.md) |
| 日本語 | [README_JA.md](docs/README_JA.md) |
| Deutsch | [README_DE.md](docs/README_DE.md) |
| हिन्दी | [README_HI.md](docs/README_HI.md) |

## 📄 License

[MIT License](LICENSE)

---

<div align="center">
Made with ❤️ by <a href="https://github.com/lixuanqun">lixuanqun</a>
</div>
