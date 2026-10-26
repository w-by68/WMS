package com.example.wms.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人资料请求：普通用户/管理员只能改自己的基础信息。
 * 刻意不含 username / role / status —— 这些属于管理员权限，防止越权提权。
 */
@Data
public class ProfileUpdateRequest {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名长度不能超过 50")
    private String realName;

    @Size(max = 20, message = "手机号长度不能超过 20")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过 100")
    private String email;
}
