package com.example.wms.controller;

import com.example.wms.aspect.OperationLog;
import com.example.wms.common.R;
import com.example.wms.dto.auth.LoginRequest;
import com.example.wms.dto.auth.PasswordChangeRequest;
import com.example.wms.dto.auth.ProfileUpdateRequest;
import com.example.wms.service.AuthService;
import com.example.wms.vo.auth.LoginVO;
import com.example.wms.vo.auth.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：登录、个人信息、改密、登出。
 * 登录接口放行，其余需带 token。
 */
@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录")
    @OperationLog(module = "认证", operation = "用户登录")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    @Operation(summary = "当前登录用户详情")
    @GetMapping("/profile")
    public R<UserProfileVO> profile() {
        return R.ok(authService.getProfile());
    }

    @Operation(summary = "修改自己的资料")
    @OperationLog(module = "认证", operation = "修改个人资料")
    @PutMapping("/profile")
    public R<Void> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        authService.updateProfile(request);
        return R.ok();
    }

    @Operation(summary = "修改自己的密码")
    @OperationLog(module = "认证", operation = "修改密码")
    @PutMapping("/password")
    public R<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(request);
        return R.ok();
    }

    @Operation(summary = "退出登录")
    @OperationLog(module = "认证", operation = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }
}
