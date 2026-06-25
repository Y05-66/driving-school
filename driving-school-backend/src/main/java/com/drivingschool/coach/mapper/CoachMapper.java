package com.drivingschool.coach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.coach.entity.Coach;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教练员Mapper接口
 * 对应实体类：{@link Coach}（coach表）
 * 提供教练员的数据库CRUD操作，继承MyBatis-Plus的BaseMapper获得通用增删改查能力
 */
@Mapper
public interface CoachMapper extends BaseMapper<Coach> {
}
