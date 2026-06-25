package com.drivingschool.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.course.entity.Course;
import org.apache.ibatis.annotations.Mapper;

/**
 * 课程Mapper接口
 * 对应实体类：{@link Course}（course表）
 * 提供课程信息的数据库CRUD操作，继承MyBatis-Plus的BaseMapper获得通用增删改查能力
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
