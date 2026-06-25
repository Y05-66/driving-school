package com.drivingschool.practice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.practice.entity.PracticeRecord;
import com.drivingschool.practice.entity.QuestionBank;
import com.drivingschool.practice.mapper.PracticeRecordMapper;
import com.drivingschool.practice.mapper.QuestionBankMapper;
import com.drivingschool.student.entity.Student;
import com.drivingschool.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 在线刷题服务类
 * <p>
 * 负责学员在线刷题练习的核心业务逻辑，包括：
 * - 随机获取题目供学员练习
 * - 提交答案并自动判分记录
 * - 查询学员的刷题统计数据（总题数、正确率）
 * - 获取学员的错题本（答错的题目列表）
 * </p>
 */
@Service
@RequiredArgsConstructor
public class PracticeService {

    /** 题库数据访问层 */
    private final QuestionBankMapper questionBankMapper;

    /** 练习记录数据访问层 */
    private final PracticeRecordMapper practiceRecordMapper;

    /** 学员服务，用于根据用户ID查询学员信息 */
    private final StudentService studentService;

    /**
     * 获取随机题目列表
     * <p>
     * 从指定科目的题库中随机抽取指定数量的题目，用于学员在线刷题练习。
     * 业务规则：
     * 1. 科目参数必填，仅支持科目一（1）和科目四（4）
     * 2. 获取数量默认为10题，最大不超过50题
     * 3. 若题库中该科目的题目不足请求数量，则返回全部可用题目
     * </p>
     *
     * @param subject 科目类型，1-科目一 4-科目四
     * @param count   需要获取的题目数量，默认10题
     * @return 随机抽取的题目列表
     * @throws BusinessException 科目参数不合法或题库为空时抛出
     */
    public List<QuestionBank> getRandomQuestions(Integer subject, Integer count) {
        // 参数校验：科目必须为1或4
        if (subject == null || (subject != 1 && subject != 4)) {
            throw new BusinessException("科目参数不合法，仅支持科目一（1）和科目四（4）");
        }

        // 设置默认值和上限
        if (count == null || count <= 0) {
            count = 10;
        }
        if (count > 50) {
            count = 50;
        }

        // 检查题库是否有该科目的题目
        long total = questionBankMapper.countBySubject(subject);
        if (total == 0) {
            throw new BusinessException("该科目暂无题目");
        }

        // 若请求数量超过题库总量，则取题库总量
        if (count > total) {
            count = (int) total;
        }

        return questionBankMapper.getRandomQuestions(subject, count);
    }

    /**
     * 提交答案并记录练习结果
     * <p>
     * 学员提交某道题的答案后，系统自动与正确答案比对，判断对错并记录到练习记录表。
     * 业务规则：
     * 1. 题目必须存在且未被删除
     * 2. 学员答案不能为空
     * 3. 自动比对答案并标记正确/错误
     * 4. 记录作答时间，用于后续数据分析
     * </p>
     *
     * @param userId       当前登录用户的ID
     * @param questionId   题目ID
     * @param studentAnswer 学员提交的答案
     * @param practiceTime 作答所用时间（秒），可为null
     * @return 包含正确答案、是否正确、答案解析的结果Map
     * @throws BusinessException 学员信息不存在或题目不存在时抛出
     */
    public Map<String, Object> submitAnswer(Long userId, Long questionId, String studentAnswer, Integer practiceTime) {
        // 根据用户ID查询学员信息
        Student student = studentService.getByUserId(userId);
        if (student == null) {
            throw new BusinessException("学员信息不存在");
        }

        // 查询题目信息
        QuestionBank question = questionBankMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException("题目不存在");
        }

