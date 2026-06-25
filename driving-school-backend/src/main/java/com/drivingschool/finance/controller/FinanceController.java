package com.drivingschool.finance.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.common.result.PageResult;
import com.drivingschool.common.result.R;
import com.drivingschool.finance.entity.Payment;
import com.drivingschool.finance.service.PaymentService;
import com.drivingschool.student.entity.Student;
import com.drivingschool.student.service.StudentService;
import com.drivingschool.system.entity.SysUser;
import com.drivingschool.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 财务管理控制器
 * <p>
 * 功能说明：管理驾校的财务收费业务，包括收费记录的查询与创建、学员缴费记录查看、
 * 财务汇总统计、日报生成、退费处理以及教练课时费结算等功能。
 * </p>
 * 基础路径：/finance
 * <p>
 * 接口权限说明：
 * - 收费列表、新增收费、学员缴费记录、汇总统计、日报：需要ADMIN或STAFF角色
 * - 我的缴费记录（学员端）：需要STUDENT角色
 * - 退费操作：仅管理员（ADMIN）
 * - 教练课时费结算：仅管理员（ADMIN）
 * </p>
 *
 * @author drivingschool
 */
@Tag(name = "财务管理")
@RestController
@RequestMapping("/finance")
@RequiredArgsConstructor
public class FinanceController {

    /** 支付服务，处理收费记录的CRUD和财务统计 */
    private final PaymentService paymentService;

    /** 学员服务，用于查询学员信息 */
    private final StudentService studentService;

    /** 用户服务，用于查询系统用户信息 */
    private final SysUserService sysUserService;

    /**
     * 收费列表查询接口（管理员/前台）
     * <p>
     * 分页查询收费记录列表，支持按学员ID、收费类型、支付方式、时间范围筛选。
     * </p>
     *
     * @param studentId 学员ID筛选（可选）
     * @param type      收费类型筛选（可选，如报名费、补考费等）
     * @param payMethod 支付方式筛选（可选，如现金、微信、支付宝等）
     * @param startTime 查询开始时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param endTime   查询结束时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param pageNum   页码，默认第1页
     * @param pageSize  每页条数，默认10条
     * @return 分页后的收费记录列表（PageResult格式）
     * @summary 查询收费列表
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "收费列表（管理员/前台）")
    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<PageResult<Payment>> list(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer payMethod,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        // 构建分页对象
        Page<Payment> page = new Page<>(pageNum, pageSize);
        // 执行分页查询
        Page<Payment> result = paymentService.page(page, studentId, type, payMethod, startTime, endTime);
        return R.ok(PageResult.of(result));
    }

    /**
     * 新增收费接口
     * <p>
     * 创建新的收费记录，自动记录当前操作员ID。
     * </p>
     *
     * @param payment        支付实体对象，包含收费信息（学员ID、金额、类型、支付方式等）
     * @param authentication Spring Security认证对象，用于获取当前操作员ID
     * @return 操作成功返回R.ok()
     * @summary 新增收费记录
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "新增收费")
    @PostMapping("/payments")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    // @Valid 触发 Payment 实体上的 Jakarta Validation 注解校验，参数不合法时自动返回400
    public R<Void> create(@Valid @RequestBody Payment payment, Authentication authentication) {
        // 设置操作员ID为当前登录用户
        payment.setOperatorId((Long) authentication.getPrincipal());
        paymentService.create(payment);
        return R.ok();
    }

    /**
     * 我的缴费记录接口（学员端）
     * <p>
     * 学员查看自己的所有缴费记录。
     * </p>
     *
     * @param authentication Spring Security认证对象，包含当前用户身份信息
     * @return 当前学员的缴费记录列表，未查到学员信息时返回空列表
     * @summary 查看我的缴费记录
     * @权限要求 学员角色（STUDENT）
     */
    @Operation(summary = "我的缴费记录（学员）")
    @GetMapping("/payments/my")
    @PreAuthorize("hasRole('STUDENT')")
    public R<List<Payment>> myPayments(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Student student = studentService.getByUserId(userId);
        if (student == null) {
            return R.ok(List.of());
        }
        return R.ok(paymentService.getByStudentId(student.getId()));
    }

    /**
     * 学员缴费记录查询接口（管理员/前台）
     * <p>
     * 管理员或前台工作人员查询指定学员的缴费记录。
     * </p>
     *
     * @param id 学员ID（路径参数）
     * @return 指定学员的缴费记录列表
     * @summary 查询学员缴费记录
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "学员缴费记录（管理员/前台）")
    @GetMapping("/payments/student/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<List<Payment>> studentPayments(@PathVariable Long id) {
        return R.ok(paymentService.getByStudentId(id));
    }

    /**
     * 财务汇总统计接口
     * <p>
     * 获取财务汇总统计数据，包括总收入、各类型收费汇总等。
     * </p>
     *
     * @return 财务汇总统计数据Map
     * @summary 查询财务汇总统计
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "汇总统计")
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<Map<String, Object>> summary() {
        return R.ok(paymentService.getSummary());
    }

    /**
     * 财务日报接口
     * <p>
     * 查询指定日期的财务日报数据，包括当天的所有收费记录。
     * </p>
     *
     * @param date 查询日期（格式：yyyy-MM-dd）
     * @return 指定日期的收费记录列表
     * @summary 查询财务日报
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "日报")
    @GetMapping("/daily")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<List<Payment>> daily(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return R.ok(paymentService.getDailyReport(date));
    }

    /**
     * 退费接口
     * <p>
     * 对指定收费记录执行退费操作，仅管理员可执行。
     * </p>
     *
     * @param id             收费记录ID（路径参数）
     * @param authentication Spring Security认证对象，用于获取操作员ID
     * @return 操作成功返回R.ok()
     * @summary 执行退费操作
     * @权限要求 仅管理员（ADMIN）
     */
    @Operation(summary = "退费（仅管理员）")
    @PostMapping("/payments/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> refund(@PathVariable Long id, Authentication authentication) {
        Long operatorId = (Long) authentication.getPrincipal();
        paymentService.refund(id, operatorId);
        return R.ok();
    }

    /**
     * 教练课时费结算接口
     * <p>
     * 查询指定教练在指定时间段内的课时费结算数据，包括课时数和应结算金额。
     * </p>
     *
     * @param coachId   教练ID（请求参数）
     * @param startDate 结算开始日期（格式：yyyy-MM-dd）
     * @param endDate   结算结束日期（格式：yyyy-MM-dd）
     * @return 教练课时费结算数据Map，包含课时数、单价、总金额等信息
     * @summary 查询教练课时费结算
     * @权限要求 仅管理员（ADMIN）
     */
    @Operation(summary = "教练课时费结算")
    @GetMapping("/coach-settlement")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Map<String, Object>> coachSettlement(
            @RequestParam Long coachId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return R.ok(paymentService.getCoachSettlement(coachId, startDate, endDate));
    }
}
