package com.drivingschool.practice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.practice.entity.QuestionBank;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 题库Mapper接口
 * 对应实体类：{@link QuestionBank}（question_bank表）
 * 提供题库的数据库CRUD操作，继承MyBatis-Plus的BaseMapper获得通用增删改查能力
 * 同时包含题目统计和随机出题相关的自定义查询方法
 */
@Mapper
public interface QuestionBankMapper extends BaseMapper<QuestionBank> {

    /**
     * 按科目统计题目数量
     * 查询指定科目下的题目总数（排除已逻辑删除的记录）
     * 用于出题前判断题库余量是否充足
     *
     * @param subject 科目类型，1-科目一 4-科目四
     * @return 该科目下的题目总数
     */
    @Select("SELECT COUNT(*) FROM question_bank WHERE subject = #{subject} AND deleted = 0")
    long countBySubject(@Param("subject") Integer subject);

    /**
     * 随机获取指定数量的题目
     * 从指定科目中随机抽取指定数量的未删除题目，用于学员在线刷题练习
     * 使用 ORDER BY RAND() 实现随机排序，取前limit条记录
     * 注意：当题库数据量较大时，可考虑优化为其他随机策略
     *
     * @param subject 科目类型，1-科目一 4-科目四
     * @param limit   需要获取的题目数量
     * @return 随机抽取的题目列表
     */
    @Select("SELECT * FROM question_bank WHERE subject = #{subject} AND deleted = 0 ORDER BY RAND() LIMIT #{limit}")
    List<QuestionBank> getRandomQuestions(@Param("subject") Integer subject, @Param("limit") Integer limit);
}
