package com.drivingschool.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.course.entity.Lesson;
import org.apache.ibatis.annotations.Mapper;

/**
 * 课时记录Mapper接口
 * 对应实体类：{@link Lesson}（lesson表）
 * 提供课程预约/课时记录的数据库CRUD操作，继承MyBatis-Plus的BaseMapper获得通用增删改查能力
 */
@Mapper
public interface LessonMapper extends BaseMapper<Lesson> {
}
