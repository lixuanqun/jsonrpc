package com.lixq.jsonrpc;

import com.fasterxml.jackson.databind.ObjectMapper;
<<<<<<< HEAD
import com.lixq.jsonrpc.core.RpcErrorEnums;
import com.lixq.jsonrpc.core.RpcRequest;
import com.lixq.jsonrpc.core.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * JSON-RPC 服务器处理器
 */
public class JsonRpcServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(JsonRpcServerHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final JsonRpcServiceRegistry serviceRegistry;

    public JsonRpcServerHandler(JsonRpcServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // StringDecoder 已经将 ByteBuf 转换为 String
        String json = (String) msg;
        log.debug("Received JSON-RPC request: {}", json);

        RpcResponse response;
        try {
            // 尝试解析为单个请求或批量请求
            Object jsonNode = objectMapper.readValue(json, Object.class);
            
            if (jsonNode instanceof List) {
                // 批量请求
                @SuppressWarnings("unchecked")
                List<Object> requestList = (List<Object>) jsonNode;
                RpcRequest[] requests = objectMapper.convertValue(requestList, RpcRequest[].class);
                List<RpcResponse> responses = Arrays.asList(handleBatchRequest(requests));
                String responseJson = objectMapper.writeValueAsString(responses);
                ctx.writeAndFlush(responseJson + "\n");
                return;
            } else {
                // 单个请求
                RpcRequest request = objectMapper.readValue(json, RpcRequest.class);
                response = handleRequest(request);
            }
        } catch (Exception e) {
            log.error("Error parsing JSON-RPC request", e);
            response = createErrorResponse(RpcErrorEnums.ParseError, null, null);
        }

        String responseJson = objectMapper.writeValueAsString(response);
        log.debug("Sending JSON-RPC response: {}", responseJson);
        ctx.writeAndFlush(responseJson + "\n");
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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception in channel", cause);
        ctx.close();
    }
=======
import com.fasterxml.jackson.databind.SerializationFeature;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

public class JsonRpcServerHandler extends SimpleChannelInboundHandler<Object> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonRpcMethodRegistry methodRegistry = new JsonRpcMethodRegistry();

    public JsonRpcServerHandler() {
        try {
            // 注册方法示例
            Method testMethod = this.getClass().getMethod("testMethod");
            methodRegistry.registerMethod("testMethod", testMethod);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public String testMethod() {
        return "Method executed successfully";
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        JsonRpcRequest rpcRequest;
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            if (request.method() != HttpMethod.POST) {
                sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
                return;
            }
            String content = request.content().toString(StandardCharsets.UTF_8);
            objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
            rpcRequest = objectMapper.readValue(content, JsonRpcRequest.class);
            handleRequest(ctx, rpcRequest, true);
        } else if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame frame = (TextWebSocketFrame) msg;
            String content = frame.text();
            objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
            rpcRequest = objectMapper.readValue(content, JsonRpcRequest.class);
            handleRequest(ctx, rpcRequest, false);
        } else if (msg instanceof ByteBuf) {
            ByteBuf buffer = (ByteBuf) msg;
            String content = buffer.toString(StandardCharsets.UTF_8);
            objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
            rpcRequest = objectMapper.readValue(content, JsonRpcRequest.class);
            handleRequest(ctx, rpcRequest, false);
        }
    }

    private void handleRequest(ChannelHandlerContext ctx, JsonRpcRequest rpcRequest, boolean isHttp) throws Exception {
        JsonRpcResponse rpcResponse = new JsonRpcResponse();
        rpcResponse.setJsonrpc("2.0");
        rpcResponse.setId(rpcRequest.getId());

        JsonRpcMethodRegistry.MethodInfo methodInfo = methodRegistry.getMethod(rpcRequest.getMethod());
        if (methodInfo != null) {
            Object result = methodInfo.getMethod().invoke(methodInfo.getInstance());
            rpcResponse.setResult(result);
        } else {
            rpcResponse.setError("Method not found");
        }

        String responseJson = objectMapper.writeValueAsString(rpcResponse);
        if (isHttp) {
            ByteBuf responseContent = Unpooled.copiedBuffer(responseJson, StandardCharsets.UTF_8);
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, responseContent);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, responseContent.readableBytes());
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.writeAndFlush(new TextWebSocketFrame(responseJson));
        }
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", StandardCharsets.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private JsonRpcResponse processRequest(JsonRpcRequest rpcRequest) {
        JsonRpcResponse rpcResponse = new JsonRpcResponse();
        rpcResponse.setJsonrpc("2.0");
        rpcResponse.setId(rpcRequest.getId());

        Method method = methodRegistry.getMethod(rpcRequest.getMethod());
        if (method != null) {
            try {
                Object result = method.invoke(this);
                rpcResponse.setResult(result);
            } catch (Exception e) {
                rpcResponse.setError("Method execution error: " + e.getMessage());
            }
        } else {
            rpcResponse.setError("Method not found");
        }
        return rpcResponse;
    }

>>>>>>> 8e4a0eb843bd226a7ca1b5f86903446f846cc9b7
}
