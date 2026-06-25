package com.drivingschool.coach.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drivingschool.coach.entity.CoachSchedule;
import com.drivingschool.coach.mapper.CoachScheduleMapper;
import com.drivingschool.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 教练排班服务类
 */
@Service
@RequiredArgsConstructor
public class CoachScheduleService extends ServiceImpl<CoachScheduleMapper, CoachSchedule> {

    /**
     * 教练添加排班
     */
    @Transactional(rollbackFor = Exception.class)
    public CoachSchedule addSchedule(CoachSchedule schedule) {
        // 检查时间段是否冲突
        Long count = count(new LambdaQueryWrapper<CoachSchedule>()
                .eq(CoachSchedule::getCoachId, schedule.getCoachId())
                .eq(CoachSchedule::getScheduleDate, schedule.getScheduleDate())
                .lt(CoachSchedule::getStartTime, schedule.getEndTime())
                .gt(CoachSchedule::getEndTime, schedule.getStartTime()));
        if (count > 0) {
            throw new BusinessException("该时间段与已有排班冲突");
        }

        schedule.setBookedCount(0);
        schedule.setStatus(0);
        save(schedule);
        return schedule;
    }

    /**
     * 教练批量添加排班（按周）
     */
    @Transactional(rollbackFor = Exception.class)
    public void addWeeklySchedule(Long coachId, LocalDate startDate, LocalDate endDate,
                                   LocalTime startTime, LocalTime endTime, Integer maxStudents) {
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            CoachSchedule schedule = new CoachSchedule();
            schedule.setCoachId(coachId);
            schedule.setScheduleDate(current);
            schedule.setStartTime(startTime);
            schedule.setEndTime(endTime);
            schedule.setMaxStudents(maxStudents != null ? maxStudents : 1);
            schedule.setBookedCount(0);
            schedule.setStatus(0);

            // 跳过冲突的日期
            Long count = count(new LambdaQueryWrapper<CoachSchedule>()
                    .eq(CoachSchedule::getCoachId, coachId)
                    .eq(CoachSchedule::getScheduleDate, current)
                    .lt(CoachSchedule::getStartTime, endTime)
                    .gt(CoachSchedule::getEndTime, startTime));
            if (count == 0) {
                save(schedule);
            }
            current = current.plusDays(1);
        }
    }

    /**
     * 获取教练指定日期的排班
     */
    public List<CoachSchedule> getByCoachAndDate(Long coachId, LocalDate date) {
        return list(new LambdaQueryWrapper<CoachSchedule>()
                .eq(CoachSchedule::getCoachId, coachId)
                .eq(CoachSchedule::getScheduleDate, date)
                .orderByAsc(CoachSchedule::getStartTime));
    }

    /**
     * 获取教练的可用排班（未来7天）
     */
    public List<CoachSchedule> getAvailableSlots(Long coachId) {
        return list(new LambdaQueryWrapper<CoachSchedule>()
                .eq(CoachSchedule::getCoachId, coachId)
                .ge(CoachSchedule::getScheduleDate, LocalDate.now())
                .le(CoachSchedule::getScheduleDate, LocalDate.now().plusDays(7))
                .eq(CoachSchedule::getStatus, 0)
                .orderByAsc(CoachSchedule::getScheduleDate)
                .orderByAsc(CoachSchedule::getStartTime));
    }

    /**
     * 预约排班时段（乐观锁，防止并发超卖）
     */
    @Transactional(rollbackFor = Exception.class)
    public void bookSlot(Long scheduleId) {
        // 先检查排班是否存在且可预约
        CoachSchedule schedule = getById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("排班时段不存在");
        }
        if (schedule.getStatus() != 0) {
            throw new BusinessException("该时段不可预约");
        }

        // 使用条件更新（乐观锁）：仅当 bookedCount < maxStudents 时才 +1
        boolean success = update(new LambdaUpdateWrapper<CoachSchedule>()
                .eq(CoachSchedule::getId, scheduleId)
                .eq(CoachSchedule::getStatus, 0)
                .lt(CoachSchedule::getBookedCount, schedule.getMaxStudents())
                .setSql("booked_count = booked_count + 1"));
        if (!success) {
            throw new BusinessException("该时段已满或状态已变更，请重试");
        }

        // 如果 +1 后已满，更新状态为已满
        CoachSchedule updated = getById(scheduleId);
        if (updated != null && updated.getBookedCount() >= updated.getMaxStudents()) {
            update(new LambdaUpdateWrapper<CoachSchedule>()
                    .eq(CoachSchedule::getId, scheduleId)
                    .set(CoachSchedule::getStatus, 1));
        }
    }

    /**
     * 取消预约（乐观锁，防止并发计数错误）
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelBooking(Long scheduleId) {
        CoachSchedule schedule = getById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("排班时段不存在");
        }

        // 使用条件更新：仅当 bookedCount > 0 时才 -1
        boolean success = update(new LambdaUpdateWrapper<CoachSchedule>()
                .eq(CoachSchedule::getId, scheduleId)
                .gt(CoachSchedule::getBookedCount, 0)
                .setSql("booked_count = booked_count - 1"));
        if (!success) {
            throw new BusinessException("取消失败，预约数已为0");
        }

        // 恢复状态为可用
        update(new LambdaUpdateWrapper<CoachSchedule>()
                .eq(CoachSchedule::getId, scheduleId)
                .eq(CoachSchedule::getStatus, 1)
                .set(CoachSchedule::getStatus, 0));
    }

    /**
     * 教练关闭排班时段
     */
    @Transactional(rollbackFor = Exception.class)
    public void closeSchedule(Long scheduleId, Long coachId) {
        CoachSchedule schedule = getById(scheduleId);
        if (schedule == null || !schedule.getCoachId().equals(coachId)) {
            throw new BusinessException("排班记录不存在");
        }
        schedule.setStatus(2);
        updateById(schedule);
    }

    /**
     * 删除排班
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSchedule(Long scheduleId, Long coachId) {
        CoachSchedule schedule = getById(scheduleId);
        if (schedule == null || !schedule.getCoachId().equals(coachId)) {
            throw new BusinessException("排班记录不存在");
        }
        if (schedule.getBookedCount() > 0) {
            throw new BusinessException("该时段已有预约，不可删除");
        }
        removeById(scheduleId);
    }
}
