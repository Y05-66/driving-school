package com.drivingschool.exam.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.common.result.PageResult;
import com.drivingschool.common.result.R;
import com.drivingschool.exam.entity.Exam;
import com.drivingschool.exam.entity.ExamEnrollment;
import com.drivingschool.exam.service.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.drivingschool.student.entity.Student;
import com.drivingschool.student.service.StudentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 考试管理控制器
 * <p>
 * 功能说明：管理驾校考试的全流程，包括考试安排的创建与查询、学员考试报名、
 * 成绩录入、报名记录查询以及学员考试历史查看等功能。
 * </p>
 * 基础路径：/exams
 * <p>
 * 接口权限说明：
 * - 考试安排列表和报名记录查询：登录即可访问
 * - 创建考试安排和成绩录入：需要STAFF或ADMIN角色
 * - 考试报名：学员（STUDENT）、前台（STAFF）、管理员（ADMIN）
 * - 学员考试历史：登录即可访问
 * </p>
 *
 * @author drivingschool
 */
@Tag(name = "考试管理")
@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
public class ExamController {

    /** 考试服务，处理考试安排、报名和成绩管理的核心业务逻辑 */
    private final ExamService examService;

    /** 学员服务，用于根据用户ID查找学员信息 */
    private final StudentService studentService;

    /**
     * 考试查询条件对象
     * 用于接收前端传递的考试筛选和分页参数
     */
    @Data
    public static class ExamQuery {
        /** 考试类型筛选（如科目一、科目二、科目三、科目四） */
        private String examType;
        /** 报考类型筛选（如C1、C2） */
        private String applyType;
        /** 考试日期筛选（格式：yyyy-MM-dd） */
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate examDate;
        /** 页码，默认第1页 */
        private Integer pageNum = 1;
        /** 每页条数，默认10条 */
        private Integer pageSize = 10;
    }

    /**
     * 报名查询条件对象
     * 用于接收前端传递的考试报名筛选和分页参数
     */
    @Data
    public static class EnrollQuery {
        /** 考试安排ID筛选 */
        private Long examId;
        /** 学员ID筛选 */
        private Long studentId;
        /** 报名状态筛选 */
        private Integer status;
        /** 页码，默认第1页 */
        private Integer pageNum = 1;
        /** 每页条数，默认10条 */
        private Integer pageSize = 10;
    }

    /**
     * 成绩录入请求对象
     * 用于录入学员的考试成绩
     */
    @Data
    public static class ResultRequest {
        /** 考试分数 */
        private Integer score;
        /** 考试状态（如：1-通过，0-未通过） */
        private Integer status;
    }

    /**
     * 考试安排列表查询接口
     * <p>
     * 分页查询考试安排列表，支持按考试类型、报考类型、考试日期筛选。
     * </p>
     *
     * @param query 查询条件对象，包含考试类型、报考类型、考试日期和分页参数
     * @return 分页后的考试安排列表（PageResult格式）
     * @summary 查询考试安排列表
     * @权限要求 需要登录
     */
    @Operation(summary = "考试安排列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','STUDENT','COACH')")
    public R<PageResult<Exam>> list(ExamQuery query) {
        // 构建分页对象
        Page<Exam> page = new Page<>(query.getPageNum(), query.getPageSize());
        // 执行分页查询
        Page<Exam> result = examService.page(page, query.getExamType(), query.getApplyType(), query.getExamDate());
        return R.ok(PageResult.of(result));
    }

    /**
     * 创建考试安排接口
     * <p>
     * 创建新的考试安排，包括考试类型、考试日期、考试地点等信息。
     * </p>
     *
     * @param exam 考试安排实体对象，包含考试的基本信息
     * @return 操作成功返回R.ok()
     * @summary 创建考试安排
     * @权限要求 前台（STAFF）、管理员（ADMIN）
     */
    @Operation(summary = "创建考试安排")
    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public R<Void> create(@Valid @RequestBody Exam exam) {
        examService.create(exam);
        return R.ok();
    }

