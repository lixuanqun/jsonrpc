package com.lixq.jsonrpc;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonRpcResponse {
    @JsonProperty("jsonrpc")
    private String jsonrpc;
    @JsonProperty("result")
    private Object result;
    @JsonProperty("error")
    private Object error;
    @JsonProperty("id")
    private String id;

    // Getters and Setters
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

    public Object getError() {
        return error;
    }

    public void setError(Object error) {
        this.error = error;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
