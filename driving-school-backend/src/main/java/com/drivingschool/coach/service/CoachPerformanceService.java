package com.drivingschool.coach.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drivingschool.coach.entity.CoachPerformance;
import com.drivingschool.coach.mapper.CoachPerformanceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 教练绩效服务类
 * 提供绩效数据的计算、查询和排名功能
 *
 * @author drivingschool
 */
@Service
@RequiredArgsConstructor
public class CoachPerformanceService extends ServiceImpl<CoachPerformanceMapper, CoachPerformance> {

    /** 绩效Mapper，用于查询和保存绩效记录 */
    private final CoachPerformanceMapper performanceMapper;
    /** JDBC模板，用于执行原始SQL聚合查询 */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 计算指定月份的教练绩效
     * 从lesson、exam_enrollment、exam、coach_evaluation表汇总数据，计算综合绩效得分
     * 综合得分 = 课时完成得分(30%) + 考试通过率得分(30%) + 学员评分得分(25%) + 综合评价得分(15%)
     *
     * @param month 统计月份，格式：YYYY-MM
     * @return 计算完成的绩效记录数
     */
    @Transactional(rollbackFor = Exception.class)
    public int calculate(String month) {
        // 查询所有教练的绩效基础数据
        // 考试通过率：通过exam_enrollment和exam表关联，按coach_id（通过lesson关联）分组统计
        // 投诉扣分：暂无投诉表，complaint_count固定为0
        String sql = "SELECT c.id AS coach_id, " +
                "COUNT(DISTINCT l.id) AS total_lessons, " +
                "SUM(CASE WHEN l.status = 3 THEN 1 ELSE 0 END) AS completed_lessons, " +
                "COUNT(DISTINCT l.student_id) AS student_count, " +
                "COALESCE(exam.pass_rate, 0) AS pass_rate, " +
                "COALESCE(eval.avg_rating, 0) AS avg_rating " +
                "FROM coach c " +
                "LEFT JOIN lesson l ON c.id = l.coach_id AND DATE_FORMAT(l.lesson_date, '%Y-%m') = ? " +
                "LEFT JOIN (" +
                "    SELECT l2.coach_id, " +
                "           ROUND(SUM(CASE WHEN ee.status = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) AS pass_rate " +
                "    FROM exam_enrollment ee " +
                "    INNER JOIN exam e ON ee.exam_id = e.id AND ee.deleted = 0 " +
                "    INNER JOIN lesson l2 ON ee.student_id = l2.student_id AND l2.deleted = 0 " +
                "    WHERE DATE_FORMAT(e.exam_date, '%Y-%m') = ? " +
                "    GROUP BY l2.coach_id" +
                ") exam ON c.id = exam.coach_id " +
                "LEFT JOIN (" +
                "    SELECT coach_id, ROUND(AVG(rating), 2) AS avg_rating " +
                "    FROM coach_evaluation " +
                "    WHERE DATE_FORMAT(create_time, '%Y-%m') = ? " +
                "    GROUP BY coach_id" +
                ") eval ON c.id = eval.coach_id " +
                "WHERE c.deleted = 0 " +
                "GROUP BY c.id";

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, month, month, month);

