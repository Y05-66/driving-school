package com.drivingschool.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.exam.entity.ExamEnrollment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 考试报名记录Mapper接口
 * 对应实体类：{@link ExamEnrollment}（exam_enrollment表）
 * 提供考试报名记录的数据库CRUD操作，继承MyBatis-Plus的BaseMapper获得通用增删改查能力
 * 同时包含考试报名统计相关的自定义查询方法
 */
@Mapper
public interface ExamEnrollmentMapper extends BaseMapper<ExamEnrollment> {

    /**
     * 按报名状态统计人数
     * 查询指定状态下的考试报名记录总数（排除已逻辑删除的记录）
     *
     * @param status 报名状态值（如：0-已报名 1-已确认 2-已取消 3-已考试等）
     * @return 该状态下的报名记录数量
     */
    @Select("SELECT COUNT(*) FROM exam_enrollment WHERE status = #{status} AND deleted = 0")
    long countByStatus(@Param("status") int status);

    /**
     * 按考试场次和状态分组统计报名人数
     * 统计每场考试各个报名状态的人数分布
     * 用于考试管理页面展示各场次的报名情况概览
     *
     * @return 包含examId（考试ID）、status（报名状态）、count（人数）的Map列表
     */
    @Select("SELECT exam_id AS examId, status, COUNT(*) AS count FROM exam_enrollment WHERE deleted = 0 GROUP BY exam_id, status")
    List<Map<String, Object>> countGroupByExamAndStatus();
}
