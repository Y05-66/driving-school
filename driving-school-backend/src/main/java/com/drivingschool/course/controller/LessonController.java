package com.drivingschool.course.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.coach.entity.Coach;
import com.drivingschool.coach.service.CoachService;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.common.result.PageResult;
import com.drivingschool.common.result.R;
import com.drivingschool.course.entity.Lesson;
import com.drivingschool.course.entity.TimeSlotTemplate;
import com.drivingschool.course.service.LessonService;
import com.drivingschool.student.entity.Student;
import com.drivingschool.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 培训预约控制器
 * <p>
 * 功能说明：管理驾校培训课时的预约全流程，包括预约创建、确认、完成、取消等操作，
 * 以及日历视图、空闲时段查询、批量创建预约、时间段模板管理等功能。
 * </p>
 * 基础路径：/lessons
 * <p>
 * 接口权限说明：
 * - 预约列表：根据角色自动过滤数据（学员看自己的，教练看自己的，管理员/前台看全部）
 * - 创建预约：学员（只能为自己预约）、前台、管理员
 * - 确认/完成课时：教练（只能操作自己的）、前台、管理员
 * - 取消预约：学员（只能取消自己的）、教练（只能取消自己的）、前台、管理员
 * - 日历/时段查询：登录即可访问
 * - 批量创建和模板管理：前台、管理员（模板管理仅管理员）
 * </p>
 *
 * @author drivingschool
 */
@Tag(name = "培训预约")
@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
public class LessonController {

    /** 课时服务，处理课时预约的核心业务逻辑 */
    private final LessonService lessonService;

    /** 学员服务，用于查询学员信息 */
    private final StudentService studentService;

    /** 教练服务，用于查询教练信息 */
    private final CoachService coachService;

    /**
     * 课时查询条件对象
     * 用于接收前端传递的筛选和分页参数
     */
    @Data
    public static class LessonQuery {
        /** 学员ID筛选 */
        private Long studentId;
        /** 教练ID筛选 */
        private Long coachId;
        /** 车辆ID筛选 */
        private Long vehicleId;
        /** 状态筛选（0-待确认，1-已确认，2-进行中，3-已完成，4-已取消） */
        private Integer status;
        /** 课时日期筛选（格式：yyyy-MM-dd） */
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate lessonDate;
        /** 页码，默认第1页 */
        private Integer pageNum = 1;
        /** 每页条数，默认10条 */
        private Integer pageSize = 10;
    }

    /**
     * 完成课时请求对象
     * 用于教练/前台/管理员记录实际培训学时
     */
    @Data
    public static class CompleteRequest {
        /** 实际培训学时数 */
        private BigDecimal actualHours;
    }

    /**
     * 批量创建预约请求对象
     * 用于前台/管理员在多个日期批量创建相同配置的预约
     */
    @Data
    public static class BatchCreateRequest {
        /** 学员ID */
        private Long studentId;
        /** 教练ID */
        private Long coachId;
        /** 车辆ID */
        private Long vehicleId;
        /** 课程ID */
        private Long courseId;
        /** 开始时间 */
        private java.time.LocalTime startTime;
        /** 结束时间 */
        private java.time.LocalTime endTime;
        /** 预约日期列表（支持多天批量创建） */
        private List<LocalDate> dates;
        /** 备注信息 */
        private String remark;
    }

    /**
     * 预约列表查询接口
     * <p>
     * 分页查询课时预约列表，根据当前用户角色自动过滤数据：
     * - 学员：只能查看自己的预约
     * - 教练：只能查看自己的预约
     * - 管理员/前台：可查看所有预约
     * 支持按学员、教练、车辆、状态、日期筛选。
     * </p>
     *
     * @param query          查询条件对象
     * @param authentication Spring Security认证对象，包含当前用户身份和角色信息
     * @return 分页后的课时预约列表（PageResult格式）
     * @summary 查询预约列表
     * @权限要求 需要登录（根据角色自动过滤数据）
     */
    @Operation(summary = "预约列表")
    @GetMapping
    public R<PageResult<Lesson>> list(LessonQuery query, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        // 学员只能看自己的预约
        if (role.equals("ROLE_STUDENT")) {
            Student student = studentService.getByUserId(userId);
            if (student != null) {
                query.setStudentId(student.getId());
            }
        }
        // 教练只能看自己的预约
        else if (role.equals("ROLE_COACH")) {
            Coach coach = coachService.getByUserId(userId);
            if (coach != null) {
                query.setCoachId(coach.getId());
            }
        }

        // 构建分页对象并执行查询
        Page<Lesson> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<Lesson> result = lessonService.page(page, query.getStudentId(), query.getCoachId(),
                query.getVehicleId(), query.getStatus(), query.getLessonDate());
        return R.ok(PageResult.of(result));
    }

