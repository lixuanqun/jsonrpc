package com.lixq.jsonrpc;

import com.lixq.jsonrpc.core.JsonRpcMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON-RPC 服务注册器
 */
public class JsonRpcServiceRegistry {
    private static final Logger log = LoggerFactory.getLogger(JsonRpcServiceRegistry.class);
    
    private final Map<String, MethodInvoker> methodMap = new ConcurrentHashMap<>();

    /**
     * 注册服务对象
     */
    public void registerService(Object service) {
        Class<?> clazz = service.getClass();
        Method[] methods = clazz.getMethods();
        
        for (Method method : methods) {
            JsonRpcMethod annotation = method.getAnnotation(JsonRpcMethod.class);
            if (annotation != null) {
                String methodName = annotation.value();
                methodMap.put(methodName, new MethodInvoker(service, method));
                log.info("Registered JSON-RPC method: {}", methodName);
            }
        }
    }

    /**
     * 注册方法
     */
    public void registerMethod(String methodName, MethodInvoker invoker) {
        methodMap.put(methodName, invoker);
    }

    /**
     * 获取方法调用器
     */
    public MethodInvoker getMethodInvoker(String methodName) {
        return methodMap.get(methodName);
    }

    /**
     * 方法调用器
     */
    public static class MethodInvoker {
        private final Object service;
        private final Method method;

        public MethodInvoker(Object service, Method method) {
            this.service = service;
            this.method = method;
            this.method.setAccessible(true);
        }

        public Object invoke(Object[] params) throws Exception {
            if (params == null || params.length == 0) {
                return method.invoke(service);
            } else if (params.length == 1) {
                return method.invoke(service, params[0]);
            } else {
                return method.invoke(service, (Object) params);
            }
        }

        public Method getMethod() {
            return method;
        }
    }
}

