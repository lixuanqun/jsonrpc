package com.lixq.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lixq.jsonrpc.core.RpcErrorEnums;
import com.lixq.jsonrpc.core.RpcRequest;
import com.lixq.jsonrpc.core.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON-RPC 服务器处理器（TCP + 换行分隔）
 */
public class JsonRpcServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(JsonRpcServerHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Object INVALID_ID = new Object();

    private final JsonRpcServiceRegistry serviceRegistry;

    public JsonRpcServerHandler(JsonRpcServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof String)) {
            return;
        }
        String json = (String) msg;
        log.debug("Received JSON-RPC request: {}", json);

        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("Error parsing JSON-RPC request", e);
            writeResponse(ctx, createErrorResponse(RpcErrorEnums.ParseError, null, null));
            return;
        }

        if (root.isArray()) {
            handleBatch(ctx, root);
            return;
        }

        if (root.isObject()) {
            handleSingle(ctx, root);
            return;
        }

        writeResponse(ctx, createErrorResponse(RpcErrorEnums.InvalidRequest, null, null));
    }

    private void handleSingle(ChannelHandlerContext ctx, JsonNode node) throws Exception {
        ParsedRequest parsed = parseRequestNode(node);
        if (parsed.errorResponse != null) {
            writeResponse(ctx, parsed.errorResponse);
            return;
        }

        RpcRequest request = parsed.request;
        RpcResponse response = handleRequest(request);
        if (request.getId() != null) {
            writeResponse(ctx, response);
        }
    }

    private void handleBatch(ChannelHandlerContext ctx, JsonNode arrayNode) throws Exception {
        if (!arrayNode.isArray() || arrayNode.size() == 0) {
            writeResponse(ctx, createErrorResponse(RpcErrorEnums.InvalidRequest, null, null));
            return;
        }

        List<RpcResponse> responses = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            ParsedRequest parsed = parseRequestNode(node);
            if (parsed.errorResponse != null) {
                responses.add(parsed.errorResponse);
                continue;
            }

            RpcRequest request = parsed.request;
            RpcResponse response = handleRequest(request);
            if (request.getId() != null) {
                responses.add(response);
            }
        }

        if (!responses.isEmpty()) {
            String responseJson = objectMapper.writeValueAsString(responses);
            log.debug("Sending JSON-RPC batch response: {}", responseJson);
            ctx.writeAndFlush(responseJson + "\n");
        }
    }

    private RpcResponse handleRequest(RpcRequest request) {
        if (request.getMethod() == null || request.getMethod().isEmpty()) {
            return createErrorResponse(RpcErrorEnums.InvalidRequest, request.getId(), null);
        }

        if (request.getMethod().startsWith("rpc.")) {
            return createErrorResponse(RpcErrorEnums.MethodNotFound, request.getId(), null);
        }

        JsonRpcServiceRegistry.MethodInvoker invoker = serviceRegistry.getMethodInvoker(request.getMethod());
        if (invoker == null) {
            return createErrorResponse(RpcErrorEnums.MethodNotFound, request.getId(), null);
        }

        try {
            Object params = request.getParams();
            Object[] args = params != null
                ? (params instanceof List ? ((List<?>) params).toArray() : new Object[]{params})
                : new Object[0];

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

    private ParsedRequest parseRequestNode(JsonNode node) {
        if (node == null || !node.isObject()) {
            return ParsedRequest.invalid(createErrorResponse(RpcErrorEnums.InvalidRequest, null, null));
        }

        JsonNode jsonrpcNode = node.get("jsonrpc");
        if (jsonrpcNode == null || !jsonrpcNode.isTextual() || !"2.0".equals(jsonrpcNode.asText())) {
            return ParsedRequest.invalid(createErrorResponse(RpcErrorEnums.InvalidRequest, null, null));
        }

        JsonNode methodNode = node.get("method");
        if (methodNode == null || !methodNode.isTextual() || methodNode.asText().isEmpty()) {
            return ParsedRequest.invalid(createErrorResponse(RpcErrorEnums.InvalidRequest, null, null));
        }

        Object id = parseId(node.get("id"));
        if (id == INVALID_ID) {
            return ParsedRequest.invalid(createErrorResponse(RpcErrorEnums.InvalidRequest, null, null));
        }

        Object params = null;
        JsonNode paramsNode = node.get("params");
        if (paramsNode != null && !paramsNode.isNull()) {
            params = objectMapper.convertValue(paramsNode, Object.class);
        }

        RpcRequest request = new RpcRequest(methodNode.asText(), params, id);
        return ParsedRequest.valid(request);
    }

    private Object parseId(JsonNode idNode) {
        if (idNode == null || idNode.isNull()) {
            return null;
        }
        if (idNode.isTextual()) {
            return idNode.asText();
        }
        if (idNode.isNumber()) {
            return idNode.numberValue();
        }
        return INVALID_ID;
    }

    private void writeResponse(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        String responseJson = objectMapper.writeValueAsString(response);
        log.debug("Sending JSON-RPC response: {}", responseJson);
        ctx.writeAndFlush(responseJson + "\n");
    }

    private RpcResponse createErrorResponse(RpcErrorEnums errorEnum, Object id, Object data) {
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

    private static class ParsedRequest {
        private final RpcRequest request;
        private final RpcResponse errorResponse;

        private ParsedRequest(RpcRequest request, RpcResponse errorResponse) {
            this.request = request;
            this.errorResponse = errorResponse;
        }

        private static ParsedRequest valid(RpcRequest request) {
            return new ParsedRequest(request, null);
        }

        private static ParsedRequest invalid(RpcResponse errorResponse) {
            return new ParsedRequest(null, errorResponse);
        }
    }
}
