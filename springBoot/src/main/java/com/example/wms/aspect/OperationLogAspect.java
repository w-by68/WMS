package com.example.wms.aspect;

import com.example.wms.entity.SysOperationLog;
import com.example.wms.security.SecurityUtils;
import com.example.wms.service.SysOperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志切面：环绕通知拦截所有标注 @OperationLog 的方法，
 * 记录操作人/模块/操作/URL/方法/IP/耗时/成功状态/异常信息，异步落库。
 *
 * 注意：切面本身同步执行（只做记录与触发异步保存），不包裹业务事务，
 * 真正的写入由 SysOperationLogService.saveAsync 在独立线程完成。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SysOperationLogService sysOperationLogService;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        boolean success = true;
        String errorMsg = null;
        Object result;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            success = false;
            errorMsg = e.getMessage();
            throw e;
        } finally {
            saveLog(joinPoint, operationLog, start, success, errorMsg);
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint, OperationLog operationLog,
                         long start, boolean success, String errorMsg) {
        try {
            SysOperationLog record = new SysOperationLog();
            record.setModule(operationLog.module());
            record.setOperation(operationLog.operation());

            // 当前操作人：未登录场景（如登录接口）为 null
            record.setUserId(SecurityUtils.getCurrentUserId());
            record.setUsername(SecurityUtils.getCurrentUsername());

            // 请求信息
            HttpServletRequest request = getRequest();
            if (request != null) {
                record.setRequestUrl(request.getRequestURI());
                record.setIp(getClientIp(request));
            }
            // 方法签名：类名#方法名
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            record.setMethod(signature.getDeclaringTypeName() + "#" + signature.getName());

            record.setCostMs((int) (System.currentTimeMillis() - start));
            record.setSuccess(success ? 1 : 0);
            record.setErrorMsg(errorMsg);

            sysOperationLogService.saveAsync(record);
        } catch (Exception e) {
            // 切面自身异常不能影响业务，仅记录
            log.error("操作日志切面异常: {}", e.getMessage(), e);
        }
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    /**
     * 获取客户端真实 IP：依次从常见代理头取，取不到再用 remoteAddr。
     * 反向代理场景下 remoteAddr 是代理机器，需要取 X-Forwarded-For。
     */
    private String getClientIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For 可能是多个 IP 逗号分隔，取第一个
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
