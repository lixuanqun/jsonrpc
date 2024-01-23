package com.lixq.jsonrpc.core;

import lombok.Data;

@Data
public class RpcRequest {
    private String jsonrpc;
    private String method;
    private Object params;
    private String id;
}
