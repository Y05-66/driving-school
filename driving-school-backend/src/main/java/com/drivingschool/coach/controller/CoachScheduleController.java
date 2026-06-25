package com.drivingschool.coach.controller;

import com.drivingschool.coach.entity.Coach;
import com.drivingschool.coach.entity.CoachSchedule;
import com.drivingschool.coach.service.CoachScheduleService;
import com.drivingschool.coach.service.CoachService;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 教练排班控制器
 */
@Tag(name = "教练排班")
@RestController
@RequestMapping("/coach-schedules")
@RequiredArgsConstructor
public class CoachScheduleController {

    private final CoachScheduleService scheduleService;
    private final CoachService coachService;

    @Data
    public static class AddScheduleRequest {
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate scheduleDate;
        @DateTimeFormat(pattern = "HH:mm")
        private LocalTime startTime;
        @DateTimeFormat(pattern = "HH:mm")
        private LocalTime endTime;
        private Integer maxStudents;
        private String remark;
    }

    @Data
    public static class BatchScheduleRequest {
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate startDate;
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;
        @DateTimeFormat(pattern = "HH:mm")
        private LocalTime startTime;
        @DateTimeFormat(pattern = "HH:mm")
        private LocalTime endTime;
        private Integer maxStudents;
    }

    /**
     * 教练添加单个排班
     */
    @Operation(summary = "添加排班")
    @PostMapping
    @PreAuthorize("hasRole('COACH')")
    public R<CoachSchedule> addSchedule(Authentication authentication, @RequestBody AddScheduleRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        Coach coach = coachService.getByUserId(userId);
        if (coach == null) {
            throw new BusinessException("当前用户未关联教练信息");
        }

        CoachSchedule schedule = new CoachSchedule();
        schedule.setCoachId(coach.getId());
        schedule.setScheduleDate(request.getScheduleDate());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setMaxStudents(request.getMaxStudents() != null ? request.getMaxStudents() : 1);
        schedule.setRemark(request.getRemark());
        return R.ok(scheduleService.addSchedule(schedule));
    }

    /**
     * 教练批量添加排班
     */
    @Operation(summary = "批量排班")
    @PostMapping("/batch")
    @PreAuthorize("hasRole('COACH')")
    public R<Void> batchSchedule(Authentication authentication, @RequestBody BatchScheduleRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        Coach coach = coachService.getByUserId(userId);
        if (coach == null) {
            throw new BusinessException("当前用户未关联教练信息");
        }
        scheduleService.addWeeklySchedule(coach.getId(), request.getStartDate(), request.getEndDate(),
                request.getStartTime(), request.getEndTime(), request.getMaxStudents());
        return R.ok();
    }

    /**
     * 获取我的排班列表
     */
    @Operation(summary = "我的排班")
    @GetMapping("/my")
    @PreAuthorize("hasRole('COACH')")
    public R<List<CoachSchedule>> mySchedules(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Coach coach = coachService.getByUserId(userId);
        if (coach == null) {
            return R.ok(List.of());
        }
        return R.ok(scheduleService.getAvailableSlots(coach.getId()));
    }

    /**
     * 获取指定教练的可用排班（学员端）
     */
    @Operation(summary = "教练可用时段")
    @GetMapping("/coach/{coachId}")
    public R<List<CoachSchedule>> getCoachSlots(@PathVariable Long coachId) {
        return R.ok(scheduleService.getAvailableSlots(coachId));
    }

    /**
     * 删除排班
     */
    @Operation(summary = "删除排班")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COACH')")
    public R<Void> deleteSchedule(Authentication authentication, @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        Coach coach = coachService.getByUserId(userId);
        if (coach == null) {
            throw new BusinessException("当前用户未关联教练信息");
        }
        scheduleService.deleteSchedule(id, coach.getId());
        return R.ok();
    }
}
