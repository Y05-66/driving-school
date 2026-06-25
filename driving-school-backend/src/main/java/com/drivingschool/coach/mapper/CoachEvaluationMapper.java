package com.drivingschool.coach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.coach.entity.CoachEvaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 教练评价Mapper接口
 * 对应实体类：{@link CoachEvaluation}（coach_evaluation表）
 * 提供教练评价的数据库CRUD操作，继承MyBatis-Plus的BaseMapper获得通用增删改查能力
 * 同时包含教练评分统计相关的自定义查询方法
 */
@Mapper
public interface CoachEvaluationMapper extends BaseMapper<CoachEvaluation> {

    /**
     * 查询指定教练的平均评分
     * 计算该教练所有有效评价记录（排除已逻辑删除）的评分平均值
     * 用于更新教练的综合评分（rating字段）
     *
     * @param coachId 教练ID
     * @return 教练的平均评分，保留小数精度，无评价记录时返回null
     */
    @Select("SELECT AVG(rating) FROM coach_evaluation WHERE coach_id = #{coachId} AND deleted = 0")
    BigDecimal avgRatingByCoachId(@Param("coachId") Long coachId);
}
