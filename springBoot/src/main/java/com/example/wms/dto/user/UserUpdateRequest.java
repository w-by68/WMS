package com.example.wms.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 修改用户请求（不含密码，密码走独立的重置接口） */
@Data
public class UserUpdateRequest {

    @NotBlank(message = "姓名不能为空")
    private String realName;

    @NotBlank(message = "角色不能为空")
    private String role;

    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String status;
}
