package com.lixq.jsonrpc;

import com.lixq.jsonrpc.core.JsonRpcProtocol;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * JSON-RPC 服务器
 */
public class JsonRpcServer {
    private static final Logger log = LoggerFactory.getLogger(JsonRpcServer.class);
    
    private final JsonRpcProtocol protocol;
    private final String host;
    private final int port;
    private final JsonRpcServiceRegistry serviceRegistry;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public JsonRpcServer(JsonRpcProtocol protocol, String host, int port) {
        this(protocol, host, port, new JsonRpcServiceRegistry());
    }

    public JsonRpcServer(JsonRpcProtocol protocol, String host, int port, JsonRpcServiceRegistry serviceRegistry) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * 注册服务对象
     */
    public void registerService(Object service) {
        serviceRegistry.registerService(service);
    }

    /**
     * 启动服务器
     */
    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .option(ChannelOption.SO_BACKLOG, 1024)
             .childOption(ChannelOption.SO_KEEPALIVE, true)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) {
                     ChannelPipeline pipeline = ch.pipeline();
                     
                     // 使用换行符作为分隔符，支持行分隔的 JSON 消息
                     pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                     pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));
                     pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));
                     
                     // 添加业务处理器
                     pipeline.addLast(new JsonRpcServerHandler(serviceRegistry));
                 }
             });

            ChannelFuture f = b.bind(host, port).sync();
            serverChannel = f.channel();
            
            log.info("JSON-RPC Server started on {}:{} with protocol: {}", host, port, protocol);
            
            // 等待服务器 socket 关闭
            f.channel().closeFuture().sync();
        } finally {
            stop();
        }
    }

    /**
     * 异步启动服务器（不阻塞）
     */
    public void startAsync() {
        new Thread(() -> {
            try {
                start();
            } catch (InterruptedException e) {
                log.error("Server interrupted", e);
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * 停止服务器
     */
    public void stop() {
        if (serverChannel != null && serverChannel.isActive()) {
            serverChannel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        log.info("JSON-RPC Server stopped");
    }

    public static void main(String[] args) {
        JsonRpcServer server = new JsonRpcServer(JsonRpcProtocol.TCP, "0.0.0.0", 18080);
        
        // 注册示例服务
        server.registerService(new com.lixq.jsonrpc.example.JsonRpcService());
        
        server.startAsync();
        
        // 保持主线程运行
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            server.stop();
        }
    }
}
