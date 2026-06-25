package com.drivingschool.coach.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.coach.entity.Coach;
import com.drivingschool.coach.entity.CoachEvaluation;
import com.drivingschool.coach.mapper.CoachEvaluationMapper;
import com.drivingschool.coach.mapper.CoachMapper;
import com.drivingschool.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 教练评价服务类
 * <p>
 * 负责学员对教练的教学评价管理，包括：
 * - 提交评价（含评分校验、防重复评价）
 * - 查询教练的评价列表（分页）
 * - 查询学员的评价列表
 * - 获取/更新教练平均评分
 * </p>
 * <p>
 * 评价提交后会自动重新计算教练的平均评分并更新到教练表中
 * </p>
 */
@Service
@RequiredArgsConstructor
public class CoachEvaluationService {

    /** 教练评价数据访问层 */
    private final CoachEvaluationMapper evaluationMapper;
    /** 教练数据访问层（用于更新教练平均评分） */
    private final CoachMapper coachMapper;

    /**
     * 提交教练评价
     * <p>
     * 业务规则：
     * 1. 评分必须在1-5之间
     * 2. 同一学员对同一课时只能评价一次（通过lessonId+studentId校验）
     * 3. 评价提交后自动重新计算并更新教练的平均评分
     * </p>
     *
     * @param evaluation 评价实体（包含教练ID、学员ID、课时ID、评分、评价内容等）
     * @throws BusinessException 评分不合法或重复评价时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(CoachEvaluation evaluation) {
        // 校验评分范围
        if (evaluation.getRating() == null || evaluation.getRating() < 1 || evaluation.getRating() > 5) {
            throw new BusinessException("评分必须在1-5之间");
        }

        // 检查是否已评价过该课时（同一学员+同一课时只能评价一次）
        if (evaluation.getLessonId() != null) {
            Long count = evaluationMapper.selectCount(
                    new LambdaQueryWrapper<CoachEvaluation>()
                            .eq(CoachEvaluation::getLessonId, evaluation.getLessonId())
                            .eq(CoachEvaluation::getStudentId, evaluation.getStudentId())
            );
            if (count > 0) {
                throw new BusinessException("该课时已评价，不可重复评价");
            }
        }

        evaluationMapper.insert(evaluation);

        // 评价提交后自动更新教练平均评分
        updateCoachRating(evaluation.getCoachId());
    }

    /**
     * 分页获取教练的评价列表
     *
     * @param coachId  教练ID
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 分页结果，按创建时间降序排列
     */
    public Page<CoachEvaluation> pageByCoachId(Long coachId, int pageNum, int pageSize) {
        Page<CoachEvaluation> page = new Page<>(pageNum, pageSize);
        return evaluationMapper.selectPage(page,
                new LambdaQueryWrapper<CoachEvaluation>()
                        .eq(CoachEvaluation::getCoachId, coachId)
                        .orderByDesc(CoachEvaluation::getCreateTime)
        );
    }

    /**
     * 获取学员的所有评价记录
     *
     * @param studentId 学员ID
     * @return 评价列表，按创建时间降序排列
     */
    public List<CoachEvaluation> listByStudentId(Long studentId) {
        return evaluationMapper.selectList(
                new LambdaQueryWrapper<CoachEvaluation>()
                        .eq(CoachEvaluation::getStudentId, studentId)
                        .orderByDesc(CoachEvaluation::getCreateTime)
        );
    }

    /**
     * 获取教练的平均评分
     *
     * @param coachId 教练ID
     * @return 平均评分（BigDecimal），无评价时返回null
     */
    public BigDecimal getAvgRating(Long coachId) {
        return evaluationMapper.avgRatingByCoachId(coachId);
    }

    /**
     * 重新计算并更新教练的平均评分（私有方法）
     * <p>
     * 查询教练所有评价的平均分，四舍五入保留1位小数后更新到教练表
     * </p>
     *
     * @param coachId 教练ID
     */
    private void updateCoachRating(Long coachId) {
        BigDecimal avg = evaluationMapper.avgRatingByCoachId(coachId);
        if (avg != null) {
            Coach update = new Coach();
            update.setId(coachId);
            // 四舍五入保留1位小数
            update.setRating(avg.setScale(1, RoundingMode.HALF_UP));
            coachMapper.updateById(update);
        }
    }
}
