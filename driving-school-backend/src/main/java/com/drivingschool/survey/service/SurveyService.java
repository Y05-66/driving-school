package com.drivingschool.survey.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.survey.entity.SatisfactionSurvey;
import com.drivingschool.survey.mapper.SatisfactionSurveyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 满意度调查服务类
 * <p>
 * 负责学员满意度评价的核心业务逻辑，包括：
 * - 学员提交课后满意度调查
 * - 查询教练的评价列表
 * - 查询教练的平均评分
 * </p>
 */
@Service
@RequiredArgsConstructor
public class SurveyService {

    /** 满意度调查数据访问层 */
    private final SatisfactionSurveyMapper surveyMapper;

    /**
     * 提交满意度调查
     * <p>
     * 学员在完成课时后提交对教练教学的满意度评价。
     * 业务规则：
     * 1. 课时ID、学员ID、教练ID不能为空
     * 2. 四项评分（教学质量、服务态度、教学环境、综合满意度）必须在1-5分之间
     * 3. 同一学员对同一课时只能提交一次评价
     * </p>
     *
     * @param survey 满意度调查实体，包含评分和评价内容
     * @throws BusinessException 参数校验失败或重复提交时抛出
     */
    public void submit(SatisfactionSurvey survey) {
        // 参数校验：关联ID不能为空
        if (survey.getLessonId() == null) {
            throw new BusinessException("课时ID不能为空");
        }
        if (survey.getStudentId() == null) {
            throw new BusinessException("学员ID不能为空");
        }
        if (survey.getCoachId() == null) {
            throw new BusinessException("教练ID不能为空");
        }

        // 校验评分范围（1-5分）
        validateRating(survey.getTeachingRating(), "教学质量评分");
        validateRating(survey.getAttitudeRating(), "服务态度评分");
        validateRating(survey.getEnvironmentRating(), "教学环境评分");
        validateRating(survey.getOverallRating(), "综合满意度评分");

        // 检查是否重复提交（同一学员对同一课时只能评价一次）
        Long count = surveyMapper.selectCount(
                new LambdaQueryWrapper<SatisfactionSurvey>()
                        .eq(SatisfactionSurvey::getLessonId, survey.getLessonId())
                        .eq(SatisfactionSurvey::getStudentId, survey.getStudentId())
        );
        if (count > 0) {
            throw new BusinessException("您已对该课时提交过评价，请勿重复提交");
        }

        // 保存评价记录
        surveyMapper.insert(survey);
    }

    /**
     * 获取教练的评价列表
     * <p>
     * 查询指定教练收到的所有满意度评价，按创建时间倒序排列。
     * </p>
     *
     * @param coachId 教练ID
     * @return 该教练的所有评价列表
     */
    public List<SatisfactionSurvey> getByCoachId(Long coachId) {
        LambdaQueryWrapper<SatisfactionSurvey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SatisfactionSurvey::getCoachId, coachId)
                .orderByDesc(SatisfactionSurvey::getCreateTime);
        return surveyMapper.selectList(wrapper);
    }

    /**
     * 获取教练的平均评分统计
     * <p>
     * 统计指定教练的各项评分平均值，包括教学质量、服务态度、教学环境、综合满意度四个维度。
     * 同时返回评价总数。
     * </p>
     *
     * @param coachId 教练ID
     * @return 包含各维度平均分和评价总数的Map
     */
    public Map<String, Object> getAvgRating(Long coachId) {
        // 查询该教练的所有评价
        List<SatisfactionSurvey> surveys = getByCoachId(coachId);

        // 组装统计结果
        Map<String, Object> result = new HashMap<>();
        result.put("coachId", coachId);
        result.put("totalCount", surveys.size());

        if (surveys.isEmpty()) {
            // 无评价记录时，各项评分为0
            result.put("avgTeachingRating", 0);
            result.put("avgAttitudeRating", 0);
            result.put("avgEnvironmentRating", 0);
            result.put("avgOverallRating", 0);
        } else {
            // 计算各项评分的平均值（保留一位小数）
            result.put("avgTeachingRating", roundRating(surveys.stream()
                    .filter(s -> s.getTeachingRating() != null)
                    .mapToInt(SatisfactionSurvey::getTeachingRating).average().orElse(0)));
            result.put("avgAttitudeRating", roundRating(surveys.stream()
                    .filter(s -> s.getAttitudeRating() != null)
                    .mapToInt(SatisfactionSurvey::getAttitudeRating).average().orElse(0)));
            result.put("avgEnvironmentRating", roundRating(surveys.stream()
                    .filter(s -> s.getEnvironmentRating() != null)
                    .mapToInt(SatisfactionSurvey::getEnvironmentRating).average().orElse(0)));
            result.put("avgOverallRating", roundRating(surveys.stream()
                    .filter(s -> s.getOverallRating() != null)
                    .mapToInt(SatisfactionSurvey::getOverallRating).average().orElse(0)));
        }

        return result;
    }

    /**
     * 校验评分是否在合法范围内
     *
     * @param rating      评分值
     * @param ratingName  评分名称，用于错误提示
     * @throws BusinessException 评分不在1-5范围内时抛出
     */
    private void validateRating(Integer rating, String ratingName) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException(ratingName + "必须在1-5分之间");
        }
    }

    /**
     * 将评分四舍五入保留一位小数
     *
     * @param value 原始评分值
     * @return 保留一位小数的评分值
     */
    private double roundRating(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
