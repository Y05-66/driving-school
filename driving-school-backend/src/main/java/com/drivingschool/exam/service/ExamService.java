package com.drivingschool.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.common.constant.Constants;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.exam.entity.Exam;
import com.drivingschool.exam.entity.ExamEnrollment;
import com.drivingschool.exam.mapper.ExamEnrollmentMapper;
import com.drivingschool.exam.mapper.ExamMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试管理服务类
 * <p>
 * 负责驾校考试相关的全生命周期管理，包括：
 * - 考试安排的创建与分页查询
 * - 学员考试报名（含防重复报名、名额控制、补考次数限制、补考间隔校验）
 * - 考试结果录入与更新
 * - 报名记录的分页查询
 * - 学员考试历史查询
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ExamService {

    /** 考试安排数据访问层 */
    private final ExamMapper examMapper;
    /** 考试报名记录数据访问层 */
    private final ExamEnrollmentMapper enrollmentMapper;

    /**
     * 分页查询考试安排列表
     *
     * @param page      分页参数对象
     * @param examType  考试类型（可选，如"科目一"、"科目二"等）
     * @param applyType 报考车型类型（可选，如"C1"、"C2"等）
     * @param examDate  考试日期下限（可选，筛选该日期及之后的考试）
     * @return 分页结果，按考试日期升序排列
     */
    public Page<Exam> page(Page<Exam> page, String examType, String applyType, LocalDate examDate) {
        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(examType != null, Exam::getExamType, examType)
                .eq(applyType != null, Exam::getApplyType, applyType)
                .ge(examDate != null, Exam::getExamDate, examDate)
                .orderByAsc(Exam::getExamDate);
        return examMapper.selectPage(page, wrapper);
    }

    /**
     * 根据ID获取考试安排详情
     *
     * @param id 考试安排ID
     * @return 考试安排实体，不存在时返回null
     */
    public Exam getById(Long id) {
        return examMapper.selectById(id);
    }

    /**
     * 创建考试安排
     *
     * @param exam 考试安排实体
     */
    public void create(Exam exam) {
        examMapper.insert(exam);
    }

    /**
     * 学员考试报名
     * <p>
     * 业务规则：
     * 1. 考试安排必须存在
     * 2. 同一学员不能对同一考试重复报名（已有待审核或已通过的报名记录则拒绝）
     * 3. 考试名额限制：报名人数不得超过考试安排的最大考生数
     * 4. 补考次数限制：同一科目（按考试类型判断）的未通过次数不得超过上限（Constants.MAX_RETAKE_COUNT）
     * 5. 补考间隔限制：上次未通过后需间隔一定天数（Constants.RETAKE_INTERVAL_DAYS）才能再次报名
     * </p>
     *
     * @param examId    考试安排ID
     * @param studentId 学员ID
     * @throws BusinessException 违反任何业务规则时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void enroll(Long examId, Long studentId) {
        // 采用乐观策略：先做业务校验，依赖数据库唯一约束（DuplicateKeyException）防止并发超额。
        // 不使用synchronized块，避免与@Transactional组合导致死锁和内存泄漏（锁映射无界增长）。
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException("考试安排不存在");
        }

        // 检查是否有同一考试的未完成报名（状态0-待审核或1-已通过）
        Long duplicateCount = enrollmentMapper.selectCount(
                new LambdaQueryWrapper<ExamEnrollment>()
                        .eq(ExamEnrollment::getStudentId, studentId)
                        .eq(ExamEnrollment::getExamId, examId)
                        .in(ExamEnrollment::getStatus, 0, 1)
        );
        if (duplicateCount > 0) {
            throw new BusinessException("已报名该考试，不可重复报名");
        }

        // 检查考试名额是否已满（存在小的TOCTOU窗口，由DuplicateKeyException兜底）
        long enrolledCount = enrollmentMapper.selectCount(
                new LambdaQueryWrapper<ExamEnrollment>()
                        .eq(ExamEnrollment::getExamId, examId)
                        .in(ExamEnrollment::getStatus, 0, 1)
        );
        if (enrolledCount >= exam.getMaxCandidates()) {
            throw new BusinessException("名额已满");
        }

        // 检查该科目的补考次数
        List<Exam> sameTypeExams = examMapper.selectList(
                new LambdaQueryWrapper<Exam>().eq(Exam::getExamType, exam.getExamType())
        );
        List<Long> sameTypeExamIds = sameTypeExams.stream().map(Exam::getId).toList();

        long retakeCount = 0;
        if (!sameTypeExamIds.isEmpty()) {
            retakeCount = enrollmentMapper.selectCount(
                    new LambdaQueryWrapper<ExamEnrollment>()
                            .eq(ExamEnrollment::getStudentId, studentId)
                            .eq(ExamEnrollment::getStatus, 2)
                            .in(ExamEnrollment::getExamId, sameTypeExamIds)
            );
            if (retakeCount >= Constants.MAX_RETAKE_COUNT) {
                throw new BusinessException("该科目补考次数已达上限");
            }

            // 检查补考间隔
            if (retakeCount > 0) {
                ExamEnrollment lastFailed = enrollmentMapper.selectOne(
                        new LambdaQueryWrapper<ExamEnrollment>()
                                .eq(ExamEnrollment::getStudentId, studentId)
                                .eq(ExamEnrollment::getStatus, 2)
                                .in(ExamEnrollment::getExamId, sameTypeExamIds)
                                .orderByDesc(ExamEnrollment::getCreateTime)
                                .last("LIMIT 1")
                );
                if (lastFailed != null && lastFailed.getCreateTime() != null) {
                    long daysSinceLastExam = java.time.Duration.between(lastFailed.getCreateTime(), LocalDateTime.now()).toDays();
                    if (daysSinceLastExam < Constants.RETAKE_INTERVAL_DAYS) {
                        throw new BusinessException(1004, "补考间隔不足" + Constants.RETAKE_INTERVAL_DAYS + "天");
                    }
                }
            }
        }

        // 所有校验通过，创建报名记录
        // DuplicateKeyException处理并发场景下的重复报名
        ExamEnrollment enrollment = new ExamEnrollment();
        enrollment.setStudentId(studentId);
        enrollment.setExamId(examId);
        enrollment.setApplyTime(LocalDateTime.now());
        enrollment.setStatus(0);
        try {
            enrollmentMapper.insert(enrollment);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new BusinessException("报名失败，请重试");
        }
    }

    /**
     * 更新考试结果
     * <p>
     * 录入学员的考试成绩和最终状态
     * </p>
     *
     * @param enrollmentId 报名记录ID
     * @param score        考试成绩/分数（0-100）
     * @param status       考试结果状态（1-通过 2-未通过）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateResult(Long enrollmentId, Integer score, Integer status) {
        // 参数验证
        if (score == null || score < 0 || score > 100) {
            throw new BusinessException("成绩必须在0-100之间");
        }
        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException("状态只能为1(通过)或2(未通过)");
        }

        // 先校验报名记录是否存在
        ExamEnrollment existing = enrollmentMapper.selectById(enrollmentId);
        if (existing == null) {
            throw new BusinessException("报名记录不存在");
        }
        // 校验状态流转：允许从待审核(0)、已通过(1)、未通过(2)状态录入/修正结果
        if (existing.getStatus() != 0 && existing.getStatus() != 1 && existing.getStatus() != 2) {
            throw new BusinessException("当前状态不允许录入结果");
        }
        ExamEnrollment enrollment = new ExamEnrollment();
        enrollment.setId(enrollmentId);
        enrollment.setScore(score);
        enrollment.setStatus(status);
        enrollmentMapper.updateById(enrollment);
    }

    /**
     * 分页查询考试报名记录
     *
     * @param page      分页参数对象
     * @param examId    考试安排ID（可选筛选条件）
     * @param studentId 学员ID（可选筛选条件）
     * @param status    报名状态（可选：0-待审核 1-已通过 2-未通过）
     * @return 分页结果，按报名时间降序排列
     */
    public Page<ExamEnrollment> enrollmentPage(Page<ExamEnrollment> page, Long examId, Long studentId, Integer status) {
        LambdaQueryWrapper<ExamEnrollment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(examId != null, ExamEnrollment::getExamId, examId)
                .eq(studentId != null, ExamEnrollment::getStudentId, studentId)
                .eq(status != null, ExamEnrollment::getStatus, status)
                .orderByDesc(ExamEnrollment::getApplyTime);
        return enrollmentMapper.selectPage(page, wrapper);
    }

    /**
     * 获取学员的考试历史记录
     *
     * @param studentId 学员ID
     * @return 考试报名记录列表，按报名时间降序排列
     */
    public List<ExamEnrollment> getExamHistory(Long studentId) {
        return enrollmentMapper.selectList(
                new LambdaQueryWrapper<ExamEnrollment>()
                        .eq(ExamEnrollment::getStudentId, studentId)
                        .orderByDesc(ExamEnrollment::getApplyTime)
        );
    }
}
