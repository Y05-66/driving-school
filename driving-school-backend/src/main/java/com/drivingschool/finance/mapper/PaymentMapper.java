package com.drivingschool.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.finance.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 缴费记录Mapper接口
 * 对应实体类：{@link Payment}（payment表）
 * 提供缴费记录的数据库CRUD操作，继承MyBatis-Plus的BaseMapper获得通用增删改查能力
 * 同时包含财务统计相关的自定义查询方法，用于报表和仪表盘数据展示
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {

    /**
     * 按日期范围统计缴费总额
     * 统计指定时间区间内的缴费总金额（左闭右开区间）
     * 用于首页仪表盘和财务报表展示
     *
     * @param start 开始时间（包含）
     * @param end   结束时间（不包含）
     * @return 时间区间内的缴费总额，无记录时返回0
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM payment WHERE pay_time >= #{start} AND pay_time < #{end} AND deleted = 0")
    BigDecimal sumAmountByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 按缴费类型分组统计金额
     * 统计各缴费类型（报名费、培训费、考试费等）的累计金额
     * 用于财务分析报表展示收入构成
     *
     * @return 包含type（缴费类型）和amount（累计金额）的Map列表
     */
    @Select("SELECT type, COALESCE(SUM(amount), 0) AS amount FROM payment WHERE deleted = 0 GROUP BY type")
    List<Map<String, Object>> sumGroupByType();

    /**
     * 按支付方式分组统计金额
     * 统计各支付方式（现金、微信、支付宝、银行卡等）的累计金额
     * 用于财务分析报表展示支付渠道分布
     *
     * @return 包含payMethod（支付方式）和amount（累计金额）的Map列表
     */
    @Select("SELECT pay_method AS payMethod, COALESCE(SUM(amount), 0) AS amount FROM payment WHERE deleted = 0 GROUP BY pay_method")
    List<Map<String, Object>> sumGroupByPayMethod();

    /**
     * 按月份分组统计缴费金额
     * 根据缴费时间按年月分组统计每月的缴费总额
     * 用于生成财务收入趋势图表数据
     *
     * @return 包含period（年月，格式YYYY-MM）和amount（累计金额）的Map列表
     */
    @Select("SELECT DATE_FORMAT(pay_time, '%Y-%m') AS period, COALESCE(SUM(amount), 0) AS amount FROM payment WHERE deleted = 0 GROUP BY DATE_FORMAT(pay_time, '%Y-%m') ORDER BY period")
    List<Map<String, Object>> sumGroupByMonth();

    /**
     * 按收据编号前缀统计记录数
     * 查询以指定前缀开头的收据编号数量，用于生成递增的收据编号
     * 通过统计已有编号数量来确定下一个序号，避免编号冲突
     *
     * @param prefix 收据编号前缀（如：日期格式"20260623"）
     * @return 该前缀下的已有收据数量
     */
    @Select("SELECT COUNT(*) FROM payment WHERE receipt_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0")
    long countByReceiptNoPrefix(@Param("prefix") String prefix);
}
