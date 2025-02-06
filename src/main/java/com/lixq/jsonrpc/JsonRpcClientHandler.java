package com.lixq.jsonrpc;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    }
}
