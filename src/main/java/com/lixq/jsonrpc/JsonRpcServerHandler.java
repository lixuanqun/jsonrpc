package com.lixq.jsonrpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lixq.jsonrpc.core.RpcErrorEnums;
import com.lixq.jsonrpc.core.RpcRequest;
import com.lixq.jsonrpc.core.RpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * JSON-RPC 服务器处理器
 * Supports TCP (String), HTTP (FullHttpRequest), and WebSocket (TextWebSocketFrame)
 */
@ChannelHandler.Sharable
public class JsonRpcServerHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger log = LoggerFactory.getLogger(JsonRpcServerHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final JsonRpcServiceRegistry serviceRegistry;

    public JsonRpcServerHandler(JsonRpcServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof String) {
            // TCP String processing (from StringDecoder)
            handleStringRequest(ctx, (String) msg, false, false);
        } else if (msg instanceof FullHttpRequest) {
            // HTTP processing
            FullHttpRequest request = (FullHttpRequest) msg;
            if (request.method() != HttpMethod.POST) {
                sendHttpError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
                return;
            }
            String content = request.content().toString(StandardCharsets.UTF_8);
            handleStringRequest(ctx, content, true, false);
        } else if (msg instanceof TextWebSocketFrame) {
            // WebSocket processing
            TextWebSocketFrame frame = (TextWebSocketFrame) msg;
            handleStringRequest(ctx, frame.text(), false, true);
        } else {
            // Fallback: pass to next handler if possible, or ignore
            ctx.fireChannelRead(msg);
        }
    }

    private void handleStringRequest(ChannelHandlerContext ctx, String json, boolean isHttp, boolean isWs) {
        log.debug("Received JSON-RPC request: {}", json);
        String responseJson;
        try {
            // 尝试解析为单个请求或批量请求
            Object jsonNode = objectMapper.readValue(json, Object.class);
            
            if (jsonNode instanceof List) {
                // 批量请求
                @SuppressWarnings("unchecked")
                List<Object> requestList = (List<Object>) jsonNode;
                RpcRequest[] requests = objectMapper.convertValue(requestList, RpcRequest[].class);
                List<RpcResponse> responses = Arrays.asList(handleBatchRequest(requests));
                responseJson = objectMapper.writeValueAsString(responses);
            } else {
                // 单个请求
                RpcRequest request = objectMapper.readValue(json, RpcRequest.class);
                RpcResponse response = handleRequest(request);
                responseJson = objectMapper.writeValueAsString(response);
            }
        } catch (Exception e) {
            log.error("Error parsing JSON-RPC request", e);
            RpcResponse errorResponse = createErrorResponse(RpcErrorEnums.ParseError, null, null);
            try {
                responseJson = objectMapper.writeValueAsString(errorResponse);
            } catch (Exception ex) {
                // Fallback valid JSON
                responseJson = "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32700,\"message\":\"Parse error\"},\"id\":null}";
            }
        }

        log.debug("Sending JSON-RPC response: {}", responseJson);
        sendResponse(ctx, responseJson, isHttp, isWs);
    }

    private void sendResponse(ChannelHandlerContext ctx, String responseJson, boolean isHttp, boolean isWs) {
        if (isHttp) {
            ByteBuf content = Unpooled.copiedBuffer(responseJson, StandardCharsets.UTF_8);
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else if (isWs) {
            ctx.writeAndFlush(new TextWebSocketFrame(responseJson));
        } else {
            // TCP (assume StringEncoder is next)
            ctx.writeAndFlush(responseJson + "\n");
        }
    }

    private RpcResponse handleRequest(RpcRequest request) {
        // 验证请求
        if (request.getMethod() == null || request.getMethod().isEmpty()) {
            return createErrorResponse(RpcErrorEnums.InvalidRequest, request.getId(), null);
        }

        // 查找方法
        JsonRpcServiceRegistry.MethodInvoker invoker = serviceRegistry.getMethodInvoker(request.getMethod());
        if (invoker == null) {
            return createErrorResponse(RpcErrorEnums.MethodNotFound, request.getId(), null);
        }

        // 调用方法
        try {
            Object params = request.getParams();
            Object[] args = params != null ? (params instanceof List ? ((List<?>) params).toArray() : new Object[]{params}) : new Object[0];
            
            Object result = invoker.invoke(args);
            return new RpcResponse(result, request.getId());
        } catch (IllegalArgumentException e) {
            log.error("Invalid parameters for method: {}", request.getMethod(), e);
            return createErrorResponse(RpcErrorEnums.InvalidParams, request.getId(), e.getMessage());
        } catch (Exception e) {
            log.error("Error invoking method: {}", request.getMethod(), e);
            return createErrorResponse(RpcErrorEnums.InternalError, request.getId(), e.getMessage());
        }
    }

    private RpcResponse[] handleBatchRequest(RpcRequest[] requests) {
        RpcResponse[] responses = new RpcResponse[requests.length];
        for (int i = 0; i < requests.length; i++) {
            responses[i] = handleRequest(requests[i]);
        }
        return responses;
    }

    private RpcResponse createErrorResponse(RpcErrorEnums errorEnum, String id, Object data) {
        RpcResponse.RpcError rpcError = new RpcResponse.RpcError(
            errorEnum.getCode(),
            errorEnum.getMessage(),
            data
        );
        return new RpcResponse(rpcError, id);
    }

    private void sendHttpError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", StandardCharsets.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception in channel", cause);
        ctx.close();
    }
}
