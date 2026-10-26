package com.example.wms.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户上下文工具：所有 Service 做数据范围过滤、操作人记录都从这里取。
 * 数据来源：JwtAuthenticationFilter 解析 token 后放入 SecurityContextHolder 的 LoginUser。
 */
public class SecurityUtils {

    private SecurityUtils() {
    }

    /** 获取当前认证主体，未登录时返回 null */
    public static LoginUser getCurrentLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof LoginUser ? (LoginUser) principal : null;
    }

    /** 当前用户 ID，未登录返回 null */
    public static Long getCurrentUserId() {
        LoginUser loginUser = getCurrentLoginUser();
        return loginUser == null ? null : loginUser.getSysUser().getId();
    }

    /** 当前用户名 */
    public static String getCurrentUsername() {
        LoginUser loginUser = getCurrentLoginUser();
        return loginUser == null ? null : loginUser.getSysUser().getUsername();
    }

    /** 当前用户是否为管理员 */
    public static boolean isAdmin() {
        LoginUser loginUser = getCurrentLoginUser();
        return loginUser != null && "ADMIN".equals(loginUser.getSysUser().getRole());
    }
}
