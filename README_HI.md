**भाषा / Languages**: [简体中文](README.md) | [English](README_EN.md) | [日本語](README_JA.md) | [Deutsch](README_DE.md) | [हिन्दी](README_HI.md)

# Java के लिए JSON-RPC

Netty पर आधारित उच्च प्रदर्शन और उपयोग में आसान JSON-RPC फ्रेमवर्क, जो नेटिव Java और Spring Boot दोनों का समर्थन करता है।

## विशेषताएँ

### मुख्य विशेषताएँ
- ✅ **Netty आधारित**: उच्च प्रदर्शन Netty नेटवर्क फ्रेमवर्क पर निर्मित
- ✅ **प्रोटोकॉल समर्थन**: TCP, HTTP, WebSocket (अभी मुख्य रूप से TCP)
- ✅ **JSON-RPC 2.0**: JSON-RPC 2.0 विनिर्देश के अनुरूप
- ✅ **बैच अनुरोध**: JSON-RPC बैच अनुरोध समर्थित
- ✅ **असिंक कॉल**: CompletableFuture के साथ असिंक्रोनस कॉल
- ✅ **ऑटो रजिस्ट्रेशन**: Spring Boot में स्वत: सर्विस खोज और रजिस्ट्रेशन

### Spring Boot एकीकरण
- ✅ **आउट-ऑफ-द-बॉक्स**: ऑटो-कॉन्फ़िगरेशन से बिना अतिरिक्त सेटअप
- ✅ **एनोटेशन आधारित**: `@JsonRpcService` से सर्विस क्लास पहचान
- ✅ **कॉन्फ़िगरेशन**: `application.yml` के माध्यम से सेटिंग्स
- ✅ **सर्विस स्कैनिंग**: एनोटेटेड मेथड्स का स्वत: रजिस्ट्रेशन

## निर्भरताएँ

### मुख्य निर्भरताएँ
- **Netty 4.1.115.Final**: नेटवर्क कम्युनिकेशन फ्रेमवर्क
- **Jackson 2.13.4**: JSON सीरियलाइज़ेशन/डीसीरियलाइज़ेशन
  - jackson-databind
  - jackson-core
  - jackson-annotations
- **SLF4J 1.7.36**: लॉगिंग फ़साड

### वैकल्पिक निर्भरताएँ (Spring Boot)
- **Spring Boot 2.7.14**: ऑटो-कॉन्फ़िगरेशन
  - spring-boot-autoconfigure
  - spring-boot-configuration-processor
- **Spring 5.3.31**:
  - spring-context

> नोट: Spring Boot संबंधित निर्भरताएँ वैकल्पिक हैं और केवल Spring Boot परियोजनाओं में आवश्यक हैं।

## उपयोग

### विधि 1: नेटिव Java

#### 1. सर्विस क्लास बनाएँ

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

#### 2. सर्वर स्टार्ट करें

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

#### 3. क्लाइंट कॉल

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

### विधि 2: Spring Boot (अनुशंसित)

#### 1. निर्भरता जोड़ें

अपने Spring Boot प्रोजेक्ट के `pom.xml` में जोड़ें:

```xml
<dependency>
    <groupId>com.lixq.jsonrpc</groupId>
    <artifactId>jsonrpc</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

#### 2. application.yml कॉन्फ़िगर करें

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

#### 3. सर्विस क्लास बनाएँ

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

#### 4. क्लाइंट इंजेक्ट करें (वैकल्पिक)

यदि क्लाइंट उपयोग करना हो:

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

#### 5. एप्लिकेशन स्टार्ट करें

Spring Boot एप्लिकेशन शुरू करते ही JSON-RPC सर्वर स्वत: शुरू हो जाता है और `@JsonRpcService` वाली सर्विसेज़ रजिस्टर हो जाती हैं।

## विकास गाइड

### प्रोजेक्ट संरचना

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

### प्रोटोकॉल समर्थन बढ़ाना

नए प्रोटोकॉल (जैसे HTTP, WebSocket) जोड़ने के लिए:

1. **`JsonRpcServer` संशोधित करें**: नया प्रोटोकॉल लॉजिक जोड़ें
2. **नया Handler बनाएँ**: प्रोटोकॉल-विशिष्ट हैंडलर
3. **प्रोटोकॉल कोडेक जोड़ें**: Netty Pipeline में जोड़ें

HTTP उदाहरण:

```java
// Add HTTP support in JsonRpcServer.java
if (JsonRpcProtocol.HTTP.equals(protocol)) {
    // Use HttpServerCodec and other HTTP-related codecs
    pipeline.addLast(new HttpServerCodec());
    pipeline.addLast(new HttpObjectAggregator(65536));
    pipeline.addLast(new JsonRpcHttpServerHandler(serviceRegistry));
}
```

### कस्टम Handler

`JsonRpcServerHandler` या `JsonRpcClientHandler` को बढ़ाकर कस्टम लॉजिक जोड़ें:

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

### सर्विस रजिस्ट्रेशन बढ़ाएँ

कस्टम सर्विस डिस्कवरी:

```java
// Implement custom service discovery
public class CustomServiceRegistry extends JsonRpcServiceRegistry {
    // Add service discovery logic
    public void discoverServices() {
        // Discover services from configuration center, registry, etc.
    }
}
```

### मिडलवेयर समर्थन

Handler में इंटरसेप्टर/फिल्टर जोड़ें:

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

### प्रदर्शन अनुकूलन

1. **कनेक्शन पूल**: क्लाइंट कनेक्शन का पुन: उपयोग
2. **थ्रेड पूल**: सर्वर के लिए कस्टम थ्रेड पूल
3. **सीरियलाइज़ेशन अनुकूलन**: Kryo/Protobuf आदि
4. **बैच प्रोसेसिंग**: बैच प्रदर्शन सुधार

### मॉनिटरिंग और डायग्नोस्टिक्स

मॉनिटरिंग उदाहरण:

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

## प्रोजेक्ट बिल्ड

```bash
# Compile the project
mvn clean compile

# Package the project
mvn clean package

# Install to local Maven repository
mvn clean install
```

## लोड टेस्ट स्क्रिप्ट

TCP JSON-RPC लोड टेस्ट (newline-delimited JSON):

```bash
python scripts/jsonrpc_load_test.py \
  --host 127.0.0.1 \
  --port 18080 \
  --method add \
  --params "[1,2]" \
  --connections 50 \
  --requests-per-connection 1000
```

नोटिफिकेशन (रिस्पॉन्स नहीं चाहिए):

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

> नोट: यह स्क्रिप्ट मौजूदा TCP + newline-delimited प्रोटोकॉल के लिए है। पहले `JsonRpcServer` शुरू करें।

## लाइसेंस

[LICENSE](LICENSE) देखें।

## योगदान

Issues और Pull Requests का स्वागत है।

---

**चीनी संस्करण**: [README.md](README.md)