        // 校验答案不能为空
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            throw new BusinessException("答案不能为空");
        }

        // 比对答案，标准化处理（去除空格、转大写后比对）
        String correctAnswer = question.getAnswer().trim().toUpperCase();
        String submitted = studentAnswer.trim().toUpperCase();
        boolean isCorrect = correctAnswer.equals(submitted);

        // 创建练习记录并保存
        PracticeRecord record = new PracticeRecord();
        record.setStudentId(student.getId());
        record.setQuestionId(questionId);
        record.setStudentAnswer(studentAnswer);
        record.setIsCorrect(isCorrect ? 1 : 0);
        record.setPracticeTime(practiceTime);
        practiceRecordMapper.insert(record);

        // 组装返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("correct", isCorrect);
        result.put("correctAnswer", question.getAnswer());
        result.put("explanation", question.getExplanation());
        return result;
    }

    /**
     * 获取学员的刷题统计数据
     * <p>
     * 统计学员的刷题总数、答对数量和正确率。
     * 返回数据结构：
     * - total：总刷题数
     * - correct：答对数量
     * - accuracy：正确率（百分比，保留一位小数）
     * </p>
     *
     * @param userId 当前登录用户的ID
     * @return 包含刷题统计数据的Map
     * @throws BusinessException 学员信息不存在时抛出
     */
    public Map<String, Object> getMyStats(Long userId) {
        // 根据用户ID查询学员信息
        Student student = studentService.getByUserId(userId);
        if (student == null) {
            throw new BusinessException("学员信息不存在");
        }

        // 查询统计数据
        long total = practiceRecordMapper.countTotalByStudent(student.getId());
        long correct = practiceRecordMapper.countCorrectByStudent(student.getId());

        // 计算正确率（避免除零错误）
        double accuracy = total > 0 ? (double) correct / total * 100 : 0;

        // 组装返回结果
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("correct", correct);
        stats.put("accuracy", Math.round(accuracy * 10) / 10.0);
        return stats;
    }

    /**
     * 获取学员的错题本
     * <p>
     * 查询学员所有答错的题目，按作答时间倒序排列（最新的错题排在前面）。
     * 返回的题目信息中包含学员的错误答案和正确答案，方便学员复习对比。
     * </p>
     *
     * @param userId 当前登录用户的ID
     * @return 错题列表，每条记录包含题目信息、学员答案和正确答案
     * @throws BusinessException 学员信息不存在时抛出
     */
    public List<Map<String, Object>> getWrongQuestions(Long userId) {
        // 根据用户ID查询学员信息
        Student student = studentService.getByUserId(userId);
        if (student == null) {
            throw new BusinessException("学员信息不存在");
        }

        // 查询该学员所有答错的记录，按时间倒序
        LambdaQueryWrapper<PracticeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PracticeRecord::getStudentId, student.getId())
                .eq(PracticeRecord::getIsCorrect, 0)
                .orderByDesc(PracticeRecord::getCreateTime);
        List<PracticeRecord> wrongRecords = practiceRecordMapper.selectList(wrapper);

        // 批量查询题目信息（避免 N+1 查询）
        List<Long> questionIds = wrongRecords.stream()
                .map(PracticeRecord::getQuestionId)
                .distinct()
                .toList();
        Map<Long, QuestionBank> questionMap = new HashMap<>();
        if (!questionIds.isEmpty()) {
            questionBankMapper.selectBatchIds(questionIds)
                    .forEach(q -> questionMap.put(q.getId(), q));
        }

        // 组装错题详情列表
        return wrongRecords.stream().map(record -> {
            Map<String, Object> item = new HashMap<>();
            // 从批量查询结果中获取题目详情
            QuestionBank question = questionMap.get(record.getQuestionId());
            if (question != null) {
                item.put("questionId", question.getId());
                item.put("subject", question.getSubject());
                item.put("questionType", question.getQuestionType());
                item.put("content", question.getContent());
                item.put("options", question.getOptions());
                item.put("correctAnswer", question.getAnswer());
                item.put("explanation", question.getExplanation());
            }
            item.put("studentAnswer", record.getStudentAnswer());
            item.put("practiceTime", record.getPracticeTime());
            item.put("createTime", record.getCreateTime());
            return item;
        }).toList();
    }
}
