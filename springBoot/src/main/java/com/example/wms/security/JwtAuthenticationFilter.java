package com.example.wms.security;

import com.example.wms.common.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * JWT 认证过滤器：位于 UsernamePasswordAuthenticationFilter 之前，
 * 每次请求先从 Authorization 头取 Bearer token 并校验，通过则构造认证对象放入上下文。
 *
 * 关键设计：
 * - 解析失败/过期/用户被禁用时，直接用 response 写 JSON 并 return，
 *   不抛异常（否则会被全局异常处理器吞掉返回 500）；
 * - 无 token 时直接放行，由 Security 的 AuthenticationEntryPoint 返回 1002（区分白名单与受保护路径）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token == null) {
            // 无 token：交给后续 Security 链判断（白名单放行，受保护路径由 EntryPoint 返回 1002）
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Long userId = jwtUtil.getUserId(token);
            LoginUser loginUser = userDetailsService.loadUserById(userId);

            // 用户被禁用：token 仍在但账号已停用，拒绝访问
            if (!loginUser.isEnabled()) {
                writeError(response, ResultCode.ACCOUNT_DISABLED);
                return;
            }

            // 构造认证对象放入上下文，后续 @PreAuthorize 与业务层取当前用户都依赖它
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    loginUser, null, loginUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            // token 非法（签名错、过期、用户不存在）：统一视为未登录
            log.debug("JWT 校验失败: {}", e.getMessage());
            writeError(response, ResultCode.UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** 从 Authorization 头提取 Bearer token */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    /** 直接写错误响应：HTTP 200 + 业务码 JSON，避免被默认异常处理改成 401/500 */
    private void writeError(HttpServletResponse response, ResultCode resultCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = Map.of(
                "code", resultCode.getCode(),
                "message", resultCode.getMessage(),
                "data", "",
                "timestamp", System.currentTimeMillis()
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
