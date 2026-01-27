package com.lixq.jsonrpc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON-RPC Netty 服务器（支持HTTP/WebSocket/TCP）
 */
public class JsonRpcNettyServer {
    private static final Logger log = LoggerFactory.getLogger(JsonRpcNettyServer.class);
    private static final int DEFAULT_PORT = 8081;
    
    private final int port;
    private final JsonRpcServiceRegistry serviceRegistry;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public JsonRpcNettyServer() {
        this(DEFAULT_PORT);
    }
    
    public JsonRpcNettyServer(int port) {
        this(port, new JsonRpcServiceRegistry());
    }
    
    public JsonRpcNettyServer(int port, JsonRpcServiceRegistry serviceRegistry) {
        this.port = port;
        this.serviceRegistry = serviceRegistry;
    }
    
    /**
     * 注册服务
     */
    public void registerService(Object service) {
        serviceRegistry.registerService(service);
    }

    /**
     * 启动服务器
     */
    public void start() throws Exception {
        // 调整线程池大小
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
        
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new HttpObjectAggregator(65536));
                            pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
                            pipeline.addLast(new StringDecoder());
                            pipeline.addLast(new StringEncoder());
                            pipeline.addLast(new JsonRpcServerHandler(serviceRegistry));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            ChannelFuture f = b.bind(port).sync();
            log.info("JSON-RPC server started and listening on port {}", port);
            f.channel().closeFuture().sync();
        } finally {
            stop();
        }
    }
    
    /**
     * 异步启动服务器
     */
    public void startAsync() {
        new Thread(() -> {
            try {
                start();
            } catch (Exception e) {
                log.error("Failed to start server", e);
            }
        }, "jsonrpc-netty-server").start();
    }

    /**
     * 停止服务器
     */
    public void stop() {
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        log.info("JSON-RPC server stopped");
    }
    
    public static void main(String[] args) throws Exception {
        JsonRpcNettyServer server = new JsonRpcNettyServer(8081);
        server.registerService(new com.lixq.jsonrpc.example.JsonRpcService());
        server.start();
    }
}
