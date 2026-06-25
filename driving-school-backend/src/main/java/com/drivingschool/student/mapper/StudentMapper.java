package com.drivingschool.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.student.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 学员Mapper接口
 * 对应实体类：{@link Student}（student表）
 * 提供学员的数据库CRUD操作，继承MyBatis-Plus的BaseMapper获得通用增删改查能力
 * 同时包含学员统计相关的自定义查询方法
 */
@Mapper
public interface StudentMapper extends BaseMapper<Student> {

    /**
     * 按学员状态统计人数
     * 查询指定状态下的学员总数（排除已逻辑删除的记录）
     *
     * @param status 学员状态值（如：0-已报名 1-学习中 2-已结业等）
     * @return 该状态下的学员数量
     */
    @Select("SELECT COUNT(*) FROM student WHERE status = #{status} AND deleted = 0")
    long countByStatus(@Param("status") int status);

    /**
     * 按月份分组统计学员报名人数
     * 根据学员的报名日期(register_date)按年月分组统计每月报名人数
     * 用于生成报名趋势图表数据
     *
     * @return 包含period（年月，格式YYYY-MM）和count（人数）的Map列表
     */
    @Select("SELECT DATE_FORMAT(register_date, '%Y-%m') AS period, COUNT(*) AS count FROM student WHERE deleted = 0 GROUP BY DATE_FORMAT(register_date, '%Y-%m') ORDER BY period")
    List<Map<String, Object>> countGroupByMonth();
}
