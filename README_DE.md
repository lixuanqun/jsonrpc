**Sprache / Languages**: [简体中文](README.md) | [English](README_EN.md) | [日本語](README_JA.md) | [Deutsch](README_DE.md) | [हिन्दी](README_HI.md)

# JSON-RPC für Java

Ein leistungsstarkes, einfach zu nutzendes JSON-RPC-Framework auf Basis von Netty, das sowohl natives Java als auch Spring Boot unterstützt.

## Funktionen

### Kernfunktionen
- ✅ **Netty-basiert**: Implementiert mit dem leistungsstarken Netty-Netzwerkframework
- ✅ **Protokollunterstützung**: TCP, HTTP, WebSocket (derzeit Fokus auf TCP)
- ✅ **JSON-RPC 2.0**: Konform zur JSON-RPC 2.0 Spezifikation
- ✅ **Batch Requests**: Unterstützung für JSON-RPC Batch-Anfragen
- ✅ **Async Calls**: Asynchrone Aufrufe mit CompletableFuture
- ✅ **Auto-Registrierung**: Automatische Service-Erkennung und -Registrierung in Spring Boot

### Spring Boot Integration
- ✅ **Out of the Box**: Auto-Konfiguration ohne zusätzliche Settings
- ✅ **Annotation-basiert**: `@JsonRpcService` für Service-Klassen
- ✅ **Konfiguration**: Unterstützung über `application.yml`
- ✅ **Service-Scan**: Automatische Registrierung annotierter Methoden

## Abhängigkeiten

### Kernabhängigkeiten
- **Netty 4.1.115.Final**: Netzwerk-Framework
- **Jackson 2.13.4**: JSON Serialisierung/Deserialisierung
  - jackson-databind
  - jackson-core
  - jackson-annotations
- **SLF4J 1.7.36**: Logging-Fassade

### Optionale Abhängigkeiten (Spring Boot)
- **Spring Boot 2.7.14**: Auto-Konfiguration
  - spring-boot-autoconfigure
  - spring-boot-configuration-processor
- **Spring 5.3.31**:
  - spring-context

> Hinweis: Spring Boot Abhängigkeiten sind optional und nur in Spring Boot Projekten erforderlich.

## Nutzung

### Methode 1: Natives Java

#### 1. Service-Klasse erstellen

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

#### 2. Server starten

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

#### 3. Client-Aufruf

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

### Methode 2: Spring Boot (empfohlen)

#### 1. Abhängigkeit hinzufügen

Füge Folgendes in dein Spring Boot `pom.xml` ein:

```xml
<dependency>
    <groupId>com.lixq.jsonrpc</groupId>
    <artifactId>jsonrpc</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

#### 2. application.yml konfigurieren

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

#### 3. Service-Klasse erstellen

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

#### 4. Client injizieren (optional)

Wenn du den Client verwenden möchtest:

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

#### 5. Anwendung starten

Starte einfach die Spring Boot Anwendung. Der JSON-RPC Server startet automatisch, und alle `@JsonRpcService`-Services werden registriert.

## Entwicklungsleitfaden

### Projektstruktur

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

### Protokollunterstützung erweitern

Um neue Protokolle (z. B. HTTP, WebSocket) zu unterstützen:

1. **`JsonRpcServer` anpassen**: neue Protokoll-Logik hinzufügen
2. **Neue Handler erstellen**: Handler je Protokoll
3. **Protokoll-Codecs hinzufügen**: Netty Pipeline erweitern

Beispiel: HTTP-Unterstützung

```java
// Add HTTP support in JsonRpcServer.java
if (JsonRpcProtocol.HTTP.equals(protocol)) {
    // Use HttpServerCodec and other HTTP-related codecs
    pipeline.addLast(new HttpServerCodec());
    pipeline.addLast(new HttpObjectAggregator(65536));
    pipeline.addLast(new JsonRpcHttpServerHandler(serviceRegistry));
}
```

### Eigene Handler

Erweitere `JsonRpcServerHandler` oder `JsonRpcClientHandler` für eigene Logik:

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

### Service-Registrierung erweitern

Eigene Service-Discovery implementieren:

```java
// Implement custom service discovery
public class CustomServiceRegistry extends JsonRpcServiceRegistry {
    // Add service discovery logic
    public void discoverServices() {
        // Discover services from configuration center, registry, etc.
    }
}
```

### Middleware-Unterstützung

Interceptor/Filter können in Handlern ergänzt werden:

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

### Performance-Optimierung

1. **Connection Pool**: Verbindungen wiederverwenden
2. **Thread Pool**: Eigene Thread-Pools für den Server
3. **Serialization Optimization**: z. B. Kryo/Protobuf
4. **Batch Processing**: Batch-Leistung optimieren

### Monitoring und Diagnose

Beispiel für Monitoring:

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

## Projekt bauen

```bash
# Compile the project
mvn clean compile

# Package the project
mvn clean package

# Install to local Maven repository
mvn clean install
```

## Load-Testing Script

TCP JSON-RPC Lasttest (newline-delimited JSON):

```bash
python scripts/jsonrpc_load_test.py \
  --host 127.0.0.1 \
  --port 18080 \
  --method add \
  --params "[1,2]" \
  --connections 50 \
  --requests-per-connection 1000
```

Benachrichtigungen (keine Antwort erwartet):

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

> Hinweis: Das Script zielt auf das aktuelle TCP + newline-delimited Protokoll. Starte zuerst `JsonRpcServer`.

## Lizenz

Siehe [LICENSE](LICENSE).

## Beitrag

Issues und Pull Requests sind willkommen!

---

**Chinesische Version**: [README.md](README.md)
