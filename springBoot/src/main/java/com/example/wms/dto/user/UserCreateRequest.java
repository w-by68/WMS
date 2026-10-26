package com.example.wms.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 创建用户请求 */
@Data
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^.{6,}$", message = "密码长度至少 6 位")
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    /** ADMIN / USER */
    @NotBlank(message = "角色不能为空")
    private String role;

    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    /** ENABLED / DISABLED，默认 ENABLED */
    private String status;
}
