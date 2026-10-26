package com.example.wms.service;

import com.example.wms.common.PageResult;
import com.example.wms.dto.user.UserCreateRequest;
import com.example.wms.dto.user.UserQueryDTO;
import com.example.wms.dto.user.UserResetPasswordRequest;
import com.example.wms.dto.user.UserUpdateRequest;
import com.example.wms.vo.user.UserVO;

/**
 * 用户管理服务（全部 ADMIN）。
 */
public interface UserService {

    /** 分页查询 */
    PageResult<UserVO> page(UserQueryDTO query);

    /** 详情 */
    UserVO get(Long id);

    /** 创建用户（系统唯一入口，无注册页） */
    UserVO create(UserCreateRequest request);

    /** 修改用户信息（不含密码） */
    UserVO update(Long id, UserUpdateRequest request);

    /** 逻辑删除 */
    void delete(Long id);

    /** 启用/禁用 */
    void updateStatus(Long id, String status);

    /** 重置密码 */
    void resetPassword(Long id, UserResetPasswordRequest request);
}
