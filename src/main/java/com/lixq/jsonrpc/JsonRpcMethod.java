package com.lixq.jsonrpc;

import java.lang.annotation.*;

/**
 * JSON-RPC 方法注解（用于向后兼容）
 * 推荐使用 com.lixq.jsonrpc.core.JsonRpcMethod
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface JsonRpcMethod {
    /**
     * 方法名，若不指定则使用方法的实际名称
     */
    String value() default "";
    
    /**
     * 参数名称列表（可选，用于命名参数支持）
     */
    String[] params() default {};
}
