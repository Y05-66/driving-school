package com.drivingschool.coach.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.coach.dto.CoachQueryDTO;
import com.drivingschool.coach.entity.Coach;
import com.drivingschool.coach.entity.CoachEvaluation;
import com.drivingschool.coach.entity.CoachStudent;
import com.drivingschool.coach.service.CoachEvaluationService;
import com.drivingschool.coach.service.CoachService;
import com.drivingschool.coach.service.CoachStudentService;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.common.result.PageResult;
import com.drivingschool.common.result.R;
import com.drivingschool.course.service.LessonService;
import com.drivingschool.student.entity.Student;
import com.drivingschool.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 教练管理控制器
 * <p>
 * 功能说明：管理驾校教练的全生命周期，包括教练信息的增删改查、教练排班查询、
 * 教练-学员分配管理（分配、换教练、解除分配）、以及学员对教练的评价功能。
 * 同时提供教练个人操作接口（查看自己的信息和学员列表）。
 * </p>
 * 基础路径：/coaches
 * <p>
 * 接口权限说明：
 * - 教练个人接口（/me相关）：需要COACH角色
 * - 管理操作接口（增删改、分配等）：需要ADMIN或STAFF角色
 * - 查看类接口：部分需要登录即可访问，部分需要特定角色
 * - 评价接口：需要STUDENT角色
 * </p>
 *
 * @author drivingschool
 */
@Tag(name = "教练管理")
@RestController
@RequestMapping("/coaches")
@RequiredArgsConstructor
public class CoachController {

    /** 教练服务，处理教练信息的CRUD操作 */
    private final CoachService coachService;

    /** 教练-学员关联服务，处理教练与学员之间的分配关系 */
    private final CoachStudentService coachStudentService;

    /** 教练评价服务，处理学员对教练的评价 */
    private final CoachEvaluationService coachEvaluationService;

    /** 课时服务，用于查询教练的排班和课时信息 */
    private final LessonService lessonService;

    /** 学员服务，用于通过userId查找学员信息 */
    private final StudentService studentService;

