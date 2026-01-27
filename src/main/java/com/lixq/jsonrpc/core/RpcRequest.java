package com.lixq.jsonrpc.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON-RPC 2.0 请求对象
 * 
 * 规范要求：
 * - jsonrpc: 必须为 "2.0"
 * - method: 必须为字符串，方法名不能以 "rpc." 开头
 * - params: 可选，可以是数组或对象
 * - id: 可选，如果为 null 则表示通知（不期望响应）
 * 
 * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC 2.0 Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RpcRequest {
    /**
     * JSON-RPC 协议版本，必须为 "2.0"
     */
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";
    
    /**
     * 方法名
     */
    @JsonProperty("method")
    private String method;
    
    /**
     * 参数，可以是数组（位置参数）或对象（命名参数）
     */
    @JsonProperty("params")
    private Object params;
    
    /**
     * 请求标识符，如果为 null 则表示这是一个通知
     */
    @JsonProperty("id")
    private String id;

    public RpcRequest() {
    }

    public RpcRequest(String method, Object params, String id) {
        this.method = method;
        this.params = params;
        this.id = id;
    }
    
    /**
     * 创建通知请求（不期望响应）
     */
    public static RpcRequest createNotification(String method, Object params) {
        return new RpcRequest(method, params, null);
    }
    
    /**
     * 检查是否为通知
     */
    @JsonIgnore
    public boolean isNotification() {
        return id == null;
    }
    
    /**
     * 验证请求是否符合 JSON-RPC 2.0 规范
     */
    @JsonIgnore
    public boolean isValid() {
        // jsonrpc 必须为 "2.0"
        if (!"2.0".equals(jsonrpc)) {
            return false;
        }
        // method 必须存在且不为空
        if (method == null || method.isEmpty()) {
            return false;
        }
        // method 不能以 "rpc." 开头（保留）
        if (method.startsWith("rpc.")) {
            return false;
        }
        return true;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Object getParams() {
        return params;
    }

    public void setParams(Object params) {
        this.params = params;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    @Override
    public String toString() {
        return "RpcRequest{" +
                "jsonrpc='" + jsonrpc + '\'' +
                ", method='" + method + '\'' +
                ", params=" + params +
                ", id='" + id + '\'' +
                '}';
    }
}
