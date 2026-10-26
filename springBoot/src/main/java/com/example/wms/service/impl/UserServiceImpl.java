package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.common.BusinessException;
import com.example.wms.common.PageResult;
import com.example.wms.common.ResultCode;
import com.example.wms.dto.user.UserCreateRequest;
import com.example.wms.dto.user.UserQueryDTO;
import com.example.wms.dto.user.UserResetPasswordRequest;
import com.example.wms.dto.user.UserUpdateRequest;
import com.example.wms.entity.SysUser;
import com.example.wms.mapper.SysUserMapper;
import com.example.wms.security.SecurityUtils;
import com.example.wms.service.UserService;
import com.example.wms.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户管理服务实现。
 * 关键约束：
 * 1. 用户名唯一（重复返回 2001）；
 * 2. 不能禁用/删除当前登录的自己；
 * 3. 系统至少保留一个启用状态的 ADMIN（删除/禁用前 count 校验）。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<UserVO> page(UserQueryDTO query) {
        Page<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(SysUser::getUsername, query.getKeyword())
                    .or().like(SysUser::getRealName, query.getKeyword()));
        }
        if (StringUtils.hasText(query.getRole())) {
            wrapper.eq(SysUser::getRole, query.getRole());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(SysUser::getCreateTime);

        IPage<SysUser> result = sysUserMapper.selectPage(page, wrapper);
        Page<UserVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return PageResult.of(voPage);
    }

    @Override
    public UserVO get(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户不存在");
        }
        return toVO(user);
    }

    @Override
    public UserVO create(UserCreateRequest request) {
        // 用户名唯一校验
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY, "用户名已存在");
        }

        SysUser user = new SysUser();
        BeanUtils.copyProperties(request, user);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        if (!StringUtils.hasText(user.getStatus())) {
            user.setStatus("ENABLED");
        }
        sysUserMapper.insert(user);
        return toVO(user);
    }

    @Override
    public UserVO update(Long id, UserUpdateRequest request) {
        SysUser user = getOrThrow(id);
        BeanUtils.copyProperties(request, user);
        sysUserMapper.updateById(user);
        return toVO(user);
    }

    @Override
    public void delete(Long id) {
        SysUser user = getOrThrow(id);
        checkSelfOperation(id, "不能删除当前登录账号");
        checkLastAdmin(user, "系统至少保留一个启用的管理员");
        sysUserMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, String status) {
        SysUser user = getOrThrow(id);
        checkSelfOperation(id, "不能禁用当前登录账号");
        // 仅当要禁用一个启用的 ADMIN 时才校验是否最后一个
        if ("ADMIN".equals(user.getRole()) && "ENABLED".equals(user.getStatus())
                && "DISABLED".equals(status)) {
            checkLastAdmin(user, "系统至少保留一个启用的管理员");
        }
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .set(SysUser::getStatus, status));
    }

    @Override
    public void resetPassword(Long id, UserResetPasswordRequest request) {
        getOrThrow(id);
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .set(SysUser::getPassword, passwordEncoder.encode(request.getNewPassword())));
    }

    // ==================== 私有方法 ====================

    private SysUser getOrThrow(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户不存在");
        }
        return user;
    }

    /** 不允许操作当前登录账号 */
    private void checkSelfOperation(Long targetId, String message) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (targetId.equals(currentUserId)) {
            throw new BusinessException(ResultCode.DATA_REFERENCED, message);
        }
    }

    /**
     * 系统至少保留一个启用的 ADMIN：
     * 若目标是启用的 ADMIN，统计启用的 ADMIN 总数，<=1 则拒绝操作。
     */
    private void checkLastAdmin(SysUser target, String message) {
        if ("ADMIN".equals(target.getRole()) && "ENABLED".equals(target.getStatus())) {
            Long adminCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getRole, "ADMIN")
                    .eq(SysUser::getStatus, "ENABLED"));
            if (adminCount <= 1) {
                throw new BusinessException(ResultCode.DATA_REFERENCED, message);
            }
        }
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
