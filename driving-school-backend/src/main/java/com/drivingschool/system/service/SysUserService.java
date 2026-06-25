package com.drivingschool.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.system.entity.SysUser;
import com.drivingschool.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统用户管理服务类
 * <p>
 * 负责系统用户的账号管理，包括：
 * - 用户创建（含用户名唯一性校验、密码加密）
 * - 用户信息查询（按ID、用户名）
 * - 用户列表分页查询
 * - 用户信息更新（更新时自动排除密码字段，防止误覆盖）
 * - 密码重置（使用Spring Security的PasswordEncoder加密）
 * - 用户状态管理（启用/禁用）
 * </p>
 * <p>
 * 安全说明：
 * - 密码存储使用PasswordEncoder进行加密（如BCrypt）
 * - 更新用户信息时自动将password置null，避免未加密密码覆盖已加密密码
 * </p>
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    /** 系统用户数据访问层 */
    private final SysUserMapper userMapper;
    /** 密码编码器（用于密码加密和验证） */
    private final PasswordEncoder passwordEncoder;

    /**
     * 根据用户名查询用户（用于登录认证等场景）
     *
     * @param username 用户名
     * @return 用户实体，不存在时返回null
     */
    public SysUser getByUsername(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
    }

    /**
     * 根据ID获取用户详情
     *
     * @param id 用户ID
     * @return 用户实体，不存在时返回null
     */
    public SysUser getById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 分页查询用户列表
     *
     * @param page     分页参数对象
     * @param username 用户名（模糊查询，可选）
     * @param role     角色（可选，如"ADMIN"、"STAFF"、"COACH"、"STUDENT"）
     * @param status   用户状态（可选：1-启用 0-禁用）
     * @return 分页结果，按创建时间降序排列
     */
    public Page<SysUser> page(Page<SysUser> page, String username, String role, Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(username != null, SysUser::getUsername, username)
                .eq(role != null, SysUser::getRole, role)
                .eq(status != null, SysUser::getStatus, status)
                .orderByDesc(SysUser::getCreateTime);
        return userMapper.selectPage(page, wrapper);
    }

    /**
     * 创建用户
     * <p>
     * 业务规则：
     * 1. 用户名全局唯一，不能重复创建
     * 2. 密码使用PasswordEncoder加密后存储
     * 3. 新建用户默认状态为1（启用）
     * </p>
     *
     * @param user 用户实体（密码字段应为明文，本方法会自动加密）
     * @throws BusinessException 用户名已存在时抛出（错误码1001）
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(SysUser user) {
        // 校验用户名唯一性
        SysUser existing = getByUsername(user.getUsername());
        if (existing != null) {
            throw new BusinessException(1001, "用户名已存在");
        }
        // 校验密码不为空
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new BusinessException("密码不能为空");
        }
        // 密码加密后存储
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        userMapper.insert(user);
    }

    /**
     * 更新用户信息
     * <p>
     * 安全措施：将password字段置为null后执行更新，
     * 防止未加密的密码覆盖数据库中已加密的密码。
     * 如需修改密码，请使用resetPassword方法。
     * </p>
     *
     * @param user 用户实体（需包含ID，password字段会被忽略）
     */
    public void update(SysUser user) {
        // 将密码置null，防止误覆盖已加密的密码
        user.setPassword(null);
        userMapper.updateById(user);
    }

    /**
     * 重置用户密码
     * <p>
     * 使用PasswordEncoder对新密码加密后更新到数据库
     * </p>
     *
     * @param id        用户ID
     * @param newPassword 新密码（明文）
     */
    public void resetPassword(Long id, String newPassword) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    /**
     * 更新用户状态（启用/禁用）
     *
     * @param id     用户ID
     * @param status 目标状态（1-启用 0-禁用）
     */
    public void updateStatus(Long id, Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        userMapper.updateById(user);
    }
}
