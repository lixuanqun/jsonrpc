package com.lixq.jsonrpc.core;


/**
 * code	message	meaning
 * -32700	Parse error语法解析错误	服务端接收到无效的json。该错误发送于服务器尝试解析json文本
 * -32600	Invalid Request无效请求	发送的json不是一个有效的请求对象。
 * -32601	Method not found找不到方法	该方法不存在或无效
 * -32602	Invalid params无效的参数	无效的方法参数。
 * -32603	Internal error内部错误	JSON-RPC内部错误。
 * -32000 to -32099	Server error服务端错误	预留用于自定义的服务器错误。
 */
public enum RpcErrorEnums {
    ParseError(-32700, "Parse error"),
    InvalidRequest(-32600, "Invalid request"),
    MethodNotFound(-32601, "Method not found"),
    InvalidParams(-32602, "Invalid params"),
    InternalError(-32603, "Internal error");

    private int code;
    private String message;
    private Object data;

    RpcErrorEnums(int code, String message) {
        this.code = code;
        this.message = message;
    }

    void CustomRpcErrorEnums(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    public static String getMessageByCode(int code) {
        for (RpcErrorEnums errorEnum : RpcErrorEnums.values()) {
            if (errorEnum.getCode() == code) {
                return errorEnum.getMessage();
            }
        }
        return null;
    }
}
