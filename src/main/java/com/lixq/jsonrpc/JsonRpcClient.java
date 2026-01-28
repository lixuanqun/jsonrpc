package com.lixq.jsonrpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lixq.jsonrpc.core.JsonRpcProtocol;
import com.lixq.jsonrpc.core.RpcRequest;
import com.lixq.jsonrpc.core.RpcResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class JsonRpcClient {
    private static final Logger log = LoggerFactory.getLogger(JsonRpcClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String host;
    private final int port;
    private final JsonRpcProtocol protocol;
    
    private EventLoopGroup group;
    private Channel channel;
    private JsonRpcClientHandler handler;

    public JsonRpcClient(String host, int port) {
        this(JsonRpcProtocol.TCP, host, port);
    }
    
    public JsonRpcClient(JsonRpcProtocol protocol, String host, int port) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
    }

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
                 if (protocol == JsonRpcProtocol.HTTP) {
                     pipeline.addLast(new HttpClientCodec());
                     pipeline.addLast(new HttpObjectAggregator(65536));
                 } else if (protocol == JsonRpcProtocol.WS) {
                      pipeline.addLast(new HttpClientCodec());
                      pipeline.addLast(new HttpObjectAggregator(65536));
                      try {
                          URI uri = URI.create("ws://" + host + ":" + port + "/ws");
                          WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                                  uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders());
                          // Using a simpler constructor or handshaker if possible. 
                          // Netty's WebSocketClientProtocolHandler handles the handshake logic if placed in pipeline.
                          pipeline.addLast(new WebSocketClientProtocolHandler(uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders(), 65536));
                      } catch (Exception e) {
                          log.error("Failed to create WS handler", e);
                      }
                 } else {
                     // TCP
                     pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                     pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));
                     pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));
                 }
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
                connectFuture.completeExceptionally(f.cause());
                group.shutdownGracefully();
            }
        });
        return connectFuture;
    }

    public CompletableFuture<RpcResponse> sendRequest(String method, Object params) {
        return sendRequest(new RpcRequest(method, params, UUID.randomUUID().toString()));
    }

    public CompletableFuture<RpcResponse> sendRequest(RpcRequest request) {
        if (channel == null || !channel.isActive()) {
            CompletableFuture<RpcResponse> f = new CompletableFuture<>();
            f.completeExceptionally(new IllegalStateException("Not connected"));
            return f;
        }

        try {
            String json = objectMapper.writeValueAsString(request);
            CompletableFuture<RpcResponse> future = handler.registerRequest(request.getId());

            if (protocol == JsonRpcProtocol.HTTP) {
                 ByteBuf content = Unpooled.copiedBuffer(json, StandardCharsets.UTF_8);
                 FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                        HttpVersion.HTTP_1_1, HttpMethod.POST, "/", content);
                httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
                httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
                channel.writeAndFlush(httpRequest);
            } else if (protocol == JsonRpcProtocol.WS) {
                channel.writeAndFlush(new TextWebSocketFrame(json));
            } else {
                channel.writeAndFlush(json + "\n");
            }
            return future;
        } catch (Exception e) {
             CompletableFuture<RpcResponse> f = new CompletableFuture<>();
             f.completeExceptionally(e);
             return f;
        }
    }
    
    public void close() {
        if (group != null) group.shutdownGracefully();
    }
    
    public static void main(String[] args) throws Exception {
        // Example usage: HTTP default
        JsonRpcClient client = new JsonRpcClient(JsonRpcProtocol.HTTP, "127.0.0.1", 8081);
        
        try {
            client.connect().get(5, TimeUnit.SECONDS);
            RpcResponse response = client.sendRequest("hello", "World").get(5, TimeUnit.SECONDS);
            System.out.println("Result: " + response.getResult());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.close();
        }
    }
}
