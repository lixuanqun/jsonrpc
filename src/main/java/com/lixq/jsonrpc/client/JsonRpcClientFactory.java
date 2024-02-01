package com.lixq.jsonrpc.client;

import com.lixq.jsonrpc.core.JsonRpcClient;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class JsonRpcClientFactory extends BasePooledObjectFactory<JsonRpcClient> {
    @Override
    public JsonRpcClient create() throws Exception {
        return connect();
    }

    @Override
    public PooledObject<JsonRpcClient> wrap(JsonRpcClient jsonRpcClient) {
        return new DefaultPooledObject<>(jsonRpcClient);
    }

    @Override
    public void destroyObject(PooledObject<JsonRpcClient> p) throws Exception {
        // 获取Client对象
        JsonRpcClient client = p.getObject();
        // 关闭连接
        if (client != null) {
            client.close();
        }
        // 销毁对象
        super.destroyObject(p);
    }

    public JsonRpcClient connect() {
        JsonRpcClient client = new JsonRpcClient();
        String host = this.properties.getHost();
        int port = this.properties.getPort();
        try {
            client.connect(host, port, this.properties.getPassword(), this.properties.getTimeoutSec());
            return client;
        } catch (InboundConnectionFailure e) {
            log.error("esl client connect server failure: host:{}, port: {}, cause:{}", host, port, e.getMessage());
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean validateObject(PooledObject<JsonRpcClient> p) {
        // 获取Client对象
        JsonRpcClient client = p.getObject();
        if (client != null) {
            return client.canSend();
        }
        return false;
    }

    @Override
    public PooledObject<JsonRpcClient> makeObject() throws Exception {
        return super.makeObject();
    }
}
