package com.drivingschool.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.exam.entity.Exam;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考试Mapper接口
 * 对应实体类：{@link Exam}（exam表）
 * 提供考试信息的数据库CRUD操作，继承MyBatis-Plus的BaseMapper获得通用增删改查能力
 */
@Mapper
public interface ExamMapper extends BaseMapper<Exam> {
}
