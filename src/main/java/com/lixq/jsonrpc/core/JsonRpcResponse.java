package com.lixq.jsonrpc.core;

import lombok.Data;

@Data
public class JsonRpcResponse {
    private String jsonrpc;
    private String method;
    private Object result;
    private Object error;
    private String id;
}
