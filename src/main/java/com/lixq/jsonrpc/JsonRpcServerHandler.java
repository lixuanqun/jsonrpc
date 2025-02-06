package com.lixq.jsonrpc;

import com.fasterxml.jackson.databind.ObjectMapper;
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

}
