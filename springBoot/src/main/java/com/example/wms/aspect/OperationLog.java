package com.example.wms.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：标注在 Controller 方法上，切面会自动记录操作人、模块、操作、请求信息与耗时。
 * 用法：@OperationLog(module = "用户管理", operation = "创建用户")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /** 业务模块名，如"用户管理" */
    String module();

    /** 操作描述，如"创建用户" */
    String operation();
}
