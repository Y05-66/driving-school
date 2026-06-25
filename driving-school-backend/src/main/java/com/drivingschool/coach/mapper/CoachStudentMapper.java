package com.drivingschool.coach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.coach.entity.CoachStudent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教练-学员分配关系Mapper接口
 * 对应实体类：{@link CoachStudent}（coach_student表）
 * 提供教练与学员分配关系的数据库CRUD操作，继承MyBatis-Plus的BaseMapper获得通用增删改查能力
 */
@Mapper
public interface CoachStudentMapper extends BaseMapper<CoachStudent> {
}
