package com.lixq.jsonrpc;

import com.lixq.jsonrpc.core.JsonRpcProtocol;
import com.sun.net.httpserver.HttpServer;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.tcp.TcpServer;
import reactor.util.Logger;
import reactor.util.Loggers;


public class JsonRpcServer {
    static final Logger log = Loggers.getLogger(JsonRpcServer.class);
    private static JsonRpcProtocol jsonRpcProtocol;
    private static String host;
    private static int port;
    private static DisposableServer disposableServer;
    public JsonRpcServer(JsonRpcProtocol jsonRpcProtocol, String host, int port){
        if(JsonRpcProtocol.TCP.equals(jsonRpcProtocol)){
            TcpServer tcpServer = TcpServer.create().host(host).port(port);
            tcpServer.doOnConnection(connection -> {
                log.info("connection");
            });
            tcpServer.handle((inbound, outbound)->{
                return outbound.sendString(Mono.just("hello"));
            });
            disposableServer = tcpServer.bindNow();
        } else if (JsonRpcProtocol.HTTP.equals(jsonRpcProtocol)) {
            HttpServer httpServer = HttpServer.create().host(host).port(port);
//            httpServer.handle((req,resp)->{
//                return httpServer.doOnConnection(new RpcServerHandler(req,resp));
//            });
            disposableServer = httpServer.bindNow();
        } else if (JsonRpcProtocol.WS.equals(jsonRpcProtocol)) {
            // to do
//            HttpServer httpServer = HttpServer.create().host(host).port(port);
        }else {
            log.error("jsonRpcProtocol:{} not support",jsonRpcProtocol);
            throw new RuntimeException("jsonRpcProtocol not support");
        }
    }

    public void start(){
        disposableServer.onDispose().block();
    }

    public void stop(){
        if(disposableServer !=null && disposableServer.isDisposed()){
            disposableServer.disposeNow();
        }
    }


    public static void main(String[] args) {
        JsonRpcServer server = new JsonRpcServer(JsonRpcProtocol.TCP,"192.168.1.7",18080);
        server.start();
    }
}
