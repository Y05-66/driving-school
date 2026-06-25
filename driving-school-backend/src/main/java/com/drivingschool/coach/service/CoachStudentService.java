package com.drivingschool.coach.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drivingschool.coach.entity.Coach;
import com.drivingschool.coach.entity.CoachStudent;
import com.drivingschool.coach.mapper.CoachMapper;
import com.drivingschool.coach.mapper.CoachStudentMapper;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.student.entity.Student;
import com.drivingschool.student.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 教练-学员分配关系服务类
 * <p>
 * 负责管理教练与学员之间的分配关系，包括：
 * - 教练分配给学员（按科目维度，一个学员的一个科目只能分配一位教练）
 * - 换教练（解除旧分配并创建新分配）
 * - 解除分配
 * - 查询教练名下学员列表
 * - 查询学员的教练列表
 * - 分配记录查询
 * </p>
 * <p>
 * 分配状态说明：0-有效 2-已解除
 * </p>
 */
@Service
@RequiredArgsConstructor
public class CoachStudentService {

    /** 教练-学员分配关系数据访问层 */
    private final CoachStudentMapper coachStudentMapper;
    /** 教练数据访问层 */
    private final CoachMapper coachMapper;
    /** 学员数据访问层 */
    private final StudentMapper studentMapper;

    /**
     * 分配教练给学员
     * <p>
     * 业务规则：
     * 1. 教练和学员必须存在
     * 2. 同一学员的同一科目不能重复分配同一教练（检查状态为有效的记录）
     * 3. 新分配默认状态为0（有效），分配日期为当天
     * </p>
     *
     * @param coachId     教练ID
     * @param studentId   学员ID
     * @param subjectName 科目名称（如"科目一"、"科目二"等）
     * @throws BusinessException 教练/学员不存在或已存在相同分配时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long coachId, Long studentId, String subjectName) {
        Coach coach = coachMapper.selectById(coachId);
        if (coach == null) {
            throw new BusinessException("教练不存在");
        }
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException("学员不存在");
        }

        // 检查是否已有相同科目的有效分配记录
        Long count = coachStudentMapper.selectCount(
                new LambdaQueryWrapper<CoachStudent>()
                        .eq(CoachStudent::getCoachId, coachId)
                        .eq(CoachStudent::getStudentId, studentId)
                        .eq(CoachStudent::getSubjectName, subjectName)
                        .eq(CoachStudent::getStatus, 0)
        );
        if (count > 0) {
            throw new BusinessException("该学员已分配此教练负责此科目");
        }

        CoachStudent cs = new CoachStudent();
        cs.setCoachId(coachId);
        cs.setStudentId(studentId);
        cs.setSubjectName(subjectName);
        cs.setAssignDate(LocalDate.now());
        cs.setStatus(0);
        coachStudentMapper.insert(cs);
    }

    /**
     * 换教练
     * <p>
     * 事务操作，包含两步：
     * 1. 将原分配记录状态改为2（已解除），并在备注中追加"已换教练"
     * 2. 创建新的分配记录，分配给新教练
     * </p>
     *
     * @param id         原分配记录ID
     * @param newCoachId 新教练ID
     * @throws BusinessException 原分配记录或新教练不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void reassign(Long id, Long newCoachId) {
        CoachStudent existing = coachStudentMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("分配记录不存在");
        }
        Coach newCoach = coachMapper.selectById(newCoachId);
        if (newCoach == null) {
            throw new BusinessException("新教练不存在");
        }

        // 解除旧分配：状态改为已解除，追加备注
        existing.setStatus(2);
        existing.setRemark((existing.getRemark() != null ? existing.getRemark() + "\n" : "") + "已换教练");
        coachStudentMapper.updateById(existing);

        // 创建新分配：保持同一学员和科目，更换教练
        CoachStudent cs = new CoachStudent();
        cs.setCoachId(newCoachId);
        cs.setStudentId(existing.getStudentId());
        cs.setSubjectName(existing.getSubjectName());
        cs.setAssignDate(LocalDate.now());
        cs.setStatus(0);
        coachStudentMapper.insert(cs);
    }

    /**
     * 解除分配关系
     * <p>
     * 将分配记录状态改为2（已解除）
     * </p>
     *
     * @param id 分配记录ID
     * @throws BusinessException 分配记录不存在时抛出
     */
    public void release(Long id) {
        CoachStudent cs = coachStudentMapper.selectById(id);
        if (cs == null) {
            throw new BusinessException("分配记录不存在");
        }
        cs.setStatus(2);
        coachStudentMapper.updateById(cs);
    }

    /**
     * 获取教练名下所有学员
     * <p>
     * 通过查询有效的分配记录(status=0)获取教练关联的学员ID列表，
     * 再批量查询学员详情。按学员ID去重（一个学员可能分配了多个科目）。
     * </p>
     *
     * @param coachId 教练ID
     * @return 学员列表，无学员时返回空列表
     */
    public List<Student> getStudentsByCoachId(Long coachId) {
        List<CoachStudent> assignments = coachStudentMapper.selectList(
                new LambdaQueryWrapper<CoachStudent>()
                        .eq(CoachStudent::getCoachId, coachId)
                        .eq(CoachStudent::getStatus, 0)
                        .select(CoachStudent::getStudentId)
                        .groupBy(CoachStudent::getStudentId)
        );
        List<Long> studentIds = assignments.stream()
                .map(CoachStudent::getStudentId)
                .collect(Collectors.toList());
        if (studentIds.isEmpty()) {
            return Collections.emptyList();
        }
        return studentMapper.selectBatchIds(studentIds);
    }

    /**
     * 获取学员的所有教练
     * <p>
     * 通过查询有效的分配记录(status=0)获取学员关联的教练ID列表，
     * 再批量查询教练详情。按教练ID去重（一位教练可能负责多个科目）。
     * </p>
     *
     * @param studentId 学员ID
     * @return 教练列表，无教练时返回空列表
     */
    public List<Coach> getCoachesByStudentId(Long studentId) {
        List<CoachStudent> assignments = coachStudentMapper.selectList(
                new LambdaQueryWrapper<CoachStudent>()
                        .eq(CoachStudent::getStudentId, studentId)
                        .eq(CoachStudent::getStatus, 0)
                        .select(CoachStudent::getCoachId)
                        .groupBy(CoachStudent::getCoachId)
        );
        List<Long> coachIds = assignments.stream()
                .map(CoachStudent::getCoachId)
                .collect(Collectors.toList());
        if (coachIds.isEmpty()) {
            return Collections.emptyList();
        }
        return coachMapper.selectBatchIds(coachIds);
    }

    /**
     * 获取指定教练的所有分配记录（含已解除的）
     *
     * @param coachId 教练ID
     * @return 分配记录列表，按创建时间降序排列
     */
    public List<CoachStudent> listByCoachId(Long coachId) {
        return coachStudentMapper.selectList(
                new LambdaQueryWrapper<CoachStudent>()
                        .eq(CoachStudent::getCoachId, coachId)
                        .orderByDesc(CoachStudent::getCreateTime)
        );
    }

    /**
     * 获取指定学员的所有分配记录（含已解除的）
     *
     * @param studentId 学员ID
     * @return 分配记录列表，按创建时间降序排列
     */
    public List<CoachStudent> listByStudentId(Long studentId) {
        return coachStudentMapper.selectList(
                new LambdaQueryWrapper<CoachStudent>()
                        .eq(CoachStudent::getStudentId, studentId)
                        .orderByDesc(CoachStudent::getCreateTime)
        );
    }
}