    /**
     * 创建预约接口
     * <p>
     * 创建新的课时预约。学员只能为自己创建预约，前台和管理员可为任意学员创建。
     * </p>
     *
     * @param lesson         课时实体对象，包含预约信息
     * @param authentication Spring Security认证对象，包含当前用户身份和角色信息
     * @return 操作成功返回R.ok()
     * @summary 创建培训预约
     * @权限要求 学员（STUDENT）、前台（STAFF）、管理员（ADMIN）
     */
    @Operation(summary = "创建预约（学员/前台/管理员）")
    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT','STAFF','ADMIN')")
    public R<Void> create(@RequestBody Lesson lesson, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        // 学员只能为自己创建预约，强制设置学员ID
        if (role.equals("ROLE_STUDENT")) {
            Student student = studentService.getByUserId(userId);
            if (student == null) {
                throw new BusinessException("学员信息不存在");
            }
            lesson.setStudentId(student.getId());
        }

        lessonService.create(lesson);
        return R.ok();
    }

    /**
     * 确认预约接口
     * <p>
     * 教练/前台/管理员确认课时预约。教练只能确认自己的课时。
     * </p>
     *
     * @param id             课时ID（路径参数）
     * @param authentication Spring Security认证对象，包含当前用户身份和角色信息
     * @return 操作成功返回R.ok()
     * @summary 确认课时预约
     * @权限要求 教练（COACH）、前台（STAFF）、管理员（ADMIN）
     * @throws BusinessException 教练操作非本人课时时抛出
     */
    @Operation(summary = "确认预约（教练/前台/管理员）")
    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('COACH','STAFF','ADMIN')")
    public R<Void> confirm(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        // 教练只能确认自己的课时，进行权限校验
        if (role.equals("ROLE_COACH")) {
            Coach coach = coachService.getByUserId(userId);
            Lesson lesson = lessonService.getById(id);
            if (coach == null || lesson == null || !coach.getId().equals(lesson.getCoachId())) {
                throw new BusinessException("无权操作此预约");
            }
        }

        lessonService.confirm(id);
        return R.ok();
    }

    /**
     * 完成课时接口
     * <p>
     * 教练/前台/管理员标记课时完成，并记录实际培训学时。教练只能操作自己的课时。
     * </p>
     *
     * @param id             课时ID（路径参数）
     * @param request        完成请求对象，包含实际培训学时
     * @param authentication Spring Security认证对象，包含当前用户身份和角色信息
     * @return 操作成功返回R.ok()
     * @summary 完成课时
     * @权限要求 教练（COACH）、前台（STAFF）、管理员（ADMIN）
     * @throws BusinessException 教练操作非本人课时时抛出
     */
    @Operation(summary = "完成课时（教练/前台/管理员）")
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('COACH','STAFF','ADMIN')")
    public R<Void> complete(@PathVariable Long id, @RequestBody CompleteRequest request,
                            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        // 教练只能完成自己的课时，进行权限校验
        if (role.equals("ROLE_COACH")) {
            Coach coach = coachService.getByUserId(userId);
            Lesson lesson = lessonService.getById(id);
            if (coach == null || lesson == null || !coach.getId().equals(lesson.getCoachId())) {
                throw new BusinessException("无权操作此预约");
            }
        }

        lessonService.complete(id, request.getActualHours());
        return R.ok();
    }

