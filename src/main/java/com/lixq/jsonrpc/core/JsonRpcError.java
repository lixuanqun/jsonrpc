package com.lixq.jsonrpc.core;

import lombok.Data;

@Data
public class JsonRpcError {
    private String jsonrpc;
    private String method;
    private Object code;
    private String message;
    private Object data;
    private String id;
}
