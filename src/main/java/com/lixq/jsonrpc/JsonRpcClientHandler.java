package com.lixq.jsonrpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lixq.jsonrpc.core.RpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON-RPC Client Handler
 * Supports handling responses from TCP, HTTP, and WebSocket
 */
public class JsonRpcClientHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger log = LoggerFactory.getLogger(JsonRpcClientHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Key: request ID
    private final Map<String, CompletableFuture<RpcResponse>> pendingRequests = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        String json = null;
        if (msg instanceof String) {
            json = (String) msg;
        } else if (msg instanceof FullHttpResponse) {
            FullHttpResponse httpResponse = (FullHttpResponse) msg;
            json = httpResponse.content().toString(StandardCharsets.UTF_8);
        } else if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame frame = (TextWebSocketFrame) msg;
            json = frame.text();
        } else if (msg instanceof ByteBuf) {
            // Fallback for raw TCP if StringDecoder is not in pipeline or didn't trigger
             json = ((ByteBuf) msg).toString(StandardCharsets.UTF_8);
        }

        if (json != null) {
            log.debug("Received JSON-RPC response: {}", json);
            try {
                // If it's a list (batch response), this simple client might fail unless we handle List<RpcResponse>
                // For now, assuming single response or we need to handle List logic.
                // Keeping it simple as per original design: single response.
                if (json.trim().startsWith("[")) {
                    log.warn("Batch response not fully supported in simple client handler yet.");
                    return; 
                }

                RpcResponse response = objectMapper.readValue(json, RpcResponse.class);
                String id = response.getId();
                if (id != null) {
                    CompletableFuture<RpcResponse> future = pendingRequests.remove(id);
                    if (future != null) {
                        future.complete(response);
                    } else {
                        log.warn("Received response with unknown id: {}", id);
                    }
                }
            } catch (Exception e) {
                log.error("Error parsing response: {}", json, e);
            }
        }
    }

    public CompletableFuture<RpcResponse> registerRequest(String id) {
        CompletableFuture<RpcResponse> future = new CompletableFuture<>();
        pendingRequests.put(id, future);
        return future;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception in channel", cause);
        for (CompletableFuture<RpcResponse> future : pendingRequests.values()) {
            future.completeExceptionally(cause);
        }
        pendingRequests.clear();
        ctx.close();
    }
}
