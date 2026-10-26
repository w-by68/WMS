package com.example.wms.config;

import com.example.wms.common.ResultCode;
import com.example.wms.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 安全配置（02 阶段正式版）：
 * 1. 无状态 + JWT，关闭 CSRF、表单登录、httpBasic；
 * 2. 放行登录接口与 swagger，其余请求经过 JwtAuthenticationFilter；
 * 3. 开启方法级鉴权 @PreAuthorize；
 * 4. 自定义未认证(1002)/无权限(1004)响应，保持 HTTP 200 + 业务码 JSON 的统一返回。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 前后端分离 + JWT 无状态，不需要 CSRF（CSRF 依赖 Cookie-Session 机制）
                .csrf(csrf -> csrf.disable())
                // CORS：允许前端跨域调用（08 阶段运维部署后可在 Nginx 层收敛，此处保留开发期放开）
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 无状态：不创建/不使用 HttpSession，token 自带身份
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 路径权限：登录与 swagger 放行，其余需要认证
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/doc.html",
                                "/actuator/health"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // 把 JWT 过滤器插到 UsernamePasswordAuthenticationFilter 之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 异常处理：未认证返回 1002，无权限返回 1004，统一 HTTP 200
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> writeJson(res, ResultCode.UNAUTHORIZED))
                        .accessDeniedHandler((req, res, e) -> writeJson(res, ResultCode.FORBIDDEN))
                );
        return http.build();
    }

    /**
     * 暴露 AuthenticationManager：登录接口需要手动调用 authenticate 校验账号密码。
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /** BCrypt 密码编码器：自带随机盐，登录校验与密码加密统一使用 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 配置：开发期放开，生产可收紧到前端域名。
     * TODO(08-运维)：生产环境由 Nginx 反向代理统一处理，此处可仅保留同源或指定前端域名。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** 写统一错误响应（HTTP 200 + 业务码 JSON） */
    private void writeJson(HttpServletResponse response, ResultCode resultCode) throws IOException {
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
