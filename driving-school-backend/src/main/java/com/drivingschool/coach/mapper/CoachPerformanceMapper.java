package com.drivingschool.coach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.coach.entity.CoachPerformance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教练绩效Mapper接口
 * 提供对coach_performance表的基本CRUD操作
 *
 * @author drivingschool
 */
@Mapper
public interface CoachPerformanceMapper extends BaseMapper<CoachPerformance> {
}
