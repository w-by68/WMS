package com.example.wms.vo.user;

import lombok.Data;

import java.time.LocalDateTime;

/** 用户列表/详情 VO（不含密码） */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String realName;

    private String role;

    private String phone;

    private String email;

    private String status;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;
}
