package com.lixq.jsonrpc.spring;

import com.lixq.jsonrpc.JsonRpcServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * JSON-RPC 服务扫描器
 * 自动扫描并注册带 @JsonRpcService 注解的 Spring Bean
 */
public class JsonRpcServiceScanner implements BeanPostProcessor {
    private static final Logger log = LoggerFactory.getLogger(JsonRpcServiceScanner.class);
    
    private final JsonRpcServiceRegistry serviceRegistry;

    public JsonRpcServiceScanner(JsonRpcServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 检查 Bean 是否有 @JsonRpcService 注解
        JsonRpcService annotation = AnnotationUtils.findAnnotation(bean.getClass(), JsonRpcService.class);
        if (annotation != null) {
            // 注册服务到 JSON-RPC 注册表
            serviceRegistry.registerService(bean);
            log.info("Auto-registered JSON-RPC service: {} (bean: {})", bean.getClass().getName(), beanName);
        }
        return bean;
    }
}

