package com.lixq.jsonrpc;

import com.fasterxml.jackson.databind.ObjectMapper;
<<<<<<< HEAD
import com.lixq.jsonrpc.core.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON-RPC 客户端处理器
 */
public class JsonRpcClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(JsonRpcClientHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 存储待处理的响应 Future，key 为 request id
    private final Map<String, CompletableFuture<RpcResponse>> pendingRequests = new ConcurrentHashMap<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // StringDecoder 已经将 ByteBuf 转换为 String
        String json = (String) msg;
        log.debug("Received JSON-RPC response: {}", json);

        RpcResponse response = objectMapper.readValue(json, RpcResponse.class);
        
        // 查找对应的 Future 并完成
        String id = response.getId();
        CompletableFuture<RpcResponse> future = pendingRequests.remove(id);
        if (future != null) {
            future.complete(response);
        } else {
            log.warn("Received response with unknown id: {}", id);
        }
    }

    /**
     * 注册待处理的请求
     */
    public CompletableFuture<RpcResponse> registerRequest(String id) {
        CompletableFuture<RpcResponse> future = new CompletableFuture<>();
        pendingRequests.put(id, future);
        return future;
    }

    /**
     * 取消请求
     */
    public void cancelRequest(String id) {
        CompletableFuture<RpcResponse> future = pendingRequests.remove(id);
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception in channel", cause);
        
        // 取消所有待处理的请求
        for (CompletableFuture<RpcResponse> future : pendingRequests.values()) {
            if (!future.isDone()) {
                future.completeExceptionally(cause);
            }
        }
        pendingRequests.clear();
        
        ctx.close();
=======
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

public class JsonRpcClientHandler extends SimpleChannelInboundHandler<Object> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JsonRpcResponse response;
    private final CountDownLatch latch = new CountDownLatch(1);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        String responseJson;
        if (msg instanceof FullHttpResponse) {
            FullHttpResponse httpResponse = (FullHttpResponse) msg;
            ByteBuf content = httpResponse.content();
            responseJson = content.toString(StandardCharsets.UTF_8);
        } else if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame frame = (TextWebSocketFrame) msg;
            responseJson = frame.text();
        } else if (msg instanceof ByteBuf) {
            ByteBuf buffer = (ByteBuf) msg;
            responseJson = buffer.toString(StandardCharsets.UTF_8);
        } else {
            return;
        }
        objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
        response = objectMapper.readValue(responseJson, JsonRpcResponse.class);
        latch.countDown();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
        latch.countDown();
    }

    public JsonRpcResponse getResponse() throws InterruptedException {
        latch.await();
        return response;
>>>>>>> 8e4a0eb843bd226a7ca1b5f86903446f846cc9b7
    }
}
