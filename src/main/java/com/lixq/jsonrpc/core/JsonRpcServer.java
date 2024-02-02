package com.lixq.jsonrpc.core;

import reactor.netty.DisposableServer;
import reactor.netty.tcp.TcpServer;

public class JsonRpcServer {
    private static JsonRpcProtocol jsonRpcProtocol;
    private static String host;
    private static int port;
    private static String inbound;
    private static String outbound;
    private static DisposableServer disposableServer;

    public void JsonRpcServer(String host,int port,){
        disposableServer = TcpServer.create()
                .host(host)
                .port(port);
    }

    public void JsonRpcServer

    public void start(){

    }
}
