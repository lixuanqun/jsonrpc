package com.lixq.jsonrpc.core;

import java.util.List;

public class JsonRpcClient {
    void close();
    void connect();
    boolean canSend();
    void send(RpcRequest request, RequestHeader requestHeader, ResponseHeader responseHeader);
    void batchSend(List<RpcRequest> list, RequestHeader requestHeader, ResponseHeader responseHeader);
}
