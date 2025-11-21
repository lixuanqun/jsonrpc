package com.lixq.jsonrpc.spring;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JSON-RPC 服务注解
 * 用于标识一个类为 JSON-RPC 服务，Spring Boot 会自动扫描并注册该服务
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface JsonRpcService {
    /**
     * 服务名称，默认为类名（首字母小写）
     */
    String value() default "";
}

