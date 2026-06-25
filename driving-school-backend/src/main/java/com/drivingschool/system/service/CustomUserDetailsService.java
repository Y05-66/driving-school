package com.drivingschool.system.service;

import com.drivingschool.system.entity.SysUser;
import com.drivingschool.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 自定义用户详情服务实现类。
 * <p>
 * 实现 Spring Security 的 UserDetailsService 接口，
 * 负责根据用户名从数据库加载用户信息，用于登录认证。
 * </p>
 * <p>
 * 当用户提交登录请求时，Spring Security 的 AuthenticationManager
 * 会调用此服务的 loadUserByUsername 方法获取用户详情，
 * 然后将用户输入的密码与数据库中存储的加密密码进行比对验证。
 * </p>
 * <p>
 * 加载的用户信息包括：
 * - username：用户名（登录账号）
 * - password：BCrypt 加密后的密码
 * - enabled：账户是否启用（status == 1 表示启用）
 * - authorities：用户角色权限
 * </p>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    /** 用户 Mapper，用于查询数据库中的用户信息 */
    private final SysUserMapper userMapper;

    /**
     * 根据用户名加载用户详情。
     * <p>
     * Spring Security 在认证过程中会自动调用此方法。
     * 从数据库查询用户信息，如果用户不存在则抛出异常。
     * </p>
     *
     * @param username 用户名（登录账号）
     * @return UserDetails 用户详情对象，包含用户名、密码、权限等信息
     * @throws UsernameNotFoundException 当用户不存在时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 根据用户名查询数据库
        SysUser sysUser = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );

        // 用户不存在时抛出异常
        if (sysUser == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 构建 Spring Security 的 UserDetails 对象
        return new User(
                sysUser.getUsername(),                      // 用户名
                sysUser.getPassword(),                      // 加密后的密码
                sysUser.getStatus() == 1,                   // 账户是否启用（status=1 为启用）
                true,                                       // 账户是否未过期
                true,                                       // 凭证是否未过期
                true,                                       // 账户是否未锁定
                // 用户角色权限，加 "ROLE_" 前缀以符合 Spring Security 的角色命名约定
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + sysUser.getRole()))
        );
    }
}
