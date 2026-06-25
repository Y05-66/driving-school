package com.drivingschool.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.common.utils.RedisUtils;
import com.drivingschool.course.entity.Course;
import com.drivingschool.course.entity.Lesson;
import com.drivingschool.course.mapper.CourseMapper;
import com.drivingschool.course.mapper.LessonMapper;
import com.drivingschool.finance.entity.Payment;
import com.drivingschool.finance.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 支付与财务管理服务类
 * <p>
 * 负责驾校财务相关的核心业务逻辑，包括：
 * - 支付记录的创建与分页查询
 * - 退费处理（使用条件更新防止并发重复退费）
 * - 收入汇总统计（今日收入、本月收入、按类型/支付方式分组统计）
 * - 日报查询
 * - 教练课时费结算（根据已完成课时计算教练应得报酬）
 * - 收据编号生成（基于Redis的分布式序列号，带数据库降级方案）
 * </p>
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    /** 支付记录数据访问层 */
    private final PaymentMapper paymentMapper;
    /** 课程预约数据访问层（用于教练课时费结算） */
    private final LessonMapper lessonMapper;
    /** 课程数据访问层（用于获取课程单价） */
    private final CourseMapper courseMapper;
    /** Redis工具类（用于生成收据序列号） */
    private final RedisUtils redisUtils;

    /**
     * 分页查询支付记录
     *
     * @param page      分页参数对象
     * @param studentId 学员ID（可选筛选条件）
     * @param type      支付类型（可选：1-报名费 2-课时费 3-补考费 4-其他）
     * @param payMethod 支付方式（可选：1-现金 2-微信 3-支付宝 4-银行卡）
     * @param startTime 支付时间范围起始（可选）
     * @param endTime   支付时间范围结束（可选）
     * @return 分页结果，按支付时间降序排列
     */
    public Page<Payment> page(Page<Payment> page, Long studentId, Integer type, Integer payMethod,
                              LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(studentId != null, Payment::getStudentId, studentId)
                .eq(type != null, Payment::getType, type)
                .eq(payMethod != null, Payment::getPayMethod, payMethod)
                .ge(startTime != null, Payment::getPayTime, startTime)
                .le(endTime != null, Payment::getPayTime, endTime)
                .orderByDesc(Payment::getPayTime);
        return paymentMapper.selectPage(page, wrapper);
    }

    /**
     * 创建支付记录
     * <p>
     * 业务规则：
     * 1. 金额必须大于0
     * 2. 学员ID不能为空
     * 3. 自动生成收据编号
     * 4. 自动记录支付时间为当前时间
     * 5. 退费状态初始化为0（未退费）
     * </p>
     *
     * @param payment 支付记录实体
     * @throws BusinessException 金额非法或学员ID为空时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"overview", "dashboard", "revenueTrend"}, allEntries = true)
    public void create(Payment payment) {
        // 校验金额合法性
        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("金额必须大于0");
        }
        // 校验学员ID
        if (payment.getStudentId() == null) {
            throw new BusinessException("学员ID不能为空");
        }
        // 自动生成收据编号、设置支付时间和退费状态
        payment.setReceiptNo(generateReceiptNo());
        payment.setPayTime(LocalDateTime.now());
        payment.setRefundStatus(0);
        paymentMapper.insert(payment);
    }

    /**
     * 获取指定学员的所有支付记录
     *
     * @param studentId 学员ID
     * @return 支付记录列表，按支付时间降序排列
     */
    public List<Payment> getByStudentId(Long studentId) {
        return paymentMapper.selectList(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getStudentId, studentId)
                        .orderByDesc(Payment::getPayTime)
        );
    }

    /**
     * 获取收入汇总统计
     * <p>
     * 返回内容包括：
     * - todayRevenue：今日收入总额
     * - monthRevenue：本月收入总额
     * - byType：按支付类型分组的收入（key为类型编号，value为金额）
     * - byMethod：按支付方式分组的收入（key为方式编号，value为金额）
     * </p>
     *
     * @return 包含汇总数据的Map
     */
    public Map<String, Object> getSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

        // 查询今日收入和本月收入（SUM无记录时返回null，转为0）
        BigDecimal todayRevenue = paymentMapper.sumAmountByDateRange(todayStart, todayEnd);
        if (todayRevenue == null) todayRevenue = BigDecimal.ZERO;
        BigDecimal monthRevenue = paymentMapper.sumAmountByDateRange(monthStart, todayEnd);
        if (monthRevenue == null) monthRevenue = BigDecimal.ZERO;

        // 按支付类型分组统计
        List<Map<String, Object>> byTypeList = paymentMapper.sumGroupByType();
        Map<Integer, BigDecimal> byType = new HashMap<>();
        for (Map<String, Object> row : byTypeList) {
            byType.put(((Number) row.get("type")).intValue(), (BigDecimal) row.get("amount"));
        }

        // 按支付方式分组统计
        List<Map<String, Object>> byMethodList = paymentMapper.sumGroupByPayMethod();
        Map<Integer, BigDecimal> byMethod = new HashMap<>();
        for (Map<String, Object> row : byMethodList) {
            byMethod.put(((Number) row.get("payMethod")).intValue(), (BigDecimal) row.get("amount"));
        }

        // 组装汇总结果
        Map<String, Object> summary = new HashMap<>();
        summary.put("todayRevenue", todayRevenue);
        summary.put("monthRevenue", monthRevenue);
        summary.put("byType", byType);
        summary.put("byMethod", byMethod);
        return summary;
    }

    /**
     * 获取指定日期的日报数据
     *
     * @param date 查询日期
     * @return 该日期的所有支付记录，按支付时间降序排列
     */
    public List<Payment> getDailyReport(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        return paymentMapper.selectList(
                new LambdaQueryWrapper<Payment>()
                        .ge(Payment::getPayTime, start)
                        .le(Payment::getPayTime, end)
                        .orderByDesc(Payment::getPayTime)
        );
    }

    /**
     * 退费处理
     * <p>
     * 业务规则：
     * 1. 原支付记录必须存在
     * 2. 原支付记录不能已退费（防止重复退费）
     * 3. 使用条件更新（WHERE refund_status=0）防止并发场景下的重复退费
     * 4. 退费成功后创建一条负金额的退费记录，关联原始支付单号
     * </p>
     * <p>
     * 并发安全说明：通过条件更新（乐观锁方式）确保同一笔支付记录在并发退费时只有一个请求能成功
     * </p>
     *
     * @param paymentId  原支付记录ID
     * @param operatorId 操作人ID
     * @throws BusinessException 原记录不存在、已退费或并发退费失败时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long paymentId, Long operatorId) {
        Payment original = paymentMapper.selectById(paymentId);
        if (original == null) {
            throw new BusinessException("原支付记录不存在");
        }
        if (original.getRefundStatus() != null && original.getRefundStatus() == 1) {
            throw new BusinessException("该记录已退费");
        }

        // 条件更新：只有 refund_status=0 时才能更新成功，防止并发重复退费
        Payment update = new Payment();
        update.setId(paymentId);
        update.setRefundStatus(1);
        update.setRefundTime(LocalDateTime.now());
        int rows = paymentMapper.update(update,
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getId, paymentId)
                        .eq(Payment::getRefundStatus, 0)
        );
        // 如果更新影响行数为0，说明被其他请求抢先退费
        if (rows == 0) {
            throw new BusinessException("退费失败，该记录可能已被退费");
        }

        // 创建退费记录（金额取负值，表示退款）
        Payment refund = new Payment();
        refund.setStudentId(original.getStudentId());
        refund.setType(original.getType());
        refund.setAmount(original.getAmount().negate());
        refund.setPayMethod(original.getPayMethod());
        refund.setPayTime(LocalDateTime.now());
        refund.setReceiptNo(generateReceiptNo());
        refund.setRemark("退费 - 关联单号: " + original.getReceiptNo());
        refund.setOperatorId(operatorId);
        refund.setRefundStatus(0);
        refund.setOriginalPaymentId(paymentId);
        paymentMapper.insert(refund);
    }

    /**
     * 教练课时费结算
     * <p>
     * 计算指定教练在指定日期范围内的课时费收入：
     * 1. 查询该教练在该时间段内所有已完成(status=3)的课时
     * 2. 统计总课时数
     * 3. 逐课时计算费用：实际课时 x 课程单价 x 教练分成比例(40%)
     * 4. 返回汇总金额及明细列表
     * </p>
     *
     * @param coachId   教练ID
     * @param startDate 结算开始日期
     * @param endDate   结算结束日期
     * @return 包含教练ID、日期范围、总课时数、总金额、明细列表的Map
     */
    public Map<String, Object> getCoachSettlement(Long coachId, LocalDate startDate, LocalDate endDate) {
        // 查询指定时间段内已完成的课时
        List<Lesson> lessons = lessonMapper.selectList(
                new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getCoachId, coachId)
                        .eq(Lesson::getStatus, 3)
                        .ge(Lesson::getLessonDate, startDate)
                        .le(Lesson::getLessonDate, endDate)
        );

        // 计算总实际课时
        BigDecimal totalHours = lessons.stream()
                .filter(l -> l.getActualHours() != null)
                .map(Lesson::getActualHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 批量查询课程信息（避免 N+1 查询）
        List<Long> courseIds = lessons.stream()
                .filter(l -> l.getCourseId() != null)
                .map(Lesson::getCourseId)
                .distinct()
                .toList();
        Map<Long, Course> courseMap = new HashMap<>();
        if (!courseIds.isEmpty()) {
            courseMapper.selectBatchIds(courseIds).forEach(c -> courseMap.put(c.getId(), c));
        }

        // 教练分成比例为40%
        BigDecimal coachRate = new BigDecimal("0.4");
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Map<String, Object>> details = new ArrayList<>();

        // 逐课时计算费用
        for (Lesson lesson : lessons) {
            if (lesson.getActualHours() == null) continue;
            // 从课程配置获取单价，默认120元/小时
            BigDecimal pricePerHour = new BigDecimal("120.00");
            if (lesson.getCourseId() != null) {
                Course course = courseMap.get(lesson.getCourseId());
                if (course != null && course.getPricePerHour() != null) {
                    pricePerHour = course.getPricePerHour();
                }
            }
            // 计算单次课时费 = 实际课时 x 单价 x 分成比例
            BigDecimal lessonAmount = lesson.getActualHours().multiply(pricePerHour).multiply(coachRate);
            totalAmount = totalAmount.add(lessonAmount);

            // 组装单条明细
            Map<String, Object> detail = new HashMap<>();
            detail.put("lessonId", lesson.getId());
            detail.put("lessonDate", lesson.getLessonDate());
            detail.put("actualHours", lesson.getActualHours());
            detail.put("amount", lessonAmount);
            details.add(detail);
        }

        // 组装结算结果
        Map<String, Object> result = new HashMap<>();
        result.put("coachId", coachId);
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("totalLessons", lessons.size());
        result.put("totalHours", totalHours);
        result.put("totalAmount", totalAmount);
        result.put("details", details);
        return result;
    }

    /**
     * 生成收据编号（私有方法）
     * <p>
     * 格式：RCP-yyyyMMdd-XXXX（如 RCP-20240101-0001）
     * 实现方案：
     * 1. 优先使用Redis自增序列号（分布式安全，设置2天过期）
     * 2. Redis不可用时降级为数据库查询当天已有收据数量+1
     * </p>
     *
     * @return 唯一收据编号
     */
    private String generateReceiptNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "RCP-" + dateStr + "-";
        try {
            // 优先使用Redis自增序列号，设置2天过期避免内存泄漏
            Long seq = redisUtils.increment("receipt:seq:" + dateStr);
            redisUtils.expire("receipt:seq:" + dateStr, 2, TimeUnit.DAYS);
            return prefix + String.format("%04d", seq);
        } catch (Exception e) {
            // Redis不可用时降级：查询数据库中当天已有收据数量，+1作为新序号
            // NOTE: under high concurrency this may produce duplicates.
            // A unique constraint on receipt_no + retry is recommended for production.
            long count = paymentMapper.countByReceiptNoPrefix(prefix);
            // 降级方案使用6位序号，防止高并发下4位序号不足导致编号冲突
            return prefix + String.format("%06d", count + 1);
        }
    }
}