    /**
     * 确认并完成课时接口
     * <p>
     * 一步完成确认和标记完成操作，适用于教练现场确认并记录学时的场景。
     * 教练只能操作自己的课时。
     * </p>
     *
     * @param id             课时ID（路径参数）
     * @param request        完成请求对象，包含实际培训学时
     * @param authentication Spring Security认证对象，包含当前用户身份和角色信息
     * @return 操作成功返回R.ok()
     * @summary 确认并完成课时
     * @权限要求 教练（COACH）、前台（STAFF）、管理员（ADMIN）
     * @throws BusinessException 教练操作非本人课时时抛出
     */
    @Operation(summary = "确认并完成课时（教练/前台/管理员）")
    @PutMapping("/{id}/confirm-complete")
    @PreAuthorize("hasAnyRole('COACH','STAFF','ADMIN')")
    public R<Void> confirmAndComplete(@PathVariable Long id, @RequestBody CompleteRequest request,
                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        // 教练只能操作自己的课时，进行权限校验
        if (role.equals("ROLE_COACH")) {
            Coach coach = coachService.getByUserId(userId);
            Lesson lesson = lessonService.getById(id);
            if (coach == null || lesson == null || !coach.getId().equals(lesson.getCoachId())) {
                throw new BusinessException("无权操作此预约");
            }
        }

        lessonService.confirmAndComplete(id, request.getActualHours());
        return R.ok();
    }

    /**
     * 取消预约接口
     * <p>
     * 取消课时预约。学员只能取消自己的预约，教练只能取消自己的预约，
     * 前台和管理员可取消任意预约。
     * </p>
     *
     * @param id             课时ID（路径参数）
     * @param authentication Spring Security认证对象，包含当前用户身份和角色信息
     * @return 操作成功返回R.ok()
     * @summary 取消课时预约
     * @权限要求 学员（STUDENT）、教练（COACH）、前台（STAFF）、管理员（ADMIN）
     * @throws BusinessException 用户取消非本人课时时抛出
     */
    @Operation(summary = "取消预约")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('STUDENT','COACH','STAFF','ADMIN')")
    public R<Void> cancel(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        // 学员只能取消自己的预约
        if (role.equals("ROLE_STUDENT")) {
            Student student = studentService.getByUserId(userId);
            Lesson lesson = lessonService.getById(id);
            if (student == null || lesson == null || !student.getId().equals(lesson.getStudentId())) {
                throw new BusinessException("无权取消此预约");
            }
        }
        // 教练只能取消自己的预约
        else if (role.equals("ROLE_COACH")) {
            Coach coach = coachService.getByUserId(userId);
            Lesson lesson = lessonService.getById(id);
            if (coach == null || lesson == null || !coach.getId().equals(lesson.getCoachId())) {
                throw new BusinessException("无权取消此预约");
            }
        }

        lessonService.cancel(id);
        return R.ok();
    }

    /**
     * 日历视图数据接口
     * <p>
     * 查询指定日期范围内的课时预约数据，用于日历视图展示。
     * 学员只看自己的日历，教练只看自己的日历，管理员/前台可查看所有。
     * </p>
     *
     * @param startDate      开始日期（格式：yyyy-MM-dd）
     * @param endDate        结束日期（格式：yyyy-MM-dd）
     * @param coachId        教练ID筛选（可选）
     * @param studentId      学员ID筛选（可选）
     * @param authentication Spring Security认证对象，包含当前用户身份和角色信息
     * @return 指定日期范围内的课时预约列表
     * @summary 查询日历视图数据
     * @权限要求 需要登录（根据角色自动过滤数据）
     */
    @Operation(summary = "日历视图数据")
    @GetMapping("/calendar")
    public R<List<Lesson>> calendar(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) Long coachId,
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        // 学员只看自己的日历
        if (role.equals("ROLE_STUDENT")) {
            Student student = studentService.getByUserId(userId);
            if (student != null) studentId = student.getId();
        }
        // 教练只看自己的日历
        else if (role.equals("ROLE_COACH")) {
            Coach coach = coachService.getByUserId(userId);
            if (coach != null) coachId = coach.getId();
        }

