package com.example.wms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表：系统不提供注册入口，账号全部由管理员创建。
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号，全局唯一（唯一索引 uk_username） */
    private String username;

    /** BCrypt 加密后的密码，任何接口不得把该字段返回给前端 */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 角色：ADMIN-管理员 USER-普通用户（见 UserRole） */
    private String role;

    private String phone;

    private String email;

    /** 状态：ENABLED-启用 DISABLED-禁用（禁用后无法登录） */
    private String status;

    /** 最近登录时间，登录成功时更新 */
    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除标记，禁止物理 DELETE */
    @TableLogic
    private Integer deleted;
}