    /**
     * 考试报名接口
     * <p>
     * 为指定学员报名参加指定考试。
     * STUDENT 角色自动使用当前登录用户关联的学员ID，忽略请求中的 studentId。
     * STAFF/ADMIN 角色可以指定任意学员ID。
     * </p>
     *
     * @param examId         考试安排ID（路径参数）
     * @param studentId      学员ID（请求参数，STUDENT 角色忽略此参数）
     * @param authentication 当前认证信息
     * @return 操作成功返回R.ok()
     * @summary 学员考试报名
     * @权限要求 学员（STUDENT）、前台（STAFF）、管理员（ADMIN）
     */
    @Operation(summary = "报名考试")
    @PostMapping("/{examId}/enroll")
    @PreAuthorize("hasAnyRole('STUDENT','STAFF','ADMIN')")
    public R<Void> enroll(@PathVariable Long examId, @RequestParam(required = false) Long studentId,
                          Authentication authentication) {
        // STUDENT 角色：自动使用当前用户关联的学员ID
        Long userId = (Long) authentication.getPrincipal();
        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        if (isStudent) {
            Student student = studentService.getByUserId(userId);
            if (student == null) {
                return R.fail(400, "当前用户未关联学员信息");
            }
            studentId = student.getId();
        }
        if (studentId == null) {
            return R.fail(400, "学员ID不能为空");
        }
        examService.enroll(examId, studentId);
        return R.ok();
    }

    /**
     * 录入成绩接口
     * <p>
     * 为已参加考试的学员录入考试成绩和通过状态。
     * </p>
     *
     * @param id      报名记录ID（路径参数）
     * @param request 成绩录入请求对象，包含分数和状态
     * @return 操作成功返回R.ok()
     * @summary 录入考试成绩
     * @权限要求 前台（STAFF）、管理员（ADMIN）
     */
    @Operation(summary = "录入成绩")
    @PutMapping("/enrollments/{id}/result")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public R<Void> updateResult(@PathVariable Long id, @RequestBody ResultRequest request) {
        examService.updateResult(id, request.getScore(), request.getStatus());
        return R.ok();
    }

    /**
     * 报名记录列表查询接口
     * <p>
     * 分页查询考试报名记录，支持按考试安排ID、学员ID、报名状态筛选。
     * </p>
     *
     * @param query 查询条件对象，包含考试ID、学员ID、状态和分页参数
     * @return 分页后的报名记录列表（PageResult格式）
     * @summary 查询考试报名记录
     * @权限要求 需要登录
     */
    @Operation(summary = "报名列表")
    @GetMapping("/enrollments")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public R<PageResult<ExamEnrollment>> enrollments(EnrollQuery query) {
        // 构建分页对象
        Page<ExamEnrollment> page = new Page<>(query.getPageNum(), query.getPageSize());
        // 执行分页查询
        Page<ExamEnrollment> result = examService.enrollmentPage(page, query.getExamId(), query.getStudentId(), query.getStatus());
        return R.ok(PageResult.of(result));
    }

    /**
     * 学员考试历史接口
     * <p>
     * 查询指定学员的所有考试记录，包括各科目的报名和成绩信息。
     * </p>
     *
     * @param id 学员ID（路径参数）
     * @return 学员的考试历史记录列表
     * @summary 查询学员考试历史
     * @权限要求 需要登录
     */
    @Operation(summary = "学员考试历史")
    @GetMapping("/students/{id}/exam-history")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','STUDENT','COACH')")
    public R<List<ExamEnrollment>> examHistory(@PathVariable Long id, Authentication authentication) {
        // 学员只能查看自己的考试历史（IDOR 防护）
        Long userId = (Long) authentication.getPrincipal();
        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        if (isStudent) {
            Student student = studentService.getByUserId(userId);
            if (student == null || !student.getId().equals(id)) {
                return R.fail(403, "无权查看其他学员的考试历史");
            }
        }
        return R.ok(examService.getExamHistory(id));
    }
}
