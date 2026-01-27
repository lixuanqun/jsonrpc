package com.lixq.jsonrpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lixq.jsonrpc.core.JsonRpcProtocol;
import com.lixq.jsonrpc.core.RpcRequest;
import com.lixq.jsonrpc.core.RpcResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * JSON-RPC 客户端
 */
public class JsonRpcClient {
    private static final Logger log = LoggerFactory.getLogger(JsonRpcClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final String host;
    private final int port;
    private final int maxFrameLength;
    
    private EventLoopGroup group;
    private Channel channel;
    private JsonRpcClientHandler handler;

    public JsonRpcClient(String host, int port) {
        this(JsonRpcProtocol.TCP, host, port);
    }

    public JsonRpcClient(JsonRpcProtocol protocol, String host, int port) {
        this(protocol, host, port, 65536);
    }
    
    public JsonRpcClient(JsonRpcProtocol protocol, String host, int port, int maxFrameLength) {
        this.host = host;
        this.port = port;
        this.maxFrameLength = maxFrameLength;
    }

    /**
     * 连接到服务器
     */
    public CompletableFuture<Void> connect() {
        CompletableFuture<Void> connectFuture = new CompletableFuture<>();
        
        group = new NioEventLoopGroup();
        handler = new JsonRpcClientHandler();
        
        Bootstrap b = new Bootstrap();
        b.group(group)
         .channel(NioSocketChannel.class)
         .option(ChannelOption.TCP_NODELAY, true)
         .option(ChannelOption.SO_KEEPALIVE, true)
         .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
         .handler(new ChannelInitializer<SocketChannel>() {
             @Override
             public void initChannel(SocketChannel ch) {
                 ChannelPipeline pipeline = ch.pipeline();
                 
                 // 使用换行符作为分隔符
                 pipeline.addLast(new DelimiterBasedFrameDecoder(maxFrameLength, Delimiters.lineDelimiter()));
                 pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));
                 pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));
                 
                 // 添加业务处理器
                 pipeline.addLast(handler);
             }
         });

        ChannelFuture future = b.connect(host, port);
        future.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                channel = f.channel();
                log.info("Connected to JSON-RPC server at {}:{}", host, port);
                connectFuture.complete(null);
            } else {
                log.error("Failed to connect to server", f.cause());
                connectFuture.completeExceptionally(f.cause());
                group.shutdownGracefully();
            }
        });

        return connectFuture;
    }

    /**
     * 发送请求
     */
    public CompletableFuture<RpcResponse> sendRequest(String method, Object params) {
        return sendRequest(method, params, UUID.randomUUID().toString());
    }

    /**
     * 发送请求（指定 ID）
     */
    public CompletableFuture<RpcResponse> sendRequest(String method, Object params, String id) {
        if (channel == null || !channel.isActive()) {
            CompletableFuture<RpcResponse> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Client not connected"));
            return future;
        }

        RpcRequest request = new RpcRequest(method, params, id);
        return sendRequest(request);
    }

    /**
     * 发送通知（不期望响应）
     */
    public CompletableFuture<Void> sendNotification(String method, Object params) {
        if (channel == null || !channel.isActive()) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Client not connected"));
            return future;
        }

        RpcRequest request = new RpcRequest(method, params, null); // id为null表示通知
        
        try {
            String json = objectMapper.writeValueAsString(request);
            log.debug("Sending JSON-RPC notification: {}", json);
            
            CompletableFuture<Void> writeFuture = new CompletableFuture<>();
            channel.writeAndFlush(json + "\n").addListener((ChannelFutureListener) f -> {
                if (f.isSuccess()) {
                    writeFuture.complete(null);
                } else {
                    writeFuture.completeExceptionally(f.cause());
                }
            });
            return writeFuture;
        } catch (Exception e) {
            log.error("Error sending notification", e);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * 发送批量请求
     */
    public CompletableFuture<List<RpcResponse>> sendBatchRequest(List<RpcRequest> requests) {
        if (channel == null || !channel.isActive()) {
            CompletableFuture<List<RpcResponse>> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Client not connected"));
            return future;
        }

        try {
            // 为每个请求注册Future
            List<CompletableFuture<RpcResponse>> futures = requests.stream()
                    .filter(req -> req.getId() != null) // 过滤掉通知
                    .map(req -> handler.registerRequest(req.getId()))
                    .collect(Collectors.toList());
            
            String json = objectMapper.writeValueAsString(requests);
            log.debug("Sending JSON-RPC batch request: {}", json);
            
            channel.writeAndFlush(json + "\n");
            
            // 组合所有Future
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Error sending batch request", e);
            CompletableFuture<List<RpcResponse>> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * 发送请求对象
     */
    public CompletableFuture<RpcResponse> sendRequest(RpcRequest request) {
        if (channel == null || !channel.isActive()) {
            CompletableFuture<RpcResponse> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Client not connected"));
            return future;
        }

        try {
            String json = objectMapper.writeValueAsString(request);
            log.debug("Sending JSON-RPC request: {}", json);

            // 注册响应 Future
            CompletableFuture<RpcResponse> responseFuture = handler.registerRequest(request.getId());

            // 发送请求（添加换行符作为分隔符）
            channel.writeAndFlush(json + "\n");

            return responseFuture;
        } catch (Exception e) {
            log.error("Error sending request", e);
            CompletableFuture<RpcResponse> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    /**
     * 关闭连接
     */
    public void close() {
        if (channel != null && channel.isActive()) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
        log.info("JSON-RPC Client closed");
    }

    /**
     * 测试主方法
     */
    public static void main(String[] args) throws Exception {
        JsonRpcClient client = new JsonRpcClient("127.0.0.1", 18080);
        
        // 连接到服务器
        client.connect().get(5, TimeUnit.SECONDS);
        
        // 发送请求
        CompletableFuture<RpcResponse> future = client.sendRequest("hello", "world");
        
        future.thenAccept(response -> {
            if (response.getError() != null) {
                log.error("RPC Error: {} - {}", response.getError().getCode(), response.getError().getMessage());
            } else {
                log.info("RPC Response: {}", response.getResult());
            }
            client.close();
        }).exceptionally(e -> {
            log.error("Request failed", e);
            client.close();
            return null;
        });

        // 等待响应
        Thread.sleep(2000);
    }
}
