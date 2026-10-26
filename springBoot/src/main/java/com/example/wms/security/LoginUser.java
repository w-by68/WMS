package com.example.wms.security;

import com.example.wms.entity.SysUser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security 认证主体：持有 SysUser 实体，供 SecurityContext 存放。
 * 角色前缀约定：数据库存 ADMIN/USER，框架要求 ROLE_ 前缀，
 * 因此 @PreAuthorize("hasRole('ADMIN')") 实际匹配的是 ROLE_ADMIN。
 */
@Getter
@AllArgsConstructor
public class LoginUser implements UserDetails {

    private final SysUser sysUser;

    /**
     * 角色信息：框架约定权限字符串需以 ROLE_ 开头，
     * 这样 Controller 上的 @PreAuthorize("hasRole('ADMIN')") 才能正确匹配 ROLE_ADMIN。
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + sysUser.getRole()));
    }

    @Override
    public String getPassword() {
        return sysUser.getPassword();
    }

    @Override
    public String getUsername() {
        return sysUser.getUsername();
    }

    /**
     * 账号是否启用：DISABLED 的账号在此返回 false，
     * DaoAuthenticationProvider 会抛出 DisabledException，登录时据此返回 1003。
     */
    @Override
    public boolean isEnabled() {
        return "ENABLED".equals(sysUser.getStatus());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
