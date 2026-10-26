package com.example.wms.service;

import com.example.wms.dto.auth.LoginRequest;
import com.example.wms.dto.auth.PasswordChangeRequest;
import com.example.wms.dto.auth.ProfileUpdateRequest;
import com.example.wms.vo.auth.LoginVO;
import com.example.wms.vo.auth.UserProfileVO;

/**
 * 认证服务：登录、个人信息、改密、登出。
 */
public interface AuthService {

    /** 登录：校验账号密码 → 返回 token + 用户信息 */
    LoginVO login(LoginRequest request);

    /** 当前登录用户详情 */
    UserProfileVO getProfile();

    /** 修改自己的资料：仅姓名/手机/邮箱，改不了账号与角色 */
    void updateProfile(ProfileUpdateRequest request);

    /** 修改自己的密码：校验原密码后才能改 */
    void changePassword(PasswordChangeRequest request);

    /** 登出：后端无状态直接返回成功，前端清 token 即可 */
    void logout();
}
