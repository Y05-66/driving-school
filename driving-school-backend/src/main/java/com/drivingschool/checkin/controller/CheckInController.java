package com.drivingschool.checkin.controller;

import com.drivingschool.checkin.entity.CheckIn;
import com.drivingschool.checkin.service.CheckInService;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.common.result.R;
import com.drivingschool.student.entity.Student;
import com.drivingschool.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学时签到控制器
 * <p>
 * 提供签到签退相关的RESTful API接口，包括签到、签退、查询签到记录等操作。
 * </p>
 *
 * @author drivingschool
 * @since 1.0.0
 */
@Tag(name = "学时签到")
@RestController
@RequestMapping("/check-in")
@RequiredArgsConstructor
public class CheckInController {

    /** 签到服务，处理签到签退的核心业务逻辑 */
    private final CheckInService checkInService;
    /** 学员服务，用于根据用户ID查找学员信息（IDOR防护） */
    private final StudentService studentService;

    /**
     * 签到请求对象
     */
    @Data
    public static class CheckInRequest {
        /** 学员ID */
        @NotNull(message = "studentId不能为空")
        private Long studentId;
        /** 教练ID（可选） */
        private Long coachId;
        /** 签到位置（可选） */
        private String location;
        /** 签到类型：1-手动签到，2-扫码签到（默认1） */
        private Integer checkInType = 1;
    }

    /**
     * 签退请求对象
     */
    @Data
    public static class CheckOutRequest {
        /** 学员ID */
        @NotNull(message = "studentId不能为空")
        private Long studentId;
    }

    /**
     * 学员签到
     * <p>
     * 为指定课程创建签到记录，支持扫码签到和手动签到两种方式。
     * </p>
     *
     * @param lessonId 课程ID（路径参数）
     * @param request  签到参数
     * @return 签到记录
     */
    @Operation(summary = "学员签到")
    @PostMapping("/{lessonId}/check-in")
    @PreAuthorize("hasAnyRole('STUDENT','COACH','STAFF','ADMIN')")
    public R<CheckIn> checkIn(@PathVariable Long lessonId, @Valid @RequestBody CheckInRequest request,
                              Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        // STUDENT 角色：自动使用当前用户关联的学员ID，防止 IDOR
        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        if (isStudent) {
            Student student = studentService.getByUserId(userId);
            if (student == null || !student.getId().equals(request.getStudentId())) {
                throw new BusinessException("无权为其他学员签到");
            }
        }
        CheckIn checkIn = checkInService.checkIn(lessonId, request.getStudentId(), request.getCoachId(),
                request.getLocation(), request.getCheckInType());
        return R.ok(checkIn);
    }

    /**
     * 学员签退
     * <p>
     * 更新签到记录，记录签退时间。
     * 只有状态为"已签到"的记录才能签退。
     * </p>
     *
     * @param lessonId 课程ID（路径参数）
     * @param request  签退参数
     * @return 签退后的签到记录
     */
    @Operation(summary = "学员签退")
    @PutMapping("/{lessonId}/check-out")
    @PreAuthorize("hasAnyRole('STUDENT','COACH','STAFF','ADMIN')")
    public R<CheckIn> checkOut(@PathVariable Long lessonId, @Valid @RequestBody CheckOutRequest request,
                               Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        // STUDENT 角色：自动使用当前用户关联的学员ID，防止 IDOR
        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        if (isStudent) {
            Student student = studentService.getByUserId(userId);
            if (student == null || !student.getId().equals(request.getStudentId())) {
                throw new BusinessException("无权为其他学员签退");
            }
        }
        CheckIn checkIn = checkInService.checkOut(lessonId, request.getStudentId());
        return R.ok(checkIn);
    }

    /**
     * 获取课程签到记录
     * <p>
     * 查询指定课程的所有签到记录，按签到时间倒序排列。
     * </p>
     *
     * @param lessonId 课程ID
     * @return 签到记录列表
     */
    @Operation(summary = "获取课程签到记录")
    @GetMapping("/{lessonId}")
    @PreAuthorize("hasAnyRole('COACH','STAFF','ADMIN')")
    public R<List<CheckIn>> getByLessonId(@PathVariable Long lessonId) {
        List<CheckIn> list = checkInService.getByLessonId(lessonId);
        return R.ok(list);
    }
}
