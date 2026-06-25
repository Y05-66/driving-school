package com.drivingschool.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.common.result.PageResult;
import com.drivingschool.common.result.R;
import com.drivingschool.system.entity.SysUser;
import com.drivingschool.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 * <p>
 * 功能说明：管理系统用户的增删改查操作，包括用户列表查询、创建用户、修改用户信息、
 * 重置用户密码以及变更用户状态（启用/禁用）等功能。
 * </p>
 * 基础路径：/users
 * <p>
 * 所有接口均需要ADMIN角色权限，仅管理员可操作。
 * </p>
 *
 * @author drivingschool
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    /** 用户服务，处理用户相关的业务逻辑 */
    private final SysUserService userService;

    /**
     * 用户查询条件对象
     * 用于接收前端传递的筛选和分页参数
     */
    @Data
    public static class UserQuery {
        /** 用户名（模糊查询） */
        private String username;
        /** 角色筛选（如 ADMIN、STAFF、COACH、STUDENT） */
        private String role;
        /** 状态筛选（1-启用，0-禁用） */
        private Integer status;
        /** 页码，默认第1页 */
        private Integer pageNum = 1;
        /** 每页条数，默认10条 */
        private Integer pageSize = 10;
    }

    /**
     * 重置密码请求对象
     * 用于管理员重置指定用户密码时接收新密码
     */
    @Data
    public static class ResetPasswordRequest {
        /** 新密码 */
        @NotBlank(message = "新密码不能为空")
        private String newPassword;
    }

    /**
     * 用户更新请求对象
     * 仅允许更新指定字段，防止批量赋值攻击
     */
    @Data
    public static class UserUpdateDTO {
        private String realName;
        private String phone;
        private String role;
        private String avatar;
    }

    /**
     * 用户列表接口
     * <p>
     * 分页查询系统用户列表，支持按用户名、角色、状态进行筛选。
     * </p>
     *
     * @param query 查询条件对象，包含用户名、角色、状态和分页参数
     * @return 分页后的用户列表（PageResult格式）
     * @summary 查询用户列表
     * @权限要求 仅管理员（ADMIN）
     */
    @Operation(summary = "用户列表")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<PageResult<SysUser>> list(UserQuery query) {
        // 构建分页对象
        Page<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        // 执行分页查询
        Page<SysUser> result = userService.page(page, query.getUsername(), query.getRole(), query.getStatus());
        return R.ok(PageResult.of(result));
    }

    /**
     * 创建用户接口
     * <p>
     * 管理员创建新的系统用户。
     * </p>
     *
     * @param user 用户实体对象，包含用户名、密码、角色等信息
     * @return 操作成功返回R.ok()
     * @summary 创建用户
     * @权限要求 仅管理员（ADMIN）
     */
    @Operation(summary = "创建用户")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> create(@Valid @RequestBody SysUser user) {
        userService.create(user);
        return R.ok();
    }

    /**
     * 修改用户信息接口
     * <p>
     * 根据用户ID修改用户的基本信息（如真实姓名、手机号、角色等）。
     * </p>
     *
     * @param id   用户ID（路径参数）
     * @param user 用户实体对象，包含需要更新的字段
     * @return 操作成功返回R.ok()
     * @summary 修改用户信息
     * @权限要求 仅管理员（ADMIN）
     */
    @Operation(summary = "修改用户")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> update(@PathVariable Long id, @RequestBody UserUpdateDTO dto) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setRole(dto.getRole());
        user.setAvatar(dto.getAvatar());
        userService.update(user);
        return R.ok();
    }

    /**
     * 重置用户密码接口
     * <p>
     * 管理员为指定用户重置密码，无需验证旧密码。
     * </p>
     *
     * @param id      用户ID（路径参数）
     * @param request 重置密码请求对象，包含新密码
     * @return 操作成功返回R.ok()
     * @summary 重置用户密码
     * @权限要求 仅管理员（ADMIN）
     */
    @Operation(summary = "重置密码")
    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request.getNewPassword());
        return R.ok();
    }

    /**
     * 变更用户状态接口
     * <p>
     * 启用或禁用指定用户。状态值：1-启用，0-禁用。
     * 禁用后用户将无法登录系统。
     * </p>
     *
     * @param id     用户ID（路径参数）
     * @param status 目标状态（1-启用，0-禁用）
     * @return 操作成功返回R.ok()
     * @summary 变更用户状态（启用/禁用）
     * @权限要求 仅管理员（ADMIN）
     */
    @Operation(summary = "变更状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        // 校验状态值合法性：仅允许0（禁用）或1（启用）
        if (status != 0 && status != 1) {
            return R.fail(400, "状态值只能为0或1");
        }
        userService.updateStatus(id, status);
        return R.ok();
    }
}
