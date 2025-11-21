package com.lixq.jsonrpc.spring;

import com.lixq.jsonrpc.core.JsonRpcProtocol;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JSON-RPC 配置属性
 */
@ConfigurationProperties(prefix = "jsonrpc")
public class JsonRpcProperties {
    
    /**
     * 是否启用 JSON-RPC 服务器
     */
    private boolean enabled = true;
    
    /**
     * 服务器配置
     */
    private Server server = new Server();
    
    /**
     * 客户端配置
     */
    private Client client = new Client();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    /**
     * 服务器配置
     */
    public static class Server {
        /**
         * 是否启用服务器
         */
        private boolean enabled = true;
        
        /**
         * 协议类型：TCP, HTTP, WS
         */
        private JsonRpcProtocol protocol = JsonRpcProtocol.TCP;
        
        /**
         * 服务器地址
         */
        private String host = "0.0.0.0";
        
        /**
         * 服务器端口
         */
        private int port = 18080;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public JsonRpcProtocol getProtocol() {
            return protocol;
        }

        public void setProtocol(JsonRpcProtocol protocol) {
            this.protocol = protocol;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    /**
     * 客户端配置
     */
    public static class Client {
        /**
         * 是否启用客户端
         */
        private boolean enabled = false;
        
        /**
         * 服务器地址
         */
        private String host = "127.0.0.1";
        
        /**
         * 服务器端口
         */
        private int port = 18080;
        
        /**
         * 连接超时时间（秒）
         */
        private int connectTimeout = 5;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }
    }
}
