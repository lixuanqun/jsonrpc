package com.lixq.jsonrpc.spring;

import com.lixq.jsonrpc.JsonRpcClient;
import com.lixq.jsonrpc.JsonRpcServer;
import com.lixq.jsonrpc.JsonRpcServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * JSON-RPC 自动配置类
 * Spring Boot 启动时自动配置 JSON-RPC 服务器和客户端
 */
@Configuration
@EnableConfigurationProperties(JsonRpcProperties.class)
@ConditionalOnProperty(prefix = "jsonrpc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JsonRpcAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(JsonRpcAutoConfiguration.class);

    private final JsonRpcProperties properties;
    private JsonRpcServer jsonRpcServer;
    private Thread serverThread;

    public JsonRpcAutoConfiguration(JsonRpcProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建 JSON-RPC 服务注册表
     */
    @Bean
    public JsonRpcServiceRegistry jsonRpcServiceRegistry() {
        return new JsonRpcServiceRegistry();
    }

    /**
     * 创建服务扫描器，自动发现并注册带 @JsonRpcService 注解的服务
     */
    @Bean
    public JsonRpcServiceScanner jsonRpcServiceScanner(JsonRpcServiceRegistry serviceRegistry) {
        return new JsonRpcServiceScanner(serviceRegistry);
    }

    /**
     * 创建并启动 JSON-RPC 服务器
     */
    @Bean
    @ConditionalOnProperty(prefix = "jsonrpc.server", name = "enabled", havingValue = "true", matchIfMissing = true)
    @DependsOn("jsonRpcServiceScanner")
    public JsonRpcServer jsonRpcServer(JsonRpcServiceRegistry serviceRegistry) {
        JsonRpcProperties.Server serverConfig = properties.getServer();
        
        // 使用共享的 serviceRegistry，这样服务扫描器注册的服务会自动在服务器中可用
        jsonRpcServer = new JsonRpcServer(
            serverConfig.getProtocol(),
            serverConfig.getHost(),
            serverConfig.getPort(),
            serviceRegistry
        );

        // 异步启动服务器
        serverThread = new Thread(() -> {
            try {
                jsonRpcServer.start();
            } catch (InterruptedException e) {
                log.error("JSON-RPC Server interrupted", e);
                Thread.currentThread().interrupt();
            }
        }, "jsonrpc-server");
        serverThread.setDaemon(true);
        serverThread.start();

        log.info("JSON-RPC Server auto-configured and starting on {}:{}", 
            serverConfig.getHost(), serverConfig.getPort());
        
        return jsonRpcServer;
    }

    /**
     * 服务器启动后等待服务器就绪
     * 由于服务器在独立线程启动，需要稍等片刻确保服务器已经启动
     */
    @PostConstruct
    public void waitForServerReady() {
        // 服务器启动时已经通过 JsonRpcServiceScanner 注册了服务
        // 这里主要是确保服务器已经启动
        try {
            Thread.sleep(100); // 等待服务器线程启动
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 创建 JSON-RPC 客户端 Bean（如果需要）
     */
    @Bean
    @ConditionalOnProperty(prefix = "jsonrpc.client", name = "enabled", havingValue = "true")
    public JsonRpcClient jsonRpcClient() {
        JsonRpcProperties.Client clientConfig = properties.getClient();
        
        JsonRpcClient client = new JsonRpcClient(
            clientConfig.getHost(),
            clientConfig.getPort()
        );

        log.info("JSON-RPC Client auto-configured for {}:{}", 
            clientConfig.getHost(), clientConfig.getPort());
        
        return client;
    }

    /**
     * 应用关闭时停止服务器
     */
    @PreDestroy
    public void destroy() {
        if (jsonRpcServer != null) {
            log.info("Stopping JSON-RPC Server...");
            jsonRpcServer.stop();
        }
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
        }
    }
}

