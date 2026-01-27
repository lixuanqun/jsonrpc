# JSON-RPC für Java

[← Zurück zur Hauptseite](../README.md) | [English](README_EN.md) | [中文](README_ZH.md) | [日本語](README_JA.md) | **Deutsch** | [हिन्दी](README_HI.md)

---

Ein hochleistungsfähiges, einfach zu verwendendes JSON-RPC 2.0 Framework basierend auf Netty, das sowohl natives Java als auch Spring Boot unterstützt.

## Funktionen

### Kernfunktionen
- ✅ **Netty-basiert**: Hochleistungsfähiges Netzwerk-Framework
- ✅ **Protokollunterstützung**: TCP, HTTP, WebSocket
- ✅ **JSON-RPC 2.0**: Vollständig spezifikationskonform
- ✅ **Batch-Anfragen**: Native Unterstützung für Batch-Anfragen
- ✅ **Asynchrone Aufrufe**: CompletableFuture-basierte asynchrone API
- ✅ **Automatische Registrierung**: Automatische Service-Erkennung in Spring Boot

### Spring Boot Integration
- ✅ **Null-Konfiguration**: Auto-Konfiguration Unterstützung
- ✅ **Annotationsgesteuert**: `@JsonRpcService` Annotation
- ✅ **Eigenschaften**: Konfiguration über `application.yml`

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

## Schnellstart

### 1. Service-Klasse erstellen

```java
import com.lixq.jsonrpc.core.JsonRpcMethod;

public class CalculatorService {
    
    @JsonRpcMethod("add")
    public Integer add(Integer a, Integer b) {
        return a + b;
    }
    
    @JsonRpcMethod("hello")
    public String hello(String name) {
        return "Hallo, " + name + "!";
    }
}
```

### 2. Server starten

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
        
        // Laufend halten
        Thread.sleep(Long.MAX_VALUE);
    }
}
```

### 3. Client-Aufruf

```java
import com.lixq.jsonrpc.JsonRpcClient;
import com.lixq.jsonrpc.core.RpcResponse;

public class ClientExample {
    public static void main(String[] args) throws Exception {
        JsonRpcClient client = new JsonRpcClient("127.0.0.1", 18080);
        client.connect().get(5, TimeUnit.SECONDS);
        
        // Asynchroner Aufruf
        CompletableFuture<RpcResponse> future = client.sendRequest("add", new Object[]{10, 20});
        
        future.thenAccept(response -> {
            System.out.println("Ergebnis: " + response.getResult()); // 30
        });
        
        Thread.sleep(2000);
        client.close();
    }
}
```

## Spring Boot Verwendung

### 1. application.yml konfigurieren

```yaml
jsonrpc:
  enabled: true
  server:
    enabled: true
    protocol: TCP
    host: 0.0.0.0
    port: 18080
```

### 2. Service erstellen

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

### 3. Anwendung starten

Starten Sie einfach Ihre Spring Boot Anwendung. Der JSON-RPC Server startet automatisch.

## Benchmark-Ergebnisse

| Szenario | Clients | Anfragen | Durchsatz | P50 | P99 |
|----------|---------|----------|-----------|-----|-----|
| Leicht   | 10      | 1.000    | 1.771 QPS | 1ms | 73ms |
| Mittel   | 50      | 10.000   | 7.229 QPS | 3ms | 32ms |
| Schwer   | 100     | 50.000   | 13.915 QPS| 4ms | 21ms |
| Extrem   | 200     | 200.000  | 31.808 QPS| 3ms | 26ms |

## Projektstruktur

```
src/main/java/com/lixq/jsonrpc/
├── core/                    # Kernklassen
│   ├── JsonRpcMethod.java   # Methoden-Annotation
│   ├── RpcRequest.java      # Anfrage-Objekt
│   ├── RpcResponse.java     # Antwort-Objekt
│   └── RpcErrorEnums.java   # Fehlercodes
├── JsonRpcServer.java       # Server
├── JsonRpcClient.java       # Client
└── spring/                  # Spring Boot Integration
    ├── JsonRpcAutoConfiguration.java
    ├── JsonRpcService.java
    └── JsonRpcServiceScanner.java
```

## Bauen

```bash
mvn clean compile      # Kompilieren
mvn clean package      # Paketieren
mvn clean install      # In lokales Repository installieren
```

## Lizenz

[MIT License](../LICENSE)

## Mitwirken

Issues und Pull Requests sind willkommen!
