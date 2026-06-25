package com.drivingschool.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drivingschool.course.entity.Course;
import com.drivingschool.course.mapper.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 课程配置服务类
 * <p>
 * 提供课程信息的查询功能，包括按报考类型筛选课程列表和根据ID查询课程详情。
 * 课程配置由管理员维护，定义了不同报考类型（C1/C2等）对应的培训课程信息。
 * </p>
 *
 * @author drivingschool
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    /** 课程数据访问层 */
    private final CourseMapper courseMapper;

    /**
     * 查询课程列表
     * <p>
     * 根据报考类型筛选课程，如果applyType为null则查询所有课程。
     * </p>
     *
     * @param applyType 报考类型（如C1、C2），可为null
     * @return 课程列表
     */
    public List<Course> list(String applyType) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(applyType != null, Course::getApplyType, applyType);
        return courseMapper.selectList(wrapper);
    }

    /**
     * 根据ID查询课程详情
     *
     * @param id 课程ID
     * @return 课程实体，不存在时返回null
     */
    public Course getById(Long id) {
        return courseMapper.selectById(id);
    }
}
