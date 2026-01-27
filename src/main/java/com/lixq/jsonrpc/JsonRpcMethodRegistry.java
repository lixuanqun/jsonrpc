package com.lixq.jsonrpc;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * JSON-RPC 方法注册表
 * 用于管理和查找已注册的 RPC 方法
 */
public class JsonRpcMethodRegistry {
    private static final Logger log = LoggerFactory.getLogger(JsonRpcMethodRegistry.class);
    private final Map<String, MethodInfo> methodMap = new HashMap<>();

    /**
     * 注册方法
     */
    public void registerMethod(String methodName, Method method, Object instance) {
        methodMap.put(methodName, new MethodInfo(method, instance));
        log.info("Registered method: {}", methodName);
    }

    /**
     * 注册方法（无实例）
     */
    public void registerMethod(String methodName, Method method) {
        methodMap.put(methodName, new MethodInfo(method, null));
        log.info("Registered method: {}", methodName);
    }

    /**
     * 获取方法信息
     */
    public MethodInfo getMethod(String methodName) {
        return methodMap.get(methodName);
    }

    /**
     * 检查方法是否存在
     */
    public boolean hasMethod(String methodName) {
        return methodMap.containsKey(methodName);
    }

    /**
     * 获取所有已注册的方法名
     */
    public Set<String> getMethodNames() {
        return methodMap.keySet();
    }

    /**
     * 扫描指定包下带有 @JsonRpcMethod 注解的方法
     */
    public static void scanPackage(JsonRpcMethodRegistry registry, String packageName) {
        try {
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
                            log.error("Failed to create instance for class: {}", clazz.getName(), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to scan package: {}", packageName, e);
        }
    }

    /**
     * 扫描默认包（com.lixq.jsonrpc）
     */
    public static void scanDefaultPackage(JsonRpcMethodRegistry registry) {
        scanPackage(registry, "com.lixq.jsonrpc");
    }

    /**
     * 方法信息包装类
     */
    public static class MethodInfo {
        private final Method method;
        private final Object instance;

        public MethodInfo(Method method, Object instance) {
            this.method = method;
            this.instance = instance;
            if (method != null) {
                method.setAccessible(true);
            }
        }

        public Method getMethod() {
            return method;
        }

        public Object getInstance() {
            return instance;
        }

        /**
         * 调用方法
         */
        public Object invoke(Object... args) throws Exception {
            if (method == null) {
                throw new IllegalStateException("Method is null");
            }
            return method.invoke(instance, args);
        }
    }
}