        return R.ok(lessonService.getCalendarData(startDate, endDate, coachId, studentId));
    }

    /**
     * 查询空闲时段接口
     * <p>
     * 查询指定教练在指定日期的空闲时段，用于学员预约时选择可用时间。
     * </p>
     *
     * @param coachId 教练ID（请求参数）
     * @param date    查询日期（格式：yyyy-MM-dd）
     * @return 该教练在指定日期的空闲时段列表
     * @summary 查询教练空闲时段
     * @权限要求 需要登录
     */
    @Operation(summary = "查询空闲时段")
    @GetMapping("/available-slots")
    public R<List<Lesson>> availableSlots(
            @RequestParam Long coachId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return R.ok(lessonService.getAvailableSlots(coachId, date));
    }

    /**
     * 基于模板的可预约时段查询接口
     * <p>
     * 基于时间段模板查询指定教练在指定日期的可预约时段。
     * 与availableSlots不同，此接口返回结构化的时段模板数据。
     * </p>
     *
     * @param coachId 教练ID（请求参数）
     * @param date    查询日期（格式：yyyy-MM-dd）
     * @return 可预约时段列表，每个时段包含模板信息和可用状态
     * @summary 查询基于模板的可预约时段
     * @权限要求 需要登录
     */
    @Operation(summary = "基于模板的可预约时段")
    @GetMapping("/available-time-slots")
    public R<List<Map<String, Object>>> availableTimeSlots(
            @RequestParam Long coachId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return R.ok(lessonService.getAvailableTimeSlots(coachId, date));
    }

    /**
     * 批量创建预约接口
     * <p>
     * 在多个日期批量创建相同配置的课时预约，适用于连续多天培训的场景。
     * </p>
     *
     * @param request 批量创建请求对象，包含学员、教练、车辆、课程、时间和日期列表
     * @return 操作成功返回R.ok()
     * @summary 批量创建培训预约
     * @权限要求 前台（STAFF）、管理员（ADMIN）
     */
    @Operation(summary = "批量创建预约（前台/管理员）")
    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public R<Void> batchCreate(@RequestBody BatchCreateRequest request) {
        // 将请求对象转换为Lesson实体
        Lesson lesson = new Lesson();
        lesson.setStudentId(request.getStudentId());
        lesson.setCoachId(request.getCoachId());
        lesson.setVehicleId(request.getVehicleId());
        lesson.setCourseId(request.getCourseId());
        lesson.setStartTime(request.getStartTime());
        lesson.setEndTime(request.getEndTime());
        lesson.setRemark(request.getRemark());
        lessonService.batchCreate(lesson, request.getDates());
        return R.ok();
    }

    /**
     * 时间段模板列表查询接口
     * <p>
     * 查询所有可用的时间段模板，用于预约时选择时间段。
     * </p>
     *
     * @return 时间段模板列表
     * @summary 查询时间段模板列表
     * @权限要求 需要登录
     */
    @Operation(summary = "时间段模板列表")
    @GetMapping("/time-slot-templates")
    public R<List<TimeSlotTemplate>> timeSlotTemplates() {
        return R.ok(lessonService.getTimeSlotTemplates());
    }

    /**
     * 创建时间段模板接口
     * <p>
     * 创建新的时间段模板，用于规范预约时间段。
     * </p>
     *
     * @param template 时间段模板实体对象
     * @return 操作成功返回R.ok()
     * @summary 创建时间段模板
     * @权限要求 仅管理员（ADMIN）
     */
    @Operation(summary = "创建时间段模板")
    @PostMapping("/time-slot-templates")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> createTimeSlotTemplate(@RequestBody TimeSlotTemplate template) {
        lessonService.createTimeSlotTemplate(template);
        return R.ok();
    }

    /**
     * 更新时间段模板接口
     * <p>
     * 根据模板ID更新时间段模板信息。
     * </p>
     *
     * @param id       模板ID（路径参数）
     * @param template 时间段模板实体对象，包含需要更新的字段
     * @return 操作成功返回R.ok()
     * @summary 更新时间段模板
     * @权限要求 仅管理员（ADMIN）
     */
    @Operation(summary = "更新时间段模板")
    @PutMapping("/time-slot-templates/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> updateTimeSlotTemplate(@PathVariable Long id, @RequestBody TimeSlotTemplate template) {
        template.setId(id);
        lessonService.updateTimeSlotTemplate(template);
        return R.ok();
    }

    /**
     * 删除时间段模板接口
     * <p>
     * 根据模板ID删除时间段模板。
     * </p>
     *
     * @param id 模板ID（路径参数）
     * @return 操作成功返回R.ok()
     * @summary 删除时间段模板
     * @权限要求 仅管理员（ADMIN）
     */
    @Operation(summary = "删除时间段模板")
    @DeleteMapping("/time-slot-templates/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> deleteTimeSlotTemplate(@PathVariable Long id) {
        lessonService.deleteTimeSlotTemplate(id);
        return R.ok();
    }
}
