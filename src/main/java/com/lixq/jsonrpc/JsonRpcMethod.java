package com.lixq.jsonrpc;


import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface JsonRpcMethod {
    String name() default "";
    String[] params() default {};
}
