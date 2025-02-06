package com.lixq.jsonrpc;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    }
}
