package com.drivingschool.statistics.controller;

import com.drivingschool.common.result.R;
import com.drivingschool.common.utils.ExcelUtils;
import com.drivingschool.statistics.service.StatisticsService;
import com.drivingschool.system.entity.SysUser;
import com.drivingschool.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 数据统计控制器
 * <p>
 * 功能说明：提供驾校运营的各项数据统计功能，包括数据概览、收入趋势、报名趋势、
 * 考试通过率、教练排行、车辆使用率、角色仪表盘以及数据导出（学员列表、财务报表）等功能。
 * </p>
 * 基础路径：/statistics
 * <p>
 * 接口权限说明：
 * - 概览、收入趋势、报名趋势、教练排行、车辆使用率：需要ADMIN或STAFF角色
 * - 通过率：需要ADMIN、STAFF或COACH角色
 * - 角色仪表盘：根据当前用户角色返回对应的仪表盘数据
 * - 数据导出：仅管理员（ADMIN）
 * </p>
 *
 * @author drivingschool
 */
@Tag(name = "数据统计")
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    /** 统计服务，处理各项数据统计的核心业务逻辑 */
    private final StatisticsService statisticsService;

    /** 用户服务，用于查询当前用户信息以确定角色 */
    private final SysUserService sysUserService;

    /**
     * 数据概览接口
     * <p>
     * 获取驾校运营的核心数据概览，包括学员总数、教练总数、车辆总数、
     * 本月收入、本月报名等关键指标。
     * </p>
     *
     * @return 包含各项核心指标的Map
     * @summary 查询数据概览
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "概览（管理员/前台）")
    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<Map<String, Object>> overview() {
        return R.ok(statisticsService.getOverview());
    }

    /**
     * 收入趋势接口
     * <p>
     * 获取按时间维度（日/周/月）统计的收入趋势数据，用于图表展示。
     * </p>
     *
     * @param period 时间维度，可选值：day（按日）、week（按周）、month（按月），默认month
     * @return 收入趋势数据列表，每个元素包含时间和对应收入
     * @summary 查询收入趋势
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "收入趋势（管理员/前台）")
    @GetMapping("/revenue")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<List<Map<String, Object>>> revenue(@RequestParam(defaultValue = "month") String period) {
        return R.ok(statisticsService.getRevenueTrend(period));
    }

    /**
     * 报名趋势接口
     * <p>
     * 获取学员报名数量的趋势数据，用于图表展示报名情况变化。
     * </p>
     *
     * @return 报名趋势数据列表，每个元素包含时间和对应报名人数
     * @summary 查询报名趋势
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "报名趋势（管理员/前台）")
    @GetMapping("/enrollment")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<List<Map<String, Object>>> enrollment() {
        return R.ok(statisticsService.getEnrollmentTrend());
    }

    /**
     * 考试通过率接口
     * <p>
     * 获取考试通过率统计数据，支持按科目分组或按其他维度分组。
     * </p>
     *
     * @param groupBy 分组维度，默认按科目（subject）分组
     * @return 通过率统计数据Map
     * @summary 查询考试通过率
     * @权限要求 管理员（ADMIN）、前台（STAFF）、教练（COACH）
     */
    @Operation(summary = "通过率")
    @GetMapping("/pass-rate")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','COACH')")
    public R<Map<String, Object>> passRate(@RequestParam(defaultValue = "subject") String groupBy) {
        return R.ok(statisticsService.getPassRate(groupBy));
    }

    /**
     * 教练排行接口
     * <p>
     * 获取教练综合排行数据，包括学员通过率、学员数量、评价评分等维度。
     * </p>
     *
     * @return 教练排行数据列表
     * @summary 查询教练排行
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "教练排行")
    @GetMapping("/coach-ranking")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<List<Map<String, Object>>> coachRanking() {
        return R.ok(statisticsService.getCoachRanking());
    }

    /**
     * 车辆使用率接口
     * <p>
     * 获取车辆使用率统计数据，包括各车辆的使用频次和整体使用率。
     * </p>
     *
     * @return 车辆使用率统计数据Map
     * @summary 查询车辆使用率
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "车辆使用率（管理员/前台）")
    @GetMapping("/vehicle-usage")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<Map<String, Object>> vehicleUsage() {
        return R.ok(statisticsService.getVehicleUsage());
    }

    /**
     * 角色仪表盘接口
     * <p>
     * 根据当前登录用户的角色返回对应的仪表盘数据。
     * 不同角色看到的数据内容不同：
     * - 管理员：全局运营数据
     * - 前台：前台工作相关的数据
     * - 教练：个人教学相关的数据
     * - 学员：个人学习进度相关的数据
     * </p>
     *
     * @param authentication Spring Security认证对象，包含当前用户身份信息
     * @return 对应角色的仪表盘数据Map
     * @summary 获取角色仪表盘数据
     * @权限要求 需要登录（根据角色返回不同数据）
     */
    @Operation(summary = "角色仪表盘")
    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        SysUser user = sysUserService.getById(userId);
        String role = user != null ? user.getRole() : "STUDENT";
        return R.ok(statisticsService.getDashboard(role, userId));
    }

    /**
     * 导出学员列表接口
     * <p>
     * 将符合条件的学员数据导出为Excel文件。
     * 支持按姓名、状态、报考类型筛选导出范围。
     * </p>
     *
     * @param name      学员姓名筛选（可选）
     * @param status    学员状态筛选（可选）
     * @param applyType 报考类型筛选（可选）
     * @param response  HTTP响应对象，用于设置Excel文件的响应头和输出流
     * @summary 导出学员列表为Excel
     * @权限要求 仅管理员（ADMIN）
     */
    @Operation(summary = "导出学员列表（仅管理员）")
    @GetMapping("/export/students")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportStudents(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String applyType,
            HttpServletResponse response) {
        // 获取导出数据
        List<Map<String, Object>> data = statisticsService.getStudentExportData(name, status, applyType);
        // 使用Excel工具类导出
        ExcelUtils.export(response, "学员列表", data, "学员列表");
    }

    /**
     * 导出财务报表接口
     * <p>
     * 将指定日期范围内的财务数据导出为Excel文件。
     * </p>
     *
     * @param startDate 报表开始日期（可选，格式：yyyy-MM-dd）
     * @param endDate   报表结束日期（可选，格式：yyyy-MM-dd）
     * @param response  HTTP响应对象，用于设置Excel文件的响应头和输出流
     * @summary 导出财务报表为Excel
     * @权限要求 仅管理员（ADMIN）
     */
    @Operation(summary = "导出财务报表（仅管理员）")
    @GetMapping("/export/finance")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportFinance(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletResponse response) {
        // 获取导出数据
        List<Map<String, Object>> data = statisticsService.getFinanceExportData(startDate, endDate);
        // 使用Excel工具类导出
        ExcelUtils.export(response, "财务报表", data, "财务报表");
    }
}
