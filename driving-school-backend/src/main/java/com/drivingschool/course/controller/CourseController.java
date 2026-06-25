package com.drivingschool.course.controller;

import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.common.result.R;
import com.drivingschool.course.entity.Course;
import com.drivingschool.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程配置控制器
 * <p>
 * 功能说明：管理驾校的课程配置信息，提供课程列表查询和课程详情查看功能。
 * 课程是按报考类型（如C1、C2等）划分的培训方案，包含各科目的学时要求等配置。
 * </p>
 * 基础路径：/courses
 * <p>
 * 接口权限说明：
 * - 所有接口均无需特殊权限，登录即可访问
 * </p>
 *
 * @author drivingschool
 */
@Tag(name = "课程配置")
@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    /** 课程服务，处理课程配置的查询操作 */
    private final CourseService courseService;

    /**
     * 课程列表查询接口
     * <p>
     * 查询课程配置列表，可按报考类型筛选。
     * </p>
     *
     * @param applyType 报考类型筛选条件（可选，如C1、C2等）
     * @return 课程配置列表
     * @summary 查询课程列表
     * @权限要求 无需特殊权限（登录即可访问）
     */
    @Operation(summary = "课程列表")
    @GetMapping
    public R<List<Course>> list(@RequestParam(required = false) String applyType) {
        return R.ok(courseService.list(applyType));
    }

    /**
     * 课程详情接口
     * <p>
     * 根据课程ID查询课程的详细配置信息。
     * </p>
     *
     * @param id 课程ID（路径参数）
     * @return 课程的详细配置信息
     * @summary 查询课程详情
     * @权限要求 无需特殊权限（登录即可访问）
     */
    @Operation(summary = "课程详情")
    @GetMapping("/{id}")
    public R<Course> detail(@PathVariable Long id) {
        Course course = courseService.getById(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        return R.ok(course);
    }
}
