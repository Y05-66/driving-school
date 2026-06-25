package com.drivingschool.notification.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.common.result.PageResult;
import com.drivingschool.common.result.R;
import com.drivingschool.notification.entity.Notification;
import com.drivingschool.notification.service.NotificationService;
import com.drivingschool.system.entity.SysUser;
import com.drivingschool.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 通知公告控制器
 * <p>
 * 功能说明：管理系统通知和公告的发布与查看，包括通知列表查询、未读数量统计、
 * 标记已读、删除通知以及管理员发布通知等功能。
 * 通知支持按角色定向推送，不同角色看到的通知内容可能不同。
 * </p>
 * 基础路径：/notifications
 * <p>
 * 接口权限说明：
 * - 通知列表、未读数量、标记已读、删除：需要登录（根据角色返回对应通知）
 * - 发布通知、通知管理列表：需要ADMIN或STAFF角色
 * </p>
 *
 * @author drivingschool
 */
@Tag(name = "通知公告")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    /** 通知服务，处理通知的核心业务逻辑 */
    private final NotificationService notificationService;

    /** 用户服务，用于查询当前用户信息以确定角色 */
    private final SysUserService sysUserService;

    /**
     * 我的通知列表接口
     * <p>
     * 分页查询当前用户的通知列表，根据用户角色返回对应的通知。
     * 支持按通知类型筛选。
     * </p>
     *
     * @param authentication Spring Security认证对象，包含当前用户身份信息
     * @param type           通知类型筛选（可选）
     * @param pageNum        页码，默认第1页
     * @param pageSize       每页条数，默认10条
     * @return 分页后的通知列表（PageResult格式）
     * @summary 查询我的通知列表
     * @权限要求 需要登录（根据角色返回对应通知）
     */
    @Operation(summary = "我的通知列表")
    @GetMapping
    public R<PageResult<Notification>> myNotifications(
            Authentication authentication,
            @RequestParam(required = false) Integer type,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        // 获取当前用户ID和角色
        Long userId = (Long) authentication.getPrincipal();
        SysUser user = sysUserService.getById(userId);
        String role = user != null ? user.getRole() : "STUDENT";
        // 分页查询该用户的通知
        Page<Notification> result = notificationService.pageByUser(userId, role, type, pageNum, pageSize);
        return R.ok(PageResult.of(result));
    }

    /**
     * 未读通知数量接口
     * <p>
     * 查询当前用户的未读通知数量，用于前端显示红点或角标。
     * </p>
     *
     * @param authentication Spring Security认证对象，包含当前用户身份信息
     * @return 包含未读通知数量的Map（key为"count"）
     * @summary 查询未读通知数量
     * @权限要求 需要登录
     */
    @Operation(summary = "未读通知数量")
    @GetMapping("/unread-count")
    public R<Map<String, Long>> unreadCount(Authentication authentication) {
        // 获取当前用户ID和角色
        Long userId = (Long) authentication.getPrincipal();
        SysUser user = sysUserService.getById(userId);
        String role = user != null ? user.getRole() : "STUDENT";
        // 统计未读通知数量
        long count = notificationService.countUnread(userId, role);
        Map<String, Long> data = new HashMap<>();
        data.put("count", count);
        return R.ok(data);
    }

    /**
     * 标记已读接口
     * <p>
     * 将指定通知标记为已读状态。
     * </p>
     *
     * @param id 通知ID（路径参数）
     * @return 操作成功返回R.ok()
     * @summary 标记通知为已读
     * @权限要求 需要登录
     */
    @Operation(summary = "标记已读")
    @PutMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Notification notification = notificationService.getById(id);
        if (notification == null) {
            return R.fail(404, "通知不存在");
        }
        // 归属校验：只有目标用户/角色才能将自己的通知标记为已读
        boolean hasAccess = false;
        if (notification.getTargetUserId() != null) {
            // 指定用户的通知：只有该用户可操作
            hasAccess = notification.getTargetUserId().equals(userId);
        } else if (notification.getTargetRole() != null) {
            // 按角色推送的通知：检查当前用户角色是否匹配
            SysUser user = sysUserService.getById(userId);
            String role = user != null ? user.getRole() : "STUDENT";
            hasAccess = "ALL".equals(notification.getTargetRole()) || role.equals(notification.getTargetRole());
        }
        if (!hasAccess) {
            return R.fail(403, "无权操作此通知");
        }
        notificationService.markRead(id);
        return R.ok();
    }

    /**
     * 全部标记已读接口
     * <p>
     * 将当前用户的所有未读通知标记为已读状态。
     * </p>
     *
     * @param authentication Spring Security认证对象，包含当前用户身份信息
     * @return 操作成功返回R.ok()
     * @summary 全部标记已读
     * @权限要求 需要登录
     */
    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public R<Void> markAllRead(Authentication authentication) {
        // 获取当前用户ID和角色
        Long userId = (Long) authentication.getPrincipal();
        SysUser user = sysUserService.getById(userId);
        String role = user != null ? user.getRole() : "STUDENT";
        // 标记该用户的所有通知为已读
        notificationService.markAllRead(userId, role);
        return R.ok();
    }

    /**
     * 删除通知接口
     * <p>
     * 删除指定的通知记录。
     * </p>
     *
     * @param id 通知ID（路径参数）
     * @return 操作成功返回R.ok()
     * @summary 删除通知
     * @权限要求 需要登录
     */
    @Operation(summary = "删除通知")
    @DeleteMapping("/{id}")
    // 仅管理员和前台可删除通知，防止普通用户误删或恶意删除
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<Void> delete(@PathVariable Long id) {
        notificationService.delete(id);
        return R.ok();
    }

    /**
     * 发布通知接口
     * <p>
     * 管理员或前台发布新的通知公告，自动设置发布者为当前用户。
     * 通知可设置目标角色，实现定向推送。
     * </p>
     *
     * @param notification   通知实体对象，包含通知标题、内容、类型、目标角色等
     * @param authentication Spring Security认证对象，用于获取发布者ID
     * @return 操作成功返回R.ok()
     * @summary 发布通知公告
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "发布通知（管理员）")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<Void> publish(@RequestBody Notification notification, Authentication authentication) {
        // 设置发布者ID为当前登录用户
        Long userId = (Long) authentication.getPrincipal();
        notification.setPublisherId(userId);
        notificationService.create(notification);
        return R.ok();
    }

    /**
     * 通知管理列表接口（管理员端）
     * <p>
     * 管理员查看所有已发布的通知列表，支持按类型和目标角色筛选。
     * 用于通知的管理和维护。
     * </p>
     *
     * @param type       通知类型筛选（可选）
     * @param targetRole 目标角色筛选（可选，如ALL、STUDENT、COACH等）
     * @param pageNum    页码，默认第1页
     * @param pageSize   每页条数，默认10条
     * @return 分页后的通知列表（PageResult格式）
     * @summary 查询通知管理列表
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "通知管理列表（管理员）")
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<PageResult<Notification>> adminList(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String targetRole,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        // 分页查询所有通知
        Page<Notification> result = notificationService.pageAll(type, targetRole, pageNum, pageSize);
        return R.ok(PageResult.of(result));
    }
}
