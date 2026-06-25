package com.drivingschool.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.notification.entity.Notification;
import com.drivingschool.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知管理服务类
 * <p>
 * 负责系统通知的管理，支持以下功能：
 * - 通知发布（定向通知和广播通知）
 * - 用户通知列表查询（包含定向通知、角色广播、全局广播）
 * - 标记已读（单条和全部）
 * - 未读通知数量统计
 * - 通知删除
 * - 管理端通知查询
 * </p>
 * <p>
 * 通知投递方式说明：
 * - 定向通知：通过targetUserId指定接收用户
 * - 角色广播：通过targetRole指定接收角色（如"STUDENT"、"COACH"）
 * - 全局广播：targetRole设为"ALL"，所有用户可见
 * </p>
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    /** 通知数据访问层 */
    private final NotificationMapper notificationMapper;

    /**
     * 发布通知
     * <p>
     * 新通知默认为未读状态(isRead=0)
     * </p>
     *
     * @param notification 通知实体（包含标题、内容、目标用户/角色等）
     */
    public void create(Notification notification) {
        notification.setIsRead(0);
        notificationMapper.insert(notification);
    }

    /**
     * 获取用户通知列表（包含定向通知和广播通知）
     * <p>
     * 查询逻辑（OR关系）：
     * 1. 定向发送给该用户的通知（targetUserId = userId）
     * 2. 发送给该用户角色的广播通知（targetRole = role）
     * 3. 全局广播通知（targetRole = "ALL"）
     * </p>
     *
     * @param userId   当前用户ID
     * @param role     当前用户角色（如"STUDENT"、"COACH"、"ADMIN"等）
     * @param type     通知类型（可选筛选条件）
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 分页结果，按创建时间降序排列
     */
    public Page<Notification> pageByUser(Long userId, String role, Integer type, int pageNum, int pageSize) {
        Page<Notification> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        // 组合查询条件：定向通知 OR 角色广播 OR 全局广播
        wrapper.and(w -> w
                .eq(Notification::getTargetUserId, userId)
                .or()
                .eq(Notification::getTargetRole, "ALL")
                .or()
                .eq(Notification::getTargetRole, role)
        );
        wrapper.eq(type != null, Notification::getType, type);
        wrapper.orderByDesc(Notification::getCreateTime);
        return notificationMapper.selectPage(page, wrapper);
    }

    /**
     * 根据ID获取通知
     *
     * @param id 通知ID
     * @return 通知实体，不存在时返回null
     */
    public Notification getById(Long id) {
        return notificationMapper.selectById(id);
    }

    /**
     * 标记单条通知为已读
     *
     * @param id 通知ID
     * @throws BusinessException 通知不存在时抛出
     */
    public void markRead(Long id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw new BusinessException("通知不存在");
        }
        notification.setIsRead(1);
        notificationMapper.updateById(notification);
    }

    /**
     * 将用户的所有未读通知标记为已读
     * <p>
     * 查询逻辑与pageByUser一致（定向 + 角色广播 + 全局广播），
     * 将其中未读的通知逐一标记为已读
     * </p>
     *
     * @param userId 当前用户ID
     * @param role   当前用户角色
     */
    public void markAllRead(Long userId, String role) {
        // 使用单条 UPDATE 批量标记，替代 N+1 查询
        notificationMapper.update(null,
                new LambdaUpdateWrapper<Notification>()
                        .set(Notification::getIsRead, 1)
                        .eq(Notification::getIsRead, 0)
                        .and(w -> w
                                .eq(Notification::getTargetUserId, userId)
                                .or()
                                .eq(Notification::getTargetRole, "ALL")
                                .or()
                                .eq(Notification::getTargetRole, role)
                        )
        );
    }

    /**
     * 获取用户的未读通知数量
     *
     * @param userId 用户ID
     * @param role   用户角色
     * @return 未读通知数量
     */
    public long countUnread(Long userId, String role) {
        return notificationMapper.countUnread(userId, role);
    }

    /**
     * 删除通知
     * <p>
     * 注意：当前实现为物理删除，如需逻辑删除请修改为更新删除标记字段
     * </p>
     *
     * @param id 通知ID
     */
    public void delete(Long id) {
        notificationMapper.deleteById(id);
    }

    /**
     * 管理端查询所有通知（支持按类型和目标角色筛选）
     *
     * @param type       通知类型（可选筛选条件）
     * @param targetRole 目标角色（可选筛选条件）
     * @param pageNum    页码
     * @param pageSize   每页数量
     * @return 分页结果，按创建时间降序排列
     */
    public Page<Notification> pageAll(Integer type, String targetRole, int pageNum, int pageSize) {
        Page<Notification> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(type != null, Notification::getType, type)
                .eq(targetRole != null, Notification::getTargetRole, targetRole)
                .orderByDesc(Notification::getCreateTime);
        return notificationMapper.selectPage(page, wrapper);
    }
}
