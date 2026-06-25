package com.drivingschool.student.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.coach.entity.Coach;
import com.drivingschool.coach.service.CoachStudentService;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.common.result.PageResult;
import com.drivingschool.common.result.R;
import com.drivingschool.course.service.LessonService;
import com.drivingschool.student.dto.StudentCreateDTO;
import com.drivingschool.student.dto.StudentQueryDTO;
import com.drivingschool.student.entity.Student;
import com.drivingschool.student.entity.StudentProgress;
import com.drivingschool.student.service.StudentProgressService;
import com.drivingschool.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学员管理控制器
 * <p>
 * 功能说明：管理驾校学员的全生命周期，包括学员报名、信息查询与修改、状态变更、
 * 查看学员的教练信息、培训进度、科目进度以及时间线等功能。
 * 同时提供学员个人操作接口（查看自己的信息、修改个人信息、查看自己的教练和进度）。
 * </p>
 * 基础路径：/students
 * <p>
 * 接口权限说明：
 * - 学员个人接口（/me相关）：需要STUDENT角色
 * - 管理操作接口：需要ADMIN或STAFF角色
 * - 查看类接口：ADMIN、STAFF、COACH角色均可访问
 * </p>
 *
 * @author drivingschool
 */
@Tag(name = "学员管理")
@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentController {

    /** 学员服务，处理学员信息的CRUD操作 */
    private final StudentService studentService;

    /** 课时服务，用于查询学员的课时预约和教练信息 */
    private final LessonService lessonService;

    /** 学员进度服务，用于查询和更新学员的科目培训进度 */
    private final StudentProgressService studentProgressService;

    /** 教练-学员关联服务，用于查询学员的教练分配信息 */
    private final CoachStudentService coachStudentService;

    /**
     * 获取当前登录学员信息接口
     * <p>
     * 根据当前登录用户的ID查询对应的学员信息。
     * </p>
     *
     * @param authentication Spring Security认证对象，包含当前用户身份信息
     * @return 当前学员的详细信息
     * @summary 获取当前学员信息
     * @权限要求 学员角色（STUDENT）
     */
    @Operation(summary = "当前学员信息")
    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public R<Student> me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Student student = studentService.getByUserId(userId);
        if (student == null) {
            throw new BusinessException("学员信息不存在");
        }
        return R.ok(student);
    }

    /**
     * 获取学员的主要教练接口
     * <p>
     * 根据学员ID查询其通过课时关联的主要教练信息。
     * </p>
     *
     * @param id 学员ID（路径参数）
     * @return 学员的主要教练信息
     * @summary 查询学员的主要教练
     * @权限要求 管理员（ADMIN）、前台（STAFF）、教练（COACH）
     */
    @Operation(summary = "学员的教练")
    @GetMapping("/{id}/coach")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','COACH')")
    public R<Coach> getCoach(@PathVariable Long id) {
        return R.ok(lessonService.getCoachByStudentId(id));
    }

    /**
     * 获取学员的所有教练接口
     * <p>
     * 根据学员ID查询其通过教练-学员分配关系关联的所有教练列表。
     * 一个学员可能因不同科目被分配给不同的教练。
     * </p>
     *
     * @param id 学员ID（路径参数）
     * @return 学员的所有教练列表
     * @summary 查询学员的所有教练（按分配关系）
     * @权限要求 管理员（ADMIN）、前台（STAFF）、教练（COACH）
     */
    @Operation(summary = "学员的所有教练（按分配）")
    @GetMapping("/{id}/coaches")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','COACH')")
    public R<List<Coach>> getCoaches(@PathVariable Long id) {
        return R.ok(coachStudentService.getCoachesByStudentId(id));
    }

    /**
     * 获取当前学员的教练列表接口
     * <p>
     * 学员查看自己被分配的所有教练信息。
     * </p>
     *
     * @param authentication Spring Security认证对象，包含当前用户身份信息
     * @return 当前学员的教练列表，未查到学员信息时返回空列表
     * @summary 查看我的教练
     * @权限要求 学员角色（STUDENT）
     */
    @Operation(summary = "我的教练")
    @GetMapping("/me/coaches")
    @PreAuthorize("hasRole('STUDENT')")
    public R<List<Coach>> myCoaches(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Student student = studentService.getByUserId(userId);
        if (student == null) {
            return R.ok(List.of());
        }
        return R.ok(coachStudentService.getCoachesByStudentId(student.getId()));
    }

    /**
     * 学员统计数据接口
     * <p>
     * 统计各状态的学员数量，用于PC端仪表盘展示。
     * </p>
     *
     * @return 统计数据Map：total（总数）、pending（待审核）、studying（在学）、graduated（已结业）、withdrawn（已退学）
     * @summary 学员统计
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "学员统计")
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<Map<String, Long>> stats() {
        return R.ok(studentService.getStats());
    }

    /**
     * 学员列表查询接口
     * <p>
     * 分页查询学员列表，支持按姓名、身份证号、手机号、状态、报考类型筛选。
     * </p>
     *
     * @param query 查询条件DTO，包含姓名、身份证号、手机号、状态、报考类型及分页参数
     * @return 分页后的学员列表（PageResult格式）
     * @summary 查询学员列表
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "学员列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<PageResult<Student>> list(StudentQueryDTO query) {
        // 构建分页对象
        Page<Student> page = new Page<>(query.getPageNum(), query.getPageSize());
        // 执行分页查询
        Page<Student> result = studentService.page(page, query.getName(), query.getIdCard(),
                query.getPhone(), query.getStatus(), query.getApplyType());
        return R.ok(PageResult.of(result));
    }

    /**
     * 学员详情接口
     * <p>
     * 根据学员ID查询学员的详细信息。
     * </p>
     *
     * @param id 学员ID（路径参数）
     * @return 学员的详细信息
     * @summary 查询学员详情
     * @权限要求 管理员（ADMIN）、前台（STAFF）、教练（COACH）
     */
    @Operation(summary = "学员详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','COACH')")
    public R<Student> detail(@PathVariable Long id) {
        Student student = studentService.getById(id);
        if (student == null) {
            throw new BusinessException("学员不存在");
        }
        return R.ok(student);
    }

    /**
     * 学员报名接口
     * <p>
     * 创建新的学员报名记录，包含学员基本信息和报考类型等。
     * 使用@Valid注解对DTO参数进行Jakarta Validation校验。
     * </p>
     *
     * @param dto 学员创建DTO，包含报名所需的学员信息（经过参数校验）
     * @return 操作成功返回R.ok()
     * @summary 学员报名
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "学员报名")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<Void> create(@Valid @RequestBody StudentCreateDTO dto) {
        studentService.create(dto);
        return R.ok();
    }

    /**
     * 学员修改个人信息接口
     * <p>
     * 学员修改自己的个人信息（如联系方式、地址等）。
     * </p>
     *
     * @param authentication Spring Security认证对象，包含当前用户身份信息
     * @param student        学员实体对象，包含需要更新的字段
     * @return 操作成功返回R.ok()
     * @summary 学员修改个人信息
     * @权限要求 学员角色（STUDENT）
     */
    @Operation(summary = "修改个人信息")
    @PutMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public R<Void> updateMyInfo(Authentication authentication, @RequestBody Student student) {
        Long userId = (Long) authentication.getPrincipal();
        studentService.updateMyInfo(userId, student);
        return R.ok();
    }

    /**
     * 修改学员信息接口（管理员/前台操作）
     * <p>
     * 管理员或前台工作人员修改指定学员的信息。
     * </p>
     *
     * @param id      学员ID（路径参数）
     * @param student 学员实体对象，包含需要更新的字段
     * @return 操作成功返回R.ok()
     * @summary 修改学员信息
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "修改学员信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<Void> update(@PathVariable Long id, @RequestBody Student student) {
        // 只允许更新指定字段，防止客户端篡改userId、status等内部字段
        Student existing = studentService.getById(id);
        if (existing == null) {
            throw new BusinessException("学员不存在");
        }
        if (student.getName() != null) existing.setName(student.getName());
        if (student.getPhone() != null) existing.setPhone(student.getPhone());
        if (student.getGender() != null) existing.setGender(student.getGender());
        if (student.getBirthday() != null) existing.setBirthday(student.getBirthday());
        if (student.getAddress() != null) existing.setAddress(student.getAddress());
        if (student.getPhoto() != null) existing.setPhoto(student.getPhoto());
        // applyType 不允许修改（与学习进度初始化绑定）
        if (student.getRemark() != null) existing.setRemark(student.getRemark());
        studentService.update(existing);
        return R.ok();
    }

    /**
     * 变更学员状态接口
     * <p>
     * 变更学员的状态（如：在学、毕业、退学等）。
     * </p>
     *
     * @param id     学员ID（路径参数）
     * @param status 目标状态值
     * @return 操作成功返回R.ok()
     * @summary 变更学员状态
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "变更学员状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        studentService.updateStatus(id, status);
        return R.ok();
    }

    /**
     * 学员培训进度概览接口
     * <p>
     * 查询指定学员的培训进度概览，包括总课时数、已完成课时数、待处理课时数。
     * status==3表示已完成，status<=1表示待处理。
     * </p>
     *
     * @param id 学员ID（路径参数）
     * @return 包含studentId、totalLessons、completedLessons、pendingLessons的进度概览Map
     * @summary 查询学员培训进度概览
     * @权限要求 管理员（ADMIN）、前台（STAFF）、教练（COACH）
     */
    @Operation(summary = "学员培训进度")
    @GetMapping("/{id}/progress")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','COACH')")
    public R<Map<String, Object>> progress(@PathVariable Long id) {
        // 查询该学员的所有课时
        List<com.drivingschool.course.entity.Lesson> lessons = lessonService.getByStudentId(id);
        // 统计已完成课时（status==3表示已完成）
        long completed = lessons.stream().filter(l -> l.getStatus() == 3).count();
        long total = lessons.size();
        // 组装进度数据
        Map<String, Object> progress = new HashMap<>();
        progress.put("studentId", id);
        progress.put("totalLessons", total);
        progress.put("completedLessons", completed);
        progress.put("pendingLessons", lessons.stream().filter(l -> l.getStatus() <= 1).count());
        return R.ok(progress);
    }

    /**
     * 学员时间线接口
     * <p>
     * 查询指定学员的所有课时记录，按时间排列展示学员的培训时间线。
     * </p>
     *
     * @param id 学员ID（路径参数）
     * @return 学员的所有课时列表
     * @summary 查询学员培训时间线
     * @权限要求 管理员（ADMIN）、前台（STAFF）、教练（COACH）
     */
    @Operation(summary = "学员时间线")
    @GetMapping("/{id}/timeline")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','COACH')")
    public R<List<com.drivingschool.course.entity.Lesson>> timeline(@PathVariable Long id) {
        return R.ok(lessonService.getByStudentId(id));
    }

    /**
     * 学员科目进度详情接口
     * <p>
     * 查询指定学员各科目的详细培训进度，包括每个科目的完成学时数和状态。
     * </p>
     *
     * @param id 学员ID（路径参数）
     * @return 学员的科目进度详情列表
     * @summary 查询学员科目进度详情
     * @权限要求 管理员（ADMIN）、前台（STAFF）、教练（COACH）
     */
    @Operation(summary = "学员科目进度详情")
    @GetMapping("/{id}/progress/detail")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','COACH')")
    public R<List<StudentProgress>> progressDetail(@PathVariable Long id) {
        return R.ok(studentProgressService.getByStudentId(id));
    }

    /**
     * 当前学员的科目进度接口
     * <p>
     * 学员查看自己的各科目培训进度。
     * </p>
     *
     * @param authentication Spring Security认证对象，包含当前用户身份信息
     * @return 当前学员的科目进度列表，未查到学员信息时返回空列表
     * @summary 查看我的科目进度
     * @权限要求 学员角色（STUDENT）
     */
    @Operation(summary = "我的科目进度")
    @GetMapping("/me/progress")
    @PreAuthorize("hasRole('STUDENT')")
    public R<List<StudentProgress>> myProgress(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Student student = studentService.getByUserId(userId);
        if (student == null) {
            return R.ok(List.of());
        }
        return R.ok(studentProgressService.getByStudentId(student.getId()));
    }

    /**
     * 更新学员科目进度接口
     * <p>
     * 更新指定学员的某个科目进度信息，可更新已完成学时数和状态。
     * </p>
     *
     * @param id             学员ID（路径参数）
     * @param progressId     科目进度记录ID（路径参数）
     * @param completedHours 已完成学时数（可选参数）
     * @param status         科目状态（可选参数）
     * @return 操作成功返回R.ok()
     * @summary 更新学员科目进度
     * @权限要求 管理员（ADMIN）、前台（STAFF）、教练（COACH）
     */
    @Operation(summary = "更新学员科目进度")
    @PutMapping("/{id}/progress/{progressId}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','COACH')")
    public R<Void> updateProgress(@PathVariable Long id, @PathVariable Long progressId,
                                  @RequestParam(required = false) Integer completedHours,
                                  @RequestParam(required = false) Integer status) {
        studentProgressService.updateProgress(progressId, completedHours, status, id);
        return R.ok();
    }
}
