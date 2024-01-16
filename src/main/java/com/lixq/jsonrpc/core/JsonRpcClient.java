package com.lixq.jsonrpc.core;

import java.util.List;

public interface JsonRpcClient {
    void asyncSend(JsonRpcRequest request,JsonRpcRequestHeader requestHeader,JsonRpcResponseHeader responseHeader);
    JsonRpcResponse send(JsonRpcRequest request);
    void asyncBatchSend(List<JsonRpcRequest> list,JsonRpcRequestHeader requestHeader,JsonRpcResponseHeader responseHeader);
    List<JsonRpcResponse> batchSend(List<JsonRpcRequest> list);
}
