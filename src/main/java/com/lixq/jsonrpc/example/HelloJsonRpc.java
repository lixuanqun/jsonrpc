package com.lixq.jsonrpc.example;

import com.lixq.jsonrpc.JsonRpcServer;
import com.lixq.jsonrpc.core.JsonRpcProtocol;

public class HelloJsonRpc {
    public static void main(String[] args) {
        JsonRpcServer server = new JsonRpcServer(JsonRpcProtocol.TCP, "127.0.0.1", 8080);
        try {
            server.start();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }
}
