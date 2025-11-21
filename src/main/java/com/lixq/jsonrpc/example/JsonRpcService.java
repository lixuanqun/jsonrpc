package com.lixq.jsonrpc.example;

import com.lixq.jsonrpc.core.JsonRpcMethod;

/**
 * JSON-RPC 示例服务
 */
public class JsonRpcService {
    
    @JsonRpcMethod("hello")
    public String hello(String name) {
        return "Hello, " + name + "!";
    }

    @JsonRpcMethod("add")
    public Integer add(Integer a, Integer b) {
        return a + b;
    }

    @JsonRpcMethod("echo")
    public Object echo(Object message) {
        return message;
    }
}
