package com.example.wms.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：兜住所有 Controller 抛出的异常，统一转为 R 结构返回。
 * 职责边界：这里只做"异常 -> 返回体"的翻译，不做业务判断。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：Service 主动抛出，属于预期内错误。
     * 只记 warn 级别（预期内，不需要堆栈告警），返回具体业务码。
     */
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常：@Valid 校验失败。
     * 返回 1001，message 取第一个字段的校验提示，前端可直接展示。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .orElse(ResultCode.PARAM_ERROR.getMessage());
        log.warn("参数校验失败: {}", message);
        return R.fail(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 权限不足：@PreAuthorize 方法级鉴权失败抛出 AccessDeniedException。
     * 注：该异常由 AOP 在 Controller 方法内抛出，不经过 Security 的 ExceptionTranslationFilter，
     *    因此需要在 MVC 层单独处理，返回 1004。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public R<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return R.fail(ResultCode.FORBIDDEN);
    }

    /**
     * 系统异常：未预期的错误兜底。
     * 记 error 级别完整堆栈（运维阶段日志采集会依赖此日志），对外只给笼统提示，避免泄露内部信息。
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return R.fail(ResultCode.SYSTEM_ERROR);
    }
}
