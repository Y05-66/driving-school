package com.drivingschool.reminder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.reminder.entity.Reminder;
import com.drivingschool.reminder.mapper.ReminderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自动提醒服务类
 * <p>
 * 提供提醒的核心业务逻辑，包括提醒创建、取消、查询、标记已发送等功能。
 * 支持定时任务调用getPendingReminders获取待发送提醒。
 * </p>
 *
 * @author drivingschool
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ReminderService extends ServiceImpl<ReminderMapper, Reminder> {

    /**
     * 创建提醒
     * <p>
     * 创建新的提醒记录，设置初始状态为待发送(0)。
     * </p>
     *
     * @param reminder 提醒信息（id、status、createTime、updateTime由系统自动设置）
     * @return 创建成功的提醒信息
     * @throws BusinessException 如果必填参数缺失
     */
    @Transactional(rollbackFor = Exception.class)
    public Reminder create(Reminder reminder) {
        // 参数校验
        if (reminder.getTargetUserId() == null) {
            throw new BusinessException("目标用户ID不能为空");
        }
        if (reminder.getType() == null) {
            throw new BusinessException("提醒类型不能为空");
        }
        if (reminder.getTitle() == null || reminder.getTitle().isEmpty()) {
            throw new BusinessException("提醒标题不能为空");
        }
        if (reminder.getRemindTime() == null) {
            throw new BusinessException("提醒时间不能为空");
        }

        // 设置初始状态为待发送
        reminder.setStatus(0);

        // 保存提醒
        this.save(reminder);
        return reminder;
    }

    /**
     * 取消提醒
     * <p>
     * 将指定提醒的状态变更为已取消(2)。
     * 只有状态为"待发送"的提醒才能被取消。
     * </p>
     *
     * @param reminderId 提醒ID
     * @return 取消后的提醒信息
     * @throws BusinessException 如果提醒不存在或状态不允许取消
     */
    @Transactional(rollbackFor = Exception.class)
    public Reminder cancel(Long reminderId) {
        // 查询提醒
        Reminder reminder = this.getById(reminderId);
        if (reminder == null) {
            throw new BusinessException("提醒不存在");
        }

        // 校验状态
        if (reminder.getStatus() != 0) {
            throw new BusinessException("只有待发送的提醒才能取消");
        }

        // 更新状态为已取消
        reminder.setStatus(2);
        this.updateById(reminder);

        return reminder;
    }

    /**
     * 根据用户ID获取提醒列表
     * <p>
     * 查询指定用户的所有提醒，按提醒时间倒序排列。
     * </p>
     *
     * @param userId 用户ID
     * @return 提醒列表
     */
    public List<Reminder> getByUserId(Long userId) {
        LambdaQueryWrapper<Reminder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reminder::getTargetUserId, userId)
               .orderByDesc(Reminder::getRemindTime);
        return this.list(wrapper);
    }

    /**
     * 获取待发送的提醒列表
     * <p>
     * 查询所有状态为待发送(0)且提醒时间已到达的提醒。
     * 通常由定时任务调用，获取需要发送的提醒并进行处理。
     * </p>
     *
     * @return 待发送提醒列表
     */
    public List<Reminder> getPendingReminders() {
        LambdaQueryWrapper<Reminder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reminder::getStatus, 0)
               .le(Reminder::getRemindTime, LocalDateTime.now())
               .orderByAsc(Reminder::getRemindTime);
        return this.list(wrapper);
    }

    /**
     * 标记提醒为已发送
     * <p>
     * 将指定提醒的状态变更为已发送(1)。
     * 通常在提醒消息成功发送后调用。
     * </p>
     *
     * @param reminderId 提醒ID
     * @return 标记后的提醒信息
     * @throws BusinessException 如果提醒不存在或状态不允许标记
     */
    @Transactional(rollbackFor = Exception.class)
    public Reminder markSent(Long reminderId) {
        // 查询提醒
        Reminder reminder = this.getById(reminderId);
        if (reminder == null) {
            throw new BusinessException("提醒不存在");
        }

        // 校验状态
        if (reminder.getStatus() != 0) {
            throw new BusinessException("只有待发送的提醒才能标记为已发送");
        }

        // 更新状态为已发送
        reminder.setStatus(1);
        this.updateById(reminder);

        return reminder;
    }
}
