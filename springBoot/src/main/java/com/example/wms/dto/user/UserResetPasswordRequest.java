package com.example.wms.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 重置密码请求（管理员重置他人密码） */
@Data
public class UserResetPasswordRequest {

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^.{6,}$", message = "密码长度至少 6 位")
    private String newPassword;
}
