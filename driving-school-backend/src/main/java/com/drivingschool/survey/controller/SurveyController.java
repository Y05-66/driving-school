package com.drivingschool.survey.controller;

import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.common.result.R;
import com.drivingschool.student.entity.Student;
import com.drivingschool.student.service.StudentService;
import com.drivingschool.survey.entity.SatisfactionSurvey;
import com.drivingschool.survey.service.SurveyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 满意度调查控制器
 * <p>
 * 功能说明：提供学员满意度评价的相关接口，包括提交课后满意度调查、
 * 查询教练的评价列表和平均评分等功能。
 * </p>
 * 基础路径：/surveys
 * <p>
 * 接口权限说明：
 * - 提交评价：需要学员角色（STUDENT）
 * - 查询教练评价和评分：需要管理员（ADMIN）、前台（STAFF）、教练（COACH）角色
 * </p>
 *
 * @author drivingschool
 */
@Tag(name = "满意度调查")
@RestController
@RequestMapping("/surveys")
@RequiredArgsConstructor
public class SurveyController {

    /** 满意度调查服务，处理满意度评价相关的业务逻辑 */
    private final SurveyService surveyService;
    /** 学员服务，用于根据用户ID查找学员信息 */
    private final StudentService studentService;

    /**
     * 提交满意度调查接口
     * <p>
     * 学员在完成课时后提交对教练教学的满意度评价。
     * 评价包含教学质量、服务态度、教学环境、综合满意度四个维度的评分（1-5分），
     * 以及文字评价内容和改进建议。
     * </p>
     *
     * @param authentication Spring Security认证对象，包含当前用户身份信息
     * @param survey         满意度调查实体，包含课时ID、教练ID、各项评分和评价内容
     * @return 操作成功返回R.ok()
     * @summary 提交满意度调查
     * @权限要求 学员角色（STUDENT）
     */
    @Operation(summary = "提交满意度调查")
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public R<Void> submit(Authentication authentication, @RequestBody SatisfactionSurvey survey) {
        // 从认证信息中获取当前用户ID，查找关联的学员信息
        Long userId = (Long) authentication.getPrincipal();
        Student student = studentService.getByUserId(userId);
        if (student == null) {
            throw new BusinessException("当前用户未关联学员信息");
        }
        survey.setStudentId(student.getId());
        surveyService.submit(survey);
        return R.ok();
    }

    /**
     * 获取教练的评价列表接口
     * <p>
     * 查询指定教练收到的所有满意度评价，按创建时间倒序排列。
     * </p>
     *
     * @param coachId 教练ID（路径参数）
     * @return 该教练的所有评价列表
     * @summary 查询教练的评价列表
     * @权限要求 管理员（ADMIN）、前台（STAFF）、教练（COACH）
     */
    @Operation(summary = "教练的评价列表")
    @GetMapping("/coach/{coachId}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','COACH')")
    public R<List<SatisfactionSurvey>> getByCoachId(@PathVariable Long coachId) {
        return R.ok(surveyService.getByCoachId(coachId));
    }

    /**
     * 获取教练的平均评分接口
     * <p>
     * 统计指定教练的各项评分平均值，包括教学质量、服务态度、教学环境、综合满意度四个维度，
     * 同时返回评价总数。
     * </p>
     *
     * @param coachId 教练ID（路径参数）
     * @return 包含各维度平均分和评价总数的统计数据
     * @summary 查询教练的平均评分
     * @权限要求 管理员（ADMIN）、前台（STAFF）、教练（COACH）
     */
    @Operation(summary = "教练的平均评分")
    @GetMapping("/coach/{coachId}/avg")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','COACH')")
    public R<Map<String, Object>> getAvgRating(@PathVariable Long coachId) {
        return R.ok(surveyService.getAvgRating(coachId));
    }
}
