package com.lixq.jsonrpc;

import com.fasterxml.jackson.databind.ObjectMapper;
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
}
