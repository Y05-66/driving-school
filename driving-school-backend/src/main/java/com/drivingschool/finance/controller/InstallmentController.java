package com.drivingschool.finance.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.common.result.PageResult;
import com.drivingschool.common.result.R;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.finance.entity.InstallmentDetail;
import com.drivingschool.finance.entity.InstallmentPlan;
import com.drivingschool.finance.service.InstallmentService;
import com.drivingschool.student.entity.Student;
import com.drivingschool.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 分期付款控制器
 * 提供分期计划和明细的RESTful接口
 *
 * @author drivingschool
 */
@Tag(name = "分期付款")
@RestController
@RequestMapping("/finance/installments")
@RequiredArgsConstructor
public class InstallmentController {

    private final InstallmentService installmentService;
    private final StudentService studentService;

    /**
     * 获取分期统计数据
     * <p>
     * 统计各状态的分期计划数量和本月收款金额，用于PC端仪表盘展示。
     * </p>
     *
     * @return 统计数据Map：inProgress（进行中）、completed（已结清）、overdue（逾期）、monthAmount（本月收款）
     * @summary 分期统计
     */
    @Operation(summary = "分期统计")
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public R<Map<String, Object>> stats() {
        return R.ok(installmentService.getStats());
    }

    /**
     * 分页查询所有分期计划
     *
     * @param pageNum  页码，默认1
     * @param pageSize 每页条数，默认10
     * @param status   状态筛选（可选）
     * @return 分页结果
     */
    @GetMapping("/plan")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public R<PageResult<InstallmentPlan>> listPlans(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        Page<InstallmentPlan> page = new Page<>(pageNum, pageSize);
        Page<InstallmentPlan> result = installmentService.page(page, status);
        return R.ok(PageResult.of(result));
    }

    /**
     * 创建分期计划
     * 根据学员ID、合同ID、总金额和期数创建分期计划，自动生成每期明细
     *
     * @param plan 分期计划信息（studentId, contractId, totalAmount, installments）
     * @return 创建成功的分期计划
     */
    @PostMapping("/plan")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public R<InstallmentPlan> createPlan(@RequestBody InstallmentPlan plan) {
        InstallmentPlan created = installmentService.createPlan(plan);
        return R.ok(created);
    }

    /**
     * 查询学员的分期计划
     * 根据学员ID查询其所有分期计划
     *
     * @param id 学员ID
     * @return 该学员的所有分期计划列表
     */
    @GetMapping("/plan/student/{id}")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','STUDENT')")
    public R<List<InstallmentPlan>> getPlanByStudentId(@PathVariable Long id, Authentication authentication) {
        // 学员只能查看自己的分期计划（IDOR 防护）
        Long userId = (Long) authentication.getPrincipal();
        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        if (isStudent) {
            Student student = studentService.getByUserId(userId);
            if (student == null || !student.getId().equals(id)) {
                throw new BusinessException("无权访问其他学员的分期信息");
            }
        }
        List<InstallmentPlan> plans = installmentService.getPlanByStudentId(id);
        return R.ok(plans);
    }

    /**
     * 查询分期计划明细
     * 根据分期计划ID查询所有期数明细
     *
     * @param planId 分期计划ID
     * @return 该计划的所有分期明细列表
     */
    @GetMapping("/detail/{planId}")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','STUDENT')")
    public R<List<InstallmentDetail>> getDetailsByPlanId(@PathVariable Long planId) {
        List<InstallmentDetail> details = installmentService.getDetailsByPlanId(planId);
        return R.ok(details);
    }

    /**
     * 支付某期分期款
     * 根据明细ID完成支付，更新状态并记录支付信息
     *
     * @param id 分期明细ID
     * @param params 请求参数，包含paymentId（支付记录ID）
     * @return 更新后的分期明细
     */
    @PostMapping("/detail/{id}/pay")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public R<InstallmentDetail> payInstallment(@PathVariable Long id, @RequestBody Map<String, Long> params) {
        Long paymentId = params.get("paymentId");
        InstallmentDetail detail = installmentService.payInstallment(id, paymentId);
        return R.ok(detail);
    }
}
