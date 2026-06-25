package com.drivingschool.practice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.practice.entity.PracticeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 练习记录Mapper接口
 * 对应实体类：{@link PracticeRecord}（practice_record表）
 * 提供练习记录的数据库CRUD操作，继承MyBatis-Plus的BaseMapper获得通用增删改查能力
 * 同时包含学员刷题统计相关的自定义查询方法
 */
@Mapper
public interface PracticeRecordMapper extends BaseMapper<PracticeRecord> {

    /**
     * 统计学员答对的题目数量
     * 查询指定学员在所有练习记录中答对的总题数（排除已逻辑删除的记录）
     * 用于计算学员的刷题正确率
     *
     * @param studentId 学员ID
     * @return 该学员答对的题目总数
     */
    @Select("SELECT COUNT(*) FROM practice_record WHERE student_id = #{studentId} AND is_correct = 1 AND deleted = 0")
    long countCorrectByStudent(@Param("studentId") Long studentId);

    /**
     * 统计学员的总答题数量
     * 查询指定学员的所有练习记录总数（排除已逻辑删除的记录）
     * 用于计算学员的刷题总数和正确率
     *
     * @param studentId 学员ID
     * @return 该学员的总答题数量
     */
    @Select("SELECT COUNT(*) FROM practice_record WHERE student_id = #{studentId} AND deleted = 0")
    long countTotalByStudent(@Param("studentId") Long studentId);
}