        int count = 0;
        for (Map<String, Object> row : results) {
            Long coachId = ((Number) row.get("coach_id")).longValue();
            Integer totalLessons = ((Number) row.get("total_lessons")).intValue();
            Integer completedLessons = ((Number) row.get("completed_lessons")).intValue();
            Integer studentCount = ((Number) row.get("student_count")).intValue();
            BigDecimal passRate = new BigDecimal(row.get("pass_rate").toString());
            BigDecimal avgRating = new BigDecimal(row.get("avg_rating").toString());
            // 暂无投诉表，投诉数固定为0
            int complaintCount = 0;

            // 计算综合绩效得分
            // 课时完成得分（满分30分）：完成率 * 30
            BigDecimal lessonScore = BigDecimal.ZERO;
            if (totalLessons > 0) {
                lessonScore = BigDecimal.valueOf(completedLessons)
                        .divide(BigDecimal.valueOf(totalLessons), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(30));
            }

            // 考试通过率得分（满分30分）：通过率 * 0.3
            BigDecimal examScore = passRate.multiply(BigDecimal.valueOf(0.3));

            // 学员评分得分（满分25分）：评分/5 * 25
            BigDecimal ratingScore = avgRating.divide(BigDecimal.valueOf(5), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(25));

            // 投诉扣分（每投诉一次扣5分，最多扣15分）
            BigDecimal complaintDeduct = BigDecimal.valueOf(Math.min(complaintCount * 5, 15));

            // 综合得分 = 课时得分 + 考试得分 + 评分得分 - 投诉扣分
            BigDecimal totalScore = lessonScore.add(examScore).add(ratingScore).subtract(complaintDeduct);
            // 确保得分在0-100之间
            totalScore = totalScore.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100));
            totalScore = totalScore.setScale(2, RoundingMode.HALF_UP);

            // 查询是否已存在该月绩效记录
            LambdaQueryWrapper<CoachPerformance> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CoachPerformance::getCoachId, coachId)
                   .eq(CoachPerformance::getMonth, month);
            CoachPerformance existing = performanceMapper.selectOne(wrapper);

            if (existing != null) {
                // 更新已有记录
                existing.setTotalLessons(totalLessons);
                existing.setCompletedLessons(completedLessons);
                existing.setStudentCount(studentCount);
                existing.setPassRate(passRate);
                existing.setAvgRating(avgRating);
                existing.setComplaintCount(complaintCount);
                existing.setScore(totalScore);
                performanceMapper.updateById(existing);
            } else {
                // 创建新记录
                CoachPerformance performance = new CoachPerformance();
                performance.setCoachId(coachId);
                performance.setMonth(month);
                performance.setTotalLessons(totalLessons);
                performance.setCompletedLessons(completedLessons);
                performance.setStudentCount(studentCount);
                performance.setPassRate(passRate);
                performance.setAvgRating(avgRating);
                performance.setComplaintCount(complaintCount);
                performance.setScore(totalScore);
                performanceMapper.insert(performance);
            }
            count++;
        }

        return count;
    }

    /**
     * 根据教练ID查询绩效记录
     * 返回该教练的所有月份绩效数据，按月份倒序排列
     *
     * @param coachId 教练ID
     * @return 该教练的所有绩效记录列表
     */
    public List<CoachPerformance> getByCoachId(Long coachId) {
        LambdaQueryWrapper<CoachPerformance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CoachPerformance::getCoachId, coachId)
               .orderByDesc(CoachPerformance::getMonth);
        return performanceMapper.selectList(wrapper);
    }

    /**
     * 查询有绩效记录的月份列表
     * 返回去重后的月份列表，按月份倒序排列
     *
     * @return 月份列表（格式：YYYY-MM）
     */
    public List<String> getMonthList() {
        List<CoachPerformance> list = performanceMapper.selectList(
                new LambdaQueryWrapper<CoachPerformance>()
                        .select(CoachPerformance::getMonth)
                        .groupBy(CoachPerformance::getMonth)
                        .orderByDesc(CoachPerformance::getMonth)
        );
        return list.stream()
                .map(CoachPerformance::getMonth)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取指定月份的教练绩效排名
     * 按综合得分从高到低排列
     *
     * @param month 统计月份，格式：YYYY-MM
     * @return 按得分排序的绩效记录列表
     */
    public List<CoachPerformance> getRanking(String month) {
        LambdaQueryWrapper<CoachPerformance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CoachPerformance::getMonth, month)
               .orderByDesc(CoachPerformance::getScore);
        return performanceMapper.selectList(wrapper);
    }
}
