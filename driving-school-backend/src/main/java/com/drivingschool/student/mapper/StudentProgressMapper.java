package com.drivingschool.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.student.entity.StudentProgress;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学员学习进度Mapper接口
 * 对应实体类：{@link StudentProgress}（student_progress表）
 * 提供学员学习进度的数据库CRUD操作，继承MyBatis-Plus的BaseMapper获得通用增删改查能力
 */
@Mapper
public interface StudentProgressMapper extends BaseMapper<StudentProgress> {
}
