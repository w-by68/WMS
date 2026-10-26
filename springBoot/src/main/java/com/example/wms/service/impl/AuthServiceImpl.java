package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.wms.common.BusinessException;
import com.example.wms.common.ResultCode;
import com.example.wms.dto.auth.LoginRequest;
import com.example.wms.dto.auth.PasswordChangeRequest;
import com.example.wms.dto.auth.ProfileUpdateRequest;
import com.example.wms.entity.SysUser;
import com.example.wms.mapper.SysUserMapper;
import com.example.wms.security.JwtUtil;
import com.example.wms.security.LoginUser;
import com.example.wms.security.SecurityUtils;
import com.example.wms.service.AuthService;
import com.example.wms.vo.auth.LoginVO;
import com.example.wms.vo.auth.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 认证服务实现。
 * 登录流程：AuthenticationManager.authenticate → 内部调 UserDetailsService + BCryptPasswordEncoder.matches，
 * 账号密码错误抛 BadCredentialsException，账号禁用抛 DisabledException，据此返回不同业务码。
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginVO login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (DisabledException e) {
            // 账号被禁用
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        } catch (AuthenticationException e) {
            // 用户名不存在或密码错误（含 BadCredentialsException），统一提示避免泄露用户是否存在
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        SysUser user = loginUser.getSysUser();

        // 更新最近登录时间
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getLastLoginTime, LocalDateTime.now()));

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginVO(token, user.getId(), user.getUsername(), user.getRealName(), user.getRole());
    }

    @Override
    public UserProfileVO getProfile() {
        SysUser user = getCurrentUserOrThrow();
        UserProfileVO vo = new UserProfileVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    @Override
    public void updateProfile(ProfileUpdateRequest request) {
        SysUser user = getCurrentUserOrThrow();
        // 只更新姓名/手机/邮箱三列，username/role/status/password 不受前端入参影响
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getRealName, request.getRealName())
                .set(SysUser::getPhone, request.getPhone())
                .set(SysUser::getEmail, request.getEmail()));
    }

    @Override
    public void changePassword(PasswordChangeRequest request) {
        SysUser user = getCurrentUserOrThrow();
        // 校验原密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "原密码不正确");
        }
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getPassword, passwordEncoder.encode(request.getNewPassword())));
    }

    @Override
    public void logout() {
        // 后端无状态：不维护会话，直接返回成功。
        // 若要实现服务端 token 失效，需要引入 Redis 黑名单（08 阶段可选加分项）。
    }

    /** 取当前登录用户，未登录时抛未授权异常 */
    private SysUser getCurrentUserOrThrow() {
        LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return loginUser.getSysUser();
    }
}
