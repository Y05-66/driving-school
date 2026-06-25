package com.drivingschool.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.finance.entity.InstallmentDetail;
import com.drivingschool.finance.entity.InstallmentPlan;
import com.drivingschool.finance.mapper.InstallmentDetailMapper;
import com.drivingschool.finance.mapper.InstallmentPlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分期付款服务类
 * 提供分期计划的创建、查询、支付和逾期检查等功能
 *
 * @author drivingschool
 */
@Service
@RequiredArgsConstructor
public class InstallmentService extends ServiceImpl<InstallmentPlanMapper, InstallmentPlan> {

    /** 分期计划Mapper，用于查询和更新分期计划 */
    private final InstallmentPlanMapper planMapper;
    /** 分期明细Mapper，用于查询和更新每期明细 */
    private final InstallmentDetailMapper detailMapper;

    /**
     * 创建分期计划
     * 根据总金额和期数自动生成每期明细，每期金额平均分配，最后一期补齐尾差
     *
     * @param plan 分期计划信息（需包含studentId, contractId, totalAmount, installments）
     * @return 创建成功的分期计划
     */
    @Transactional(rollbackFor = Exception.class)
    public InstallmentPlan createPlan(InstallmentPlan plan) {
        // 输入验证
        if (plan.getStudentId() == null) {
            throw new BusinessException("学员ID不能为空");
        }
        if (plan.getContractId() == null) {
            throw new BusinessException("合同ID不能为空");
        }
        if (plan.getTotalAmount() == null || plan.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("总金额必须大于0");
        }
        if (plan.getInstallments() == null || plan.getInstallments() <= 0) {
            throw new BusinessException("分期期数必须大于0");
        }

        // 设置初始状态
        plan.setPaidAmount(BigDecimal.ZERO);
        plan.setPaidInstallments(0);
        plan.setStatus(0); // 进行中

        // 保存分期计划
        planMapper.insert(plan);

        // 计算每期金额（平均分配）
        BigDecimal totalAmount = plan.getTotalAmount();
        int installments = plan.getInstallments();
        BigDecimal perAmount = totalAmount.divide(BigDecimal.valueOf(installments), 2, RoundingMode.DOWN);
        BigDecimal lastAmount = totalAmount.subtract(perAmount.multiply(BigDecimal.valueOf(installments - 1)));

        // 生成每期明细
        LocalDate today = LocalDate.now();
        for (int i = 1; i <= installments; i++) {
            InstallmentDetail detail = new InstallmentDetail();
            detail.setPlanId(plan.getId());
            detail.setPeriod(i);
            // 最后一期使用补齐金额，确保总金额一致
            detail.setAmount(i == installments ? lastAmount : perAmount);
            // 每月1号为还款日
            detail.setDueDate(today.plusMonths(i - 1).withDayOfMonth(1));
            detail.setStatus(0); // 待还
            detailMapper.insert(detail);
        }

        return plan;
    }

