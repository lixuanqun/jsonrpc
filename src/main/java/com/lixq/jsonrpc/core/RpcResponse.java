package com.lixq.jsonrpc.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON-RPC 2.0 响应对象
 * 
 * 规范要求：
 * - jsonrpc: 必须为 "2.0"
 * - result: 成功时必须存在，失败时不能存在
 * - error: 失败时必须存在，成功时不能存在
 * - id: 必须与请求中的 id 相同
 * 
 * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC 2.0 Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RpcResponse {
    /**
     * JSON-RPC 协议版本，必须为 "2.0"
     */
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";
    
    /**
     * 调用结果，成功时存在
     */
    @JsonProperty("result")
    private Object result;
    
    /**
     * 错误信息，失败时存在
     */
    @JsonProperty("error")
    private RpcError error;
    
    /**
     * 请求标识符，与请求中的 id 相同
     */
    @JsonProperty("id")
    private String id;

    public RpcResponse() {
    }

    /**
     * 创建成功响应
     */
    public RpcResponse(Object result, String id) {
        this.result = result;
        this.id = id;
    }

    /**
     * 创建错误响应
     */
    public RpcResponse(RpcError error, String id) {
        this.error = error;
        this.id = id;
    }
    
    /**
     * 创建成功响应的工厂方法
     */
    public static RpcResponse success(Object result, String id) {
        return new RpcResponse(result, id);
    }
    
    /**
     * 创建错误响应的工厂方法
     */
    public static RpcResponse error(int code, String message, Object data, String id) {
        return new RpcResponse(new RpcError(code, message, data), id);
    }
    
    /**
     * 从错误枚举创建错误响应
     */
    public static RpcResponse error(RpcErrorEnums errorEnum, String id) {
        return error(errorEnum, null, id);
    }
    
    /**
     * 从错误枚举创建错误响应（带附加数据）
     */
    public static RpcResponse error(RpcErrorEnums errorEnum, Object data, String id) {
        return new RpcResponse(new RpcError(errorEnum.getCode(), errorEnum.getMessage(), data), id);
    }
    
    /**
     * 检查是否为成功响应
     */
    public boolean isSuccess() {
        return error == null;
    }
    
    /**
     * 检查是否为错误响应
     */
    public boolean isError() {
        return error != null;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public RpcError getError() {
        return error;
    }

    public void setError(RpcError error) {
        this.error = error;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    @Override
    public String toString() {
        return "RpcResponse{" +
                "jsonrpc='" + jsonrpc + '\'' +
                ", result=" + result +
                ", error=" + error +
                ", id='" + id + '\'' +
                '}';
    }

    /**
     * JSON-RPC 2.0 错误对象
     * 
     * 规范要求：
     * - code: 必须为整数，预定义错误码范围 -32768 到 -32000
     * - message: 必须为字符串，简短描述
     * - data: 可选，包含附加信息
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RpcError {
        /**
         * 错误码
         */
        @JsonProperty("code")
        private int code;
        
        /**
         * 错误消息
         */
        @JsonProperty("message")
        private String message;
        
        /**
         * 附加数据（可选）
         */
        @JsonProperty("data")
        private Object data;

        public RpcError() {
        }

        public RpcError(int code, String message, Object data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }
        
        public RpcError(int code, String message) {
            this(code, message, null);
        }
        
        /**
         * 从错误枚举创建
         */
        public static RpcError fromEnum(RpcErrorEnums errorEnum) {
            return new RpcError(errorEnum.getCode(), errorEnum.getMessage(), errorEnum.getData());
        }
        
        /**
         * 从错误枚举创建（带附加数据）
         */
        public static RpcError fromEnum(RpcErrorEnums errorEnum, Object data) {
            return new RpcError(errorEnum.getCode(), errorEnum.getMessage(), data);
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
        
        @Override
        public String toString() {
            return "RpcError{" +
                    "code=" + code +
                    ", message='" + message + '\'' +
                    ", data=" + data +
                    '}';
        }
    }
}
