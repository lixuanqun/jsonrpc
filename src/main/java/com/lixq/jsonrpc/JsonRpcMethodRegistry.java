package com.lixq.jsonrpc;


import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JsonRpcMethodRegistry {
    private final Map<String, MethodInfo> methodMap = new HashMap<>();

    public void registerMethod(String methodName, Method method, Object instance) {
        methodMap.put(methodName, new MethodInfo(method, instance));
    }

    public MethodInfo getMethod(String methodName) {
        return methodMap.get(methodName);
    }

    public static void scanPackage(JsonRpcMethodRegistry registry) {
        // 默认扫描本工程根包，可根据实际情况修改
        String  packageName = getClass().getPackage().getName();
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(JsonRpcMethod.class);
        for (Class<?> clazz : classes) {
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(JsonRpcMethod.class)) {
                    JsonRpcMethod annotation = method.getAnnotation(JsonRpcMethod.class);
                    String methodName = annotation.value().isEmpty() ? method.getName() : annotation.value();
                    try {
                        Object instance = clazz.getDeclaredConstructor().newInstance();
                        registry.registerMethod(methodName, method, instance);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    public static class MethodInfo {
        private final Method method;
        private final Object instance;

        public MethodInfo(Method method, Object instance) {
            this.method = method;
            this.instance = instance;
        }

        public Method getMethod() {
            return method;
        }

        public Object getInstance() {
            return instance;
        }
    }
}
