package com.drivingschool.student.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.course.entity.Course;
import com.drivingschool.course.mapper.CourseMapper;
import com.drivingschool.student.entity.StudentProgress;
import com.drivingschool.student.mapper.StudentProgressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 学员学习进度服务类
 * <p>
 * 负责学员各科学习进度的管理，包括：
 * - 新学员注册时初始化四科（科目一至科目四）学习进度记录
 * - 课时完成后自动累加已完成学时
 * - 进度状态自动流转（未开始 -> 学习中 -> 已完成）
 * - 手动更新进度（管理员/教练操作）
 * - 查询学员各科进度
 * </p>
 * <p>
 * 状态说明：0-未开始 1-学习中 2-已完成
 * </p>
 */
@Service
@RequiredArgsConstructor
public class StudentProgressService {

    /** 学员进度数据访问层 */
    private final StudentProgressMapper progressMapper;
    /** 课程数据访问层（用于获取各科所需学时） */
    private final CourseMapper courseMapper;

    /**
     * 根据学员报名类型初始化四科进度记录
     * <p>
     * 为新注册学员创建科目一至科目四的进度记录：
     * 1. 根据报名类型（如C1、C2）查询对应的课程配置
     * 2. 优先使用课程配置中的所需学时
     * 3. 如果课程配置中没有对应科目，使用默认学时（getDefaultHours方法）
     * 4. 所有科目的初始状态为0（未开始），已完成学时为0
     * </p>
     *
     * @param studentId 学员ID
     * @param applyType 报考类型（如"C1"、"C2"等）
     */
    @Transactional(rollbackFor = Exception.class)
    public void initProgress(Long studentId, String applyType) {
        // 查询该报考类型下所有课程配置
        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>().eq(Course::getApplyType, applyType)
        );
        // 为四个科目分别创建进度记录
        String[] subjects = {"科目一", "科目二", "科目三", "科目四"};
        for (int i = 0; i < subjects.length; i++) {
            final String subjectName = subjects[i];
            // 在课程配置中查找匹配的科目
            Course matched = courses.stream()
                    .filter(c -> subjectName.equals(c.getName()))
                    .findFirst()
                    .orElse(null);

            StudentProgress progress = new StudentProgress();
            progress.setStudentId(studentId);
            progress.setSubjectName(subjects[i]);
            // 优先使用课程配置的所需学时，否则使用默认值
            progress.setRequiredHours(matched != null ? matched.getTotalHours() : getDefaultHours(subjects[i]));
            progress.setCompletedHours(0);
            progress.setStatus(0);
            progressMapper.insert(progress);
        }
    }

    /**
     * 获取指定学员的所有科目进度列表
     *
     * @param studentId 学员ID
     * @return 进度列表，按科目名称排序
     */
    public List<StudentProgress> getByStudentId(Long studentId) {
        return progressMapper.selectList(
                new LambdaQueryWrapper<StudentProgress>()
                        .eq(StudentProgress::getStudentId, studentId)
                        .orderByAsc(StudentProgress::getSubjectName)
        );
    }

    /**
     * 获取指定学员指定科目的进度记录
     *
     * @param studentId   学员ID
     * @param subjectName 科目名称（如"科目一"）
     * @return 进度记录列表
     */
    public List<StudentProgress> getByStudentAndSubject(Long studentId, String subjectName) {
        return progressMapper.selectList(
                new LambdaQueryWrapper<StudentProgress>()
                        .eq(StudentProgress::getStudentId, studentId)
                        .eq(StudentProgress::getSubjectName, subjectName)
        );
    }

    /**
     * 完成课时后累加学时
     * <p>
     * 业务逻辑：
     * 1. 查找学员对应科目的进度记录
     * 2. 将本次课时累加到已完成学时
     * 3. 如果当前状态为"未开始(0)"，自动变更为"学习中(1)"并记录开始日期
     * 4. 如果累计学时达到或超过所需学时，自动变更为"已完成(2)"并记录完成日期
     * </p>
     *
     * @param studentId   学员ID
     * @param subjectName 科目名称
     * @param hours       本次完成的课时数
     */
    public void addHours(Long studentId, String subjectName, int hours) {
        List<StudentProgress> list = getByStudentAndSubject(studentId, subjectName);
        if (list.isEmpty()) {
            return;
        }
        StudentProgress progress = list.get(0);
        int newCompleted = progress.getCompletedHours() + hours;
        progress.setCompletedHours(newCompleted);

        // 如果是未开始状态，自动切换为学习中并记录开始日期
        if (progress.getStatus() == 0) {
            progress.setStatus(1);
            progress.setStartDate(LocalDate.now());
        }

        // 如果已完成学时达到要求，标记为已完成并记录完成日期
        if (newCompleted >= progress.getRequiredHours()) {
            progress.setStatus(2);
            progress.setCompleteDate(LocalDate.now());
        }

        progressMapper.updateById(progress);
    }

    /**
     * 手动更新进度（管理员/教练操作）
     * <p>
     * 支持单独更新已完成学时或状态，也可同时更新：
     * - 更新状态为1（学习中）时，若开始日期为空则自动设置
     * - 更新状态为2（已完成）时，自动设置完成日期
     * </p>
     *
     * @param id             进度记录ID
     * @param completedHours 已完成学时（可选，传null则不更新）
     * @param status         进度状态（可选，传null则不更新）
     * @throws BusinessException 进度记录不存在时抛出
     */
    public void updateProgress(Long id, Integer completedHours, Integer status) {
        updateProgress(id, completedHours, status, null);
    }

    /**
     * 更新学员科目进度（带归属校验）
     *
     * @param id             进度记录ID
     * @param completedHours 已完成学时（可选）
     * @param status         进度状态（可选）
     * @param studentId      学员ID（可选，传入时校验进度记录是否属于该学员）
     */
    public void updateProgress(Long id, Integer completedHours, Integer status, Long studentId) {
        StudentProgress progress = progressMapper.selectById(id);
        if (progress == null) {
            throw new BusinessException("进度记录不存在");
        }
        // 校验进度记录是否属于指定学员
        if (studentId != null && !studentId.equals(progress.getStudentId())) {
            throw new BusinessException("进度记录不属于该学员");
        }
        if (completedHours != null) {
            progress.setCompletedHours(completedHours);
        }
        if (status != null) {
            progress.setStatus(status);
            // 设置开始日期（状态变为学习中且开始日期为空时）
            if (status == 1 && progress.getStartDate() == null) {
                progress.setStartDate(LocalDate.now());
            }
            // 设置完成日期（状态变为已完成时）
            if (status == 2) {
                progress.setCompleteDate(LocalDate.now());
            }
        }
        progressMapper.updateById(progress);
    }

    /**
     * 获取各科目的默认所需学时（私有方法）
     * <p>
     * 当课程配置中未找到对应科目的学时设置时使用此默认值：
     * - 科目一：10学时
     * - 科目二：16学时
     * - 科目三：24学时
     * - 科目四：10学时
     * </p>
     *
     * @param subjectName 科目名称
     * @return 默认所需学时数
     */
    private int getDefaultHours(String subjectName) {
        return switch (subjectName) {
            case "科目一" -> 10;
            case "科目二" -> 16;
            case "科目三" -> 24;
            case "科目四" -> 10;
            default -> 10;
        };
    }
}
