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

        // 验证JSON是否为空或只有空白字符
        if (json == null || json.trim().isEmpty()) {
            RpcResponse response = createErrorResponse(RpcErrorEnums.InvalidRequest, null, "Empty request");
            String responseJson = objectMapper.writeValueAsString(response);
            ctx.writeAndFlush(responseJson + "\n");
            return;
        }

        RpcResponse response;
        try {
            // 尝试解析为单个请求或批量请求
            Object jsonNode = objectMapper.readValue(json, Object.class);
            
            if (jsonNode instanceof List) {
                // 批量请求
                @SuppressWarnings("unchecked")
                List<Object> requestList = (List<Object>) jsonNode;
                
                // JSON-RPC 2.0规范：批量请求不能为空数组
                if (requestList.isEmpty()) {
                    response = createErrorResponse(RpcErrorEnums.InvalidRequest, null, "Empty batch request");
                    String responseJson = objectMapper.writeValueAsString(response);
                    ctx.writeAndFlush(responseJson + "\n");
                    return;
                }
                
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
            response = createErrorResponse(RpcErrorEnums.ParseError, null, e.getMessage());
        }

        String responseJson = objectMapper.writeValueAsString(response);
        log.debug("Sending JSON-RPC response: {}", responseJson);
        ctx.writeAndFlush(responseJson + "\n");
    }

    private RpcResponse handleRequest(RpcRequest request) {
        // JSON-RPC 2.0规范验证：jsonrpc字段必须是"2.0"
        if (!"2.0".equals(request.getJsonrpc())) {
            return createErrorResponse(RpcErrorEnums.InvalidRequest, request.getId(), 
                "Invalid JSON-RPC version, must be '2.0'");
        }
        
        // 验证请求
        if (request.getMethod() == null || request.getMethod().isEmpty()) {
            return createErrorResponse(RpcErrorEnums.InvalidRequest, request.getId(), 
                "Method name is required");
        }
        
        // JSON-RPC 2.0规范：方法名不能以 "rpc." 开头（保留用于内部方法）
        if (request.getMethod().startsWith("rpc.")) {
            return createErrorResponse(RpcErrorEnums.MethodNotFound, request.getId(), 
                "Method names beginning with 'rpc.' are reserved");
        }

        // 查找方法
        JsonRpcServiceRegistry.MethodInvoker invoker = serviceRegistry.getMethodInvoker(request.getMethod());
        if (invoker == null) {
            return createErrorResponse(RpcErrorEnums.MethodNotFound, request.getId(), 
                "Method '" + request.getMethod() + "' not found");
        }

        // 调用方法
        try {
            Object params = request.getParams();
            Object[] args = params != null ? (params instanceof List ? ((List<?>) params).toArray() : new Object[]{params}) : new Object[0];
            
            Object result = invoker.invoke(args);
            
            // 检查是否为通知（id为null时为通知，不返回响应）
            if (request.getId() == null) {
                return null; // 通知不返回响应
            }
            
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
        // 过滤掉通知的响应（通知不返回响应）
        return Arrays.stream(requests)
                .map(this::handleRequest)
                .filter(response -> response != null)
                .toArray(RpcResponse[]::new);
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
