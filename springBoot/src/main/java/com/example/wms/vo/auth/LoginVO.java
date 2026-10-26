package com.example.wms.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 登录成功返回体 */
@Data
@AllArgsConstructor
public class LoginVO {

    private String token;

    private Long userId;

    private String username;

    private String realName;

    /** ADMIN / USER，前端路由权限用 */
    private String role;
}
