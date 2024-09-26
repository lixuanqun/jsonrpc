package com.lixq.jsonrpc;

import com.lixq.jsonrpc.core.JsonRpcProtocol;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;
import reactor.util.Logger;
import reactor.util.Loggers;
import sun.net.www.http.HttpClient;

public class JsonRpcClient {
    static final Logger log = Loggers.getLogger(JsonRpcClient.class);
    private static JsonRpcProtocol jsonRpcProtocol;
    private static String host;
    private static int port;
    private static NettyInbound inbound;
    private static NettyOutbound outbound;
    private static HttpClient.RequestSender sender;

    public void JsonRpcClient(String host, int port){
        if(host.toLowerCase().startsWith("http")){
            HttpClient client = HttpClient.create();
            client.host(host).port(port);
            sender = client.post();
        }else{
//            TcpClient client = TcpClient.create();
//            client.host(host).port(port);
//            sender = client.connect();
        }
//        sender.send();
    }
//    public void close();
//    public void connect();
//    public boolean canSend();
//    public void send(RpcRequest request, RequestHeader requestHeader, ResponseHeader responseHeader);
//    public void batchSend(List<RpcRequest> list, RequestHeader requestHeader, ResponseHeader responseHeader);
}
