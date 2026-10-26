package com.example.wms.vo.auth;

import lombok.Data;

import java.time.LocalDateTime;

/** 用户个人信息 VO（不含密码） */
@Data
public class UserProfileVO {

    private Long id;

    private String username;

    private String realName;

    private String role;

    private String phone;

    private String email;

    private String status;

    private LocalDateTime lastLoginTime;
}
