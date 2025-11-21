package com.lixq.jsonrpc;

import com.fasterxml.jackson.databind.ObjectMapper;
<<<<<<< HEAD
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * JSON-RPC 客户端
 */
public class JsonRpcClient {
    private static final Logger log = LoggerFactory.getLogger(JsonRpcClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final String host;
    private final int port;
    
    private EventLoopGroup group;
    private Channel channel;
    private JsonRpcClientHandler handler;

    public JsonRpcClient(String host, int port) {
        this(JsonRpcProtocol.TCP, host, port);
    }

    public JsonRpcClient(JsonRpcProtocol protocol, String host, int port) {
        this.host = host;
        this.port = port;
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
         .handler(new ChannelInitializer<SocketChannel>() {
             @Override
             public void initChannel(SocketChannel ch) {
                 ChannelPipeline pipeline = ch.pipeline();
                 
                 // 使用换行符作为分隔符
                 pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
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
=======
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.nio.charset.StandardCharsets;

public class JsonRpcClient {

    private static final String HOST = "localhost";
    private static final int PORT = 8081;
    private final EventLoopGroup group = new NioEventLoopGroup();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonRpcResponse sendRequest(JsonRpcRequest request,String protocol) throws Exception {
        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        if ("http".equals(protocol)) {
                            pipeline.addLast(new HttpClientCodec());
                            pipeline.addLast(new HttpObjectAggregator(65536));
                        } else if ("websocket".equals(protocol)) {
                            pipeline.addLast(new HttpClientCodec());
                            pipeline.addLast(new HttpObjectAggregator(65536));
                            pipeline.addLast(new WebSocketClientHandshakerFactory().newHandshaker(
                                    java.net.URI.create("ws://" + HOST + ":" + PORT + "/ws"),
                                    io.netty.handler.codec.http.websocketx.WebSocketVersion.V13,
                                    null, false, new DefaultHttpHeaders()));
                            pipeline.addLast(new io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler(java.net.URI.create("ws://" + HOST + ":" + PORT + "/ws")));
                        } else if ("tcp".equals(protocol)) {
                            pipeline.addLast(new StringDecoder());
                            pipeline.addLast(new StringEncoder());
                        }
                        pipeline.addLast(new JsonRpcClientHandler());
                    }
                });

        ChannelFuture f = b.connect(HOST, PORT).sync();
        String requestJson = objectMapper.writeValueAsString(request);

        if ("http".equals(protocol)) {
            ByteBuf content = Unpooled.copiedBuffer(requestJson, StandardCharsets.UTF_8);
            FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1, HttpMethod.POST, "/", content);
            httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
            httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
            f.channel().writeAndFlush(httpRequest).sync();
        } else if ("websocket".equals(protocol)) {
            f.channel().writeAndFlush(new TextWebSocketFrame(requestJson)).sync();
        } else if ("tcp".equals(protocol)) {
            f.channel().writeAndFlush(requestJson).sync();
        }

        JsonRpcClientHandler handler = (JsonRpcClientHandler) f.channel().pipeline().get(JsonRpcClientHandler.class);
        JsonRpcResponse response = handler.getResponse();

        f.channel().closeFuture().sync();
        return response;
    }

    public void shutdown() {
        group.shutdownGracefully();
>>>>>>> 8e4a0eb843bd226a7ca1b5f86903446f846cc9b7
    }
}
