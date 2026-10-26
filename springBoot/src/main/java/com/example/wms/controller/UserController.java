package com.example.wms.controller;

import com.example.wms.aspect.OperationLog;
import com.example.wms.common.PageResult;
import com.example.wms.common.R;
import com.example.wms.dto.user.UserCreateRequest;
import com.example.wms.dto.user.UserQueryDTO;
import com.example.wms.dto.user.UserResetPasswordRequest;
import com.example.wms.dto.user.UserUpdateRequest;
import com.example.wms.service.UserService;
import com.example.wms.vo.user.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口（全部 ADMIN）。
 * 系统不提供注册入口，账号全部由管理员在此创建。
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户分页列表")
    @GetMapping
    public R<PageResult<UserVO>> page(UserQueryDTO query) {
        return R.ok(userService.page(query));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    public R<UserVO> get(@PathVariable Long id) {
        return R.ok(userService.get(id));
    }

    @Operation(summary = "创建用户")
    @OperationLog(module = "用户管理", operation = "创建用户")
    @PostMapping
    public R<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return R.ok(userService.create(request));
    }

    @Operation(summary = "修改用户信息")
    @OperationLog(module = "用户管理", operation = "修改用户")
    @PutMapping("/{id}")
    public R<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return R.ok(userService.update(id, request));
    }

    @Operation(summary = "删除用户")
    @OperationLog(module = "用户管理", operation = "删除用户")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.ok();
    }

    @Operation(summary = "启用/禁用用户")
    @OperationLog(module = "用户管理", operation = "切换用户状态")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        userService.updateStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "重置用户密码")
    @OperationLog(module = "用户管理", operation = "重置密码")
    @PutMapping("/{id}/password")
    public R<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody UserResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return R.ok();
    }
}
