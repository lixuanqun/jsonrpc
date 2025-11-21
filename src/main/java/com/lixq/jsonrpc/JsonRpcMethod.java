package com.lixq.jsonrpc;


<<<<<<< HEAD
import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface JsonRpcMethod {
    String name() default "";
    String[] params() default {};
=======
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonRpcMethod {
    String value() default ""; // 可指定方法名，若不指定则使用方法的实际名称
>>>>>>> 8e4a0eb843bd226a7ca1b5f86903446f846cc9b7
}