    /**
     * 根据学员ID查询分期计划
     *
     * @param studentId 学员ID
     * @return 该学员的所有分期计划列表
     */
    public List<InstallmentPlan> getPlanByStudentId(Long studentId) {
        LambdaQueryWrapper<InstallmentPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InstallmentPlan::getStudentId, studentId)
               .orderByDesc(InstallmentPlan::getCreateTime);
        return planMapper.selectList(wrapper);
    }

    /**
     * 分页查询分期计划
     *
     * @param page   分页参数
     * @param status 状态筛选（可选）
     * @return 分页结果
     */
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<InstallmentPlan> page(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<InstallmentPlan> page, Integer status) {
        LambdaQueryWrapper<InstallmentPlan> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(InstallmentPlan::getStatus, status);
        }
        wrapper.orderByDesc(InstallmentPlan::getCreateTime);
        return planMapper.selectPage(page, wrapper);
    }

    /**
     * 根据分期计划ID查询所有明细
     *
     * @param planId 分期计划ID
     * @return 该计划的所有分期明细列表（按期数排序）
     */
    public List<InstallmentDetail> getDetailsByPlanId(Long planId) {
        LambdaQueryWrapper<InstallmentDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InstallmentDetail::getPlanId, planId)
               .orderByAsc(InstallmentDetail::getPeriod);
        return detailMapper.selectList(wrapper);
    }

    /**
     * 支付某期分期款
     * 更新明细状态为已还，更新计划的已付金额和已付期数，如果全部付清则更新计划状态为已结清
     *
     * @param detailId 分期明细ID
     * @param paymentId 支付记录ID
     * @return 更新后的分期明细
     */
    @Transactional(rollbackFor = Exception.class)
    public InstallmentDetail payInstallment(Long detailId, Long paymentId) {
        // 使用条件更新防止并发重复支付（乐观锁）
        int rows = detailMapper.update(null,
                new LambdaUpdateWrapper<InstallmentDetail>()
                        .eq(InstallmentDetail::getId, detailId)
                        .eq(InstallmentDetail::getStatus, 0)  // 只有未支付的才能更新
                        .set(InstallmentDetail::getStatus, 1)
                        .set(InstallmentDetail::getPayTime, LocalDateTime.now())
                        .set(InstallmentDetail::getPaymentId, paymentId)
        );
        if (rows == 0) {
            // 可能是明细不存在或已支付
            InstallmentDetail detail = detailMapper.selectById(detailId);
            if (detail == null) {
                throw new BusinessException("分期明细不存在");
            }
            throw new BusinessException("该期已支付，无需重复支付");
        }

        // 查询更新后的明细
        InstallmentDetail detail = detailMapper.selectById(detailId);

        // 使用条件更新更新分期计划（防止并发丢失更新）
        // 先尝试更新已付金额和已付期数
        InstallmentPlan plan = planMapper.selectById(detail.getPlanId());
        if (plan == null) {
            throw new BusinessException("分期计划不存在");
        }

        // 使用条件更新确保并发安全：只有当 paidInstallments 等于当前值时才更新
        int planRows;
        int maxRetries = 3;
        do {
            plan = planMapper.selectById(detail.getPlanId());
            int newPaidInstallments = plan.getPaidInstallments() + 1;
            BigDecimal newPaidAmount = plan.getPaidAmount().add(detail.getAmount());
            Integer newStatus = (newPaidInstallments == plan.getInstallments()) ? 1 : plan.getStatus();

            planRows = planMapper.update(null,
                    new LambdaUpdateWrapper<InstallmentPlan>()
                            .eq(InstallmentPlan::getId, plan.getId())
                            .eq(InstallmentPlan::getPaidInstallments, plan.getPaidInstallments())
                            .set(InstallmentPlan::getPaidAmount, newPaidAmount)
                            .set(InstallmentPlan::getPaidInstallments, newPaidInstallments)
                            .set(InstallmentPlan::getStatus, newStatus)
            );
            maxRetries--;
        } while (planRows == 0 && maxRetries > 0);

        if (planRows == 0) {
            throw new BusinessException("更新分期计划失败，请重试");
        }

        return detail;
    }

    /**
     * 检查逾期情况
     * 遍历所有进行中的分期计划，将超过还款日未支付的明细标记为逾期
     * 如果存在逾期明细，同时将计划状态更新为逾期
     *
     * @return 逾期的分期明细数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int checkOverdue() {
        int overdueCount = 0;
        LocalDate today = LocalDate.now();

        // 查询所有进行中的分期计划
        LambdaQueryWrapper<InstallmentPlan> planWrapper = new LambdaQueryWrapper<>();
        planWrapper.eq(InstallmentPlan::getStatus, 0); // 进行中
        List<InstallmentPlan> plans = planMapper.selectList(planWrapper);

        for (InstallmentPlan plan : plans) {
            // 查询该计划下所有待还且已过还款日的明细
            LambdaQueryWrapper<InstallmentDetail> detailWrapper = new LambdaQueryWrapper<>();
            detailWrapper.eq(InstallmentDetail::getPlanId, plan.getId())
                        .eq(InstallmentDetail::getStatus, 0) // 待还
                        .lt(InstallmentDetail::getDueDate, today); // 已过还款日
            List<InstallmentDetail> overdueDetails = detailMapper.selectList(detailWrapper);

            if (!overdueDetails.isEmpty()) {
                // 更新逾期明细状态
                for (InstallmentDetail detail : overdueDetails) {
                    detail.setStatus(2); // 逾期
                    detailMapper.updateById(detail);
                }
                overdueCount += overdueDetails.size();

                // 更新计划状态为逾期
                plan.setStatus(2); // 逾期
                planMapper.updateById(plan);
            }
        }

        return overdueCount;
    }

    /**
     * 获取分期统计数据
     * <p>
     * 统计各状态的分期计划数量和本月收款金额。
     * </p>
     *
     * @return 统计数据Map：inProgress、completed、overdue、monthAmount
     */
    public java.util.Map<String, Object> getStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("inProgress", planMapper.selectCount(
                new LambdaQueryWrapper<InstallmentPlan>().eq(InstallmentPlan::getStatus, 0)));
        stats.put("completed", planMapper.selectCount(
                new LambdaQueryWrapper<InstallmentPlan>().eq(InstallmentPlan::getStatus, 1)));
        stats.put("overdue", planMapper.selectCount(
                new LambdaQueryWrapper<InstallmentPlan>().eq(InstallmentPlan::getStatus, 2)));

        // 本月收款金额
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        java.time.LocalDateTime monthEnd = today.plusDays(1).atStartOfDay();
        java.math.BigDecimal monthAmount = detailMapper.selectList(
                new LambdaQueryWrapper<InstallmentDetail>()
                        .eq(InstallmentDetail::getStatus, 1)
                        .ge(InstallmentDetail::getPayTime, monthStart)
                        .lt(InstallmentDetail::getPayTime, monthEnd)
        ).stream().map(InstallmentDetail::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        stats.put("monthAmount", monthAmount);

        return stats;
    }
}
