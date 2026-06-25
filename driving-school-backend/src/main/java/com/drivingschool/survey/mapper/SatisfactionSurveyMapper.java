package com.drivingschool.survey.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.survey.entity.SatisfactionSurvey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 满意度调查Mapper接口
 * 对应实体类：{@link SatisfactionSurvey}（satisfaction_survey表）
 * 提供满意度调查的数据库CRUD操作，继承MyBatis-Plus的BaseMapper获得通用增删改查能力
 * 同时包含教练评分统计相关的自定义查询方法
 */
@Mapper
public interface SatisfactionSurveyMapper extends BaseMapper<SatisfactionSurvey> {

    /**
     * 查询教练的平均综合评分
     * 计算指定教练所有满意度调查记录的综合评分（overall_rating）平均值
     * 排除已逻辑删除的记录，用于展示教练的整体评价水平
     *
     * @param coachId 教练ID
     * @return 该教练的平均综合评分（1-5分），无评价记录时返回null
     */
    @Select("SELECT AVG(overall_rating) FROM satisfaction_survey WHERE coach_id = #{coachId} AND deleted = 0")
    Double avgRatingByCoach(@Param("coachId") Long coachId);
}
