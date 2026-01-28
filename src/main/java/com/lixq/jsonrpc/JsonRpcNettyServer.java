package com.lixq.jsonrpc;

import com.lixq.jsonrpc.example.JsonRpcService;
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

public class JsonRpcNettyServer {
    private static final int PORT = 8081;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private final JsonRpcServiceRegistry serviceRegistry;

    public JsonRpcNettyServer() {
        this.serviceRegistry = new JsonRpcServiceRegistry();
        // Register default service
        this.serviceRegistry.registerService(new JsonRpcService());
    }

//    @PostConstruct
    public void start() throws Exception {
        // 调整线程池大小
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
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
                        // StringDecoder might interfere with HTTP/WS frames if not careful, 
                        // but JsonRpcServerHandler will handle specific types.
                        // However, StringDecoder expects ByteBuf. 
                        // If WebSocket handler handles the frame, it outputs WebSocketFrame.
                        // If HttpObjectAggregator outputs FullHttpRequest.
                        // StringDecoder might complain if it receives non-ByteBuf.
                        // We should probably remove StringDecoder/Encoder here and let Handler handle conversions
                        // OR ensure they are only used for raw TCP. 
                        // For now, I'll remove them to avoid ClassCastException in StringDecoder if it gets a Frame.
                        // But wait, JsonRpcServer.java uses them for TCP. 
                        // This server seems dedicated to HTTP/WS.
                        
                        pipeline.addLast(new JsonRpcServerHandler(serviceRegistry));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        b.childOption(ChannelOption.TCP_NODELAY, true);
        ChannelFuture f = b.bind(PORT).sync();
        System.out.println("JSON-RPC server started and listening on port " + PORT);
        f.channel().closeFuture().sync();
    }


//    @PreDestroy
    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
    
    public static void main(String[] args) throws Exception {
        new JsonRpcNettyServer().start();
    }
}
