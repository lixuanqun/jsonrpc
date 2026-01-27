package com.lixq.jsonrpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lixq.jsonrpc.core.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * JSON-RPC 客户端处理器
 */
public class JsonRpcClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(JsonRpcClientHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 默认超时时间（秒）
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    
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
        return registerRequest(id, DEFAULT_TIMEOUT_SECONDS);
    }
    
    /**
     * 注册待处理的请求（带超时）
     */
    public CompletableFuture<RpcResponse> registerRequest(String id, int timeoutSeconds) {
        CompletableFuture<RpcResponse> future = new CompletableFuture<>();
        pendingRequests.put(id, future);
        
        // 设置超时
        future.orTimeout(timeoutSeconds, TimeUnit.SECONDS)
              .whenComplete((result, throwable) -> {
                  if (throwable instanceof TimeoutException) {
                      pendingRequests.remove(id);
                      log.warn("Request {} timed out after {} seconds", id, timeoutSeconds);
                  }
              });
        
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
    
    /**
     * 获取待处理请求数量
     */
    public int getPendingRequestCount() {
        return pendingRequests.size();
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
    }
}
