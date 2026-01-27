# JSON-RPC for Java

[← मुख्य पृष्ठ पर वापस](../README.md) | [English](README_EN.md) | [中文](README_ZH.md) | [日本語](README_JA.md) | [Deutsch](README_DE.md) | **हिन्दी**

---

Netty पर आधारित उच्च-प्रदर्शन, उपयोग में आसान JSON-RPC 2.0 फ्रेमवर्क, जो नेटिव Java और Spring Boot दोनों का समर्थन करता है।

## विशेषताएं

### मुख्य विशेषताएं
- ✅ **Netty-आधारित**: उच्च-प्रदर्शन नेटवर्किंग फ्रेमवर्क
- ✅ **प्रोटोकॉल समर्थन**: TCP, HTTP, WebSocket
- ✅ **JSON-RPC 2.0**: विनिर्देश के साथ पूर्ण अनुपालन
- ✅ **बैच अनुरोध**: नेटिव बैच अनुरोध समर्थन
- ✅ **असिंक्रोनस कॉल**: CompletableFuture-आधारित असिंक्रोनस API
- ✅ **स्वचालित पंजीकरण**: Spring Boot में स्वचालित सेवा खोज

### Spring Boot एकीकरण
- ✅ **शून्य कॉन्फ़िगरेशन**: ऑटो-कॉन्फ़िगरेशन समर्थन
- ✅ **एनोटेशन-संचालित**: `@JsonRpcService` एनोटेशन
- ✅ **गुण**: `application.yml` के माध्यम से कॉन्फ़िगरेशन

## स्थापना

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

## त्वरित प्रारंभ

### 1. सेवा क्लास बनाएं

```java
import com.lixq.jsonrpc.core.JsonRpcMethod;

public class CalculatorService {
    
    @JsonRpcMethod("add")
    public Integer add(Integer a, Integer b) {
        return a + b;
    }
    
    @JsonRpcMethod("hello")
    public String hello(String name) {
        return "नमस्ते, " + name + "!";
    }
}
```

### 2. सर्वर प्रारंभ करें

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
        
        // चलता रहे
        Thread.sleep(Long.MAX_VALUE);
    }
}
```

### 3. क्लाइंट कॉल

```java
import com.lixq.jsonrpc.JsonRpcClient;
import com.lixq.jsonrpc.core.RpcResponse;

public class ClientExample {
    public static void main(String[] args) throws Exception {
        JsonRpcClient client = new JsonRpcClient("127.0.0.1", 18080);
        client.connect().get(5, TimeUnit.SECONDS);
        
        // असिंक्रोनस कॉल
        CompletableFuture<RpcResponse> future = client.sendRequest("add", new Object[]{10, 20});
        
        future.thenAccept(response -> {
            System.out.println("परिणाम: " + response.getResult()); // 30
        });
        
        Thread.sleep(2000);
        client.close();
    }
}
```

## Spring Boot उपयोग

### 1. application.yml कॉन्फ़िगर करें

```yaml
jsonrpc:
  enabled: true
  server:
    enabled: true
    protocol: TCP
    host: 0.0.0.0
    port: 18080
```

### 2. सेवा बनाएं

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

### 3. एप्लिकेशन प्रारंभ करें

बस अपना Spring Boot एप्लिकेशन प्रारंभ करें। JSON-RPC सर्वर स्वचालित रूप से प्रारंभ हो जाएगा।

## बेंचमार्क परिणाम

| परिदृश्य | क्लाइंट | अनुरोध | थ्रूपुट | P50 | P99 |
|----------|---------|--------|---------|-----|-----|
| हल्का | 10 | 1,000 | 1,771 QPS | 1ms | 73ms |
| मध्यम | 50 | 10,000 | 7,229 QPS | 3ms | 32ms |
| भारी | 100 | 50,000 | 13,915 QPS | 4ms | 21ms |
| चरम | 200 | 200,000 | 31,808 QPS | 3ms | 26ms |

## प्रोजेक्ट संरचना

```
src/main/java/com/lixq/jsonrpc/
├── core/                    # कोर क्लासेस
│   ├── JsonRpcMethod.java   # मेथड एनोटेशन
│   ├── RpcRequest.java      # अनुरोध ऑब्जेक्ट
│   ├── RpcResponse.java     # प्रतिक्रिया ऑब्जेक्ट
│   └── RpcErrorEnums.java   # त्रुटि कोड
├── JsonRpcServer.java       # सर्वर
├── JsonRpcClient.java       # क्लाइंट
└── spring/                  # Spring Boot एकीकरण
    ├── JsonRpcAutoConfiguration.java
    ├── JsonRpcService.java
    └── JsonRpcServiceScanner.java
```

## बिल्ड करें

```bash
mvn clean compile      # कंपाइल करें
mvn clean package      # पैकेज करें
mvn clean install      # लोकल रिपॉजिटरी में इंस्टॉल करें
```

## लाइसेंस

[MIT License](../LICENSE)

## योगदान

Issues और Pull Requests का स्वागत है!