    /**
     * 获取当前登录教练信息接口
     * <p>
     * 根据当前登录用户的ID查询对应的教练信息。
     * </p>
     *
     * @param authentication Spring Security认证对象，包含当前用户身份信息
     * @return 当前教练的详细信息
     * @summary 获取当前教练信息
     * @权限要求 教练角色（COACH）
     */
    @Operation(summary = "当前教练信息")
    @GetMapping("/me")
    @PreAuthorize("hasRole('COACH')")
    public R<Coach> me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return R.ok(coachService.getByUserId(userId));
    }

    /**
     * 获取当前教练的学员列表接口
     * <p>
     * 教练查看自己所带的所有学员列表。
     * </p>
     *
     * @param authentication Spring Security认证对象，包含当前用户身份信息
     * @return 当前教练的学员列表，未查到教练信息时返回空列表
     * @summary 查看我的学员
     * @权限要求 教练角色（COACH）
     */
    @Operation(summary = "我的学员")
    @GetMapping("/me/students")
    @PreAuthorize("hasRole('COACH')")
    public R<List<Student>> myStudents(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Coach coach = coachService.getByUserId(userId);
        if (coach == null) return R.ok(Collections.emptyList());
        return R.ok(coachStudentService.getStudentsByCoachId(coach.getId()));
    }

    /**
     * 教练列表查询接口
     * <p>
     * 分页查询教练列表，支持按姓名、手机号、教练类型、状态进行筛选。
     * </p>
     *
     * @param query 查询条件DTO，包含姓名、手机号、教练类型、状态及分页参数
     * @return 分页后的教练列表（PageResult格式）
     * @summary 查询教练列表
     * @权限要求 无需特殊权限（登录即可访问）
     */
    @Operation(summary = "教练列表")
    @GetMapping
    public R<PageResult<Coach>> list(CoachQueryDTO query) {
        // 构建分页对象
        Page<Coach> page = new Page<>(query.getPageNum(), query.getPageSize());
        // 执行分页查询
        Page<Coach> result = coachService.page(page, query.getName(), query.getPhone(),
                query.getCoachType(), query.getStatus());
        return R.ok(PageResult.of(result));
    }

    /**
     * 教练详情接口
     * <p>
     * 根据教练ID查询教练的详细信息。
     * </p>
     *
     * @param id 教练ID（路径参数）
     * @return 教练的详细信息
     * @summary 查询教练详情
     * @权限要求 无需特殊权限（登录即可访问）
     */
    @Operation(summary = "教练详情")
    @GetMapping("/{id}")
    public R<Coach> detail(@PathVariable Long id) {
        Coach coach = coachService.getById(id);
        if (coach == null) {
            throw new BusinessException("教练不存在");
        }
        return R.ok(coach);
    }

    /**
     * 添加教练接口
     * <p>
     * 创建新的教练记录，录入教练的基本信息。
     * </p>
     *
     * @param coach 教练实体对象，包含教练的基本信息
     * @return 操作成功返回R.ok()
     * @summary 添加教练
     * @权限要求 仅管理员（ADMIN）
     */
    @Operation(summary = "添加教练")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    // @Valid 触发 Coach 实体上的 Jakarta Validation 注解校验，参数不合法时自动返回400
    public R<Void> create(@Valid @RequestBody Coach coach) {
        coachService.create(coach);
        return R.ok();
    }

    /**
     * 修改教练信息接口
     * <p>
     * 根据教练ID修改教练的基本信息。
     * </p>
     *
     * @param id    教练ID（路径参数）
     * @param coach 教练实体对象，包含需要更新的字段
     * @return 操作成功返回R.ok()
     * @summary 修改教练信息
     * @权限要求 仅管理员（ADMIN）
     */
    @Operation(summary = "修改教练信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> update(@PathVariable Long id, @RequestBody Coach coach) {
        // 白名单方式仅更新允许的字段，防止批量赋值攻击（mass assignment）
        Coach existing = coachService.getById(id);
        if (existing == null) {
            throw new BusinessException("教练不存在");
        }
        existing.setName(coach.getName());
        existing.setPhone(coach.getPhone());
        existing.setIdCard(coach.getIdCard());
        existing.setGender(coach.getGender());
        existing.setCoachType(coach.getCoachType());
        existing.setLicenseNo(coach.getLicenseNo());
        existing.setLicenseExpire(coach.getLicenseExpire());
        existing.setExperienceYears(coach.getExperienceYears());
        coachService.update(existing);
        return R.ok();
    }

    /**
     * 变更教练状态接口
     * <p>
     * 变更教练的状态（如：在职、离职、休假等）。
     * </p>
     *
     * @param id     教练ID（路径参数）
     * @param status 目标状态值
     * @return 操作成功返回R.ok()
     * @summary 变更教练状态
     * @权限要求 仅管理员（ADMIN）
     */
    @Operation(summary = "变更教练状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        // 校验状态值合法性：仅允许0（禁用）或1（启用）
        if (status != 0 && status != 1) {
            return R.fail(400, "状态值只能为0或1");
        }
        coachService.updateStatus(id, status);
        return R.ok();
    }

    /**
     * 教练排班表接口
     * <p>
     * 查询指定教练的所有课时预约记录，用于查看教练的排班情况。
     * </p>
     *
     * @param id 教练ID（路径参数）
     * @return 教练的所有课时列表
     * @summary 查询教练排班表
     * @权限要求 无需特殊权限（登录即可访问）
     */
    @Operation(summary = "教练排班表")
    @GetMapping("/{id}/schedule")
    public R<List<com.drivingschool.course.entity.Lesson>> schedule(@PathVariable Long id) {
        return R.ok(lessonService.getByCoachId(id));
    }

    /**
     * 教练所带学员列表接口
     * <p>
     * 查询指定教练当前所带的所有学员。
     * </p>
     *
     * @param id 教练ID（路径参数）
     * @return 教练所带的学员列表
     * @summary 查询教练所带学员
     * @权限要求 无需特殊权限（登录即可访问）
     */
    @Operation(summary = "所带学员")
    @GetMapping("/{id}/students")
    public R<List<Student>> students(@PathVariable Long id) {
        return R.ok(coachStudentService.getStudentsByCoachId(id));
    }

    /**
     * 分配教练给学员接口
     * <p>
     * 将指定教练分配给指定学员，可选择关联具体科目。
     * </p>
     *
     * @param coachId     教练ID（路径参数）
     * @param studentId   学员ID（请求参数）
     * @param subjectName 科目名称（可选参数，如科目二、科目三）
     * @return 操作成功返回R.ok()
     * @summary 分配教练给学员
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "分配教练给学员")
    @PostMapping("/{coachId}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<Void> assign(@PathVariable Long coachId,
                          @RequestParam Long studentId,
                          @RequestParam(required = false) String subjectName) {
        coachStudentService.assign(coachId, studentId, subjectName);
        return R.ok();
    }

    /**
     * 换教练接口
     * <p>
     * 将已分配的教练-学员关系中的教练更换为新教练。
     * </p>
     *
     * @param id         分配记录ID（路径参数）
     * @param newCoachId 新教练ID（请求参数）
     * @return 操作成功返回R.ok()
     * @summary 更换教练
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "换教练")
    @PutMapping("/assignments/{id}/reassign")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<Void> reassign(@PathVariable Long id, @RequestParam Long newCoachId) {
        coachStudentService.reassign(id, newCoachId);
        return R.ok();
    }

    /**
     * 解除分配接口
     * <p>
     * 解除教练与学员之间的分配关系。
     * </p>
     *
     * @param id 分配记录ID（路径参数）
     * @return 操作成功返回R.ok()
     * @summary 解除教练-学员分配关系
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "解除分配")
    @PutMapping("/assignments/{id}/release")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<Void> release(@PathVariable Long id) {
        coachStudentService.release(id);
        return R.ok();
    }

    /**
     * 教练的分配记录列表接口
     * <p>
     * 查询指定教练的所有教练-学员分配记录。
     * </p>
     *
     * @param id 教练ID（路径参数）
     * @return 教练的分配记录列表
     * @summary 查询教练的分配记录
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "教练的分配记录")
    @GetMapping("/{id}/assignments")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<List<CoachStudent>> assignments(@PathVariable Long id) {
        return R.ok(coachStudentService.listByCoachId(id));
    }

    /**
     * 评价教练接口
     * <p>
     * 学员对指定教练进行评价，包括评分和评价内容。
     * </p>
     *
     * @param coachId        教练ID（路径参数）
     * @param authentication Spring Security认证对象，包含当前用户身份信息
     * @param evaluation     教练评价实体对象，包含评分和评价内容
     * @return 操作成功返回R.ok()
     * @summary 学员评价教练
     * @权限要求 学员角色（STUDENT）
     */
    @Operation(summary = "评价教练")
    @PostMapping("/{coachId}/evaluations")
    @PreAuthorize("hasRole('STUDENT')")
    public R<Void> evaluate(@PathVariable Long coachId, Authentication authentication,
                            @RequestBody CoachEvaluation evaluation) {
        Long userId = (Long) authentication.getPrincipal();
        // 通过 userId 查找当前登录学员，防止身份伪造
        Student student = studentService.getByUserId(userId);
        if (student == null) {
            throw new BusinessException("当前用户未关联学员信息");
        }
        evaluation.setStudentId(student.getId());
        evaluation.setCoachId(coachId);
        coachEvaluationService.create(evaluation);
        return R.ok();
    }

    /**
     * 教练评价列表接口
     * <p>
     * 分页查询指定教练收到的所有评价。
     * </p>
     *
     * @param id       教练ID（路径参数）
     * @param pageNum  页码，默认第1页
     * @param pageSize 每页条数，默认10条
     * @return 分页后的教练评价列表（PageResult格式）
     * @summary 查询教练评价列表
     * @权限要求 无需特殊权限（登录即可访问）
     */
    @Operation(summary = "教练的评价列表")
    @GetMapping("/{id}/evaluations")
    public R<PageResult<CoachEvaluation>> evaluations(@PathVariable Long id,
                                                      @RequestParam(defaultValue = "1") int pageNum,
                                                      @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(PageResult.of(coachEvaluationService.pageByCoachId(id, pageNum, pageSize)));
    }
}
