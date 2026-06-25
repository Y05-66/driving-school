package com.drivingschool.coach.controller;

import com.drivingschool.common.result.R;
import com.drivingschool.coach.entity.CoachPerformance;
import com.drivingschool.coach.service.CoachPerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教练绩效控制器
 * 提供绩效计算、查询和排名的RESTful接口
 *
 * @author drivingschool
 */
@Tag(name = "教练绩效")
@RestController
@RequestMapping("/coaches/performance")
@RequiredArgsConstructor
public class CoachPerformanceController {

    /** 绩效服务，处理绩效计算和排名的核心业务逻辑 */
    private final CoachPerformanceService performanceService;

    /**
     * 计算指定月份的教练绩效
     * 触发从lesson、exam、coach_evaluation表汇总数据并计算综合得分
     *
     * @param month 统计月份，格式：YYYY-MM
     * @return 计算完成的绩效记录数
     */
    @Operation(summary = "计算月度绩效")
    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<Integer> calculate(@RequestParam String month) {
        int count = performanceService.calculate(month);
        return R.ok(count);
    }

    /**
     * 查询教练的绩效记录
     * 根据教练ID查询其所有月份的绩效数据
     *
     * @param id 教练ID
     * @return 该教练的所有绩效记录列表
     */
    @Operation(summary = "查询教练绩效记录")
    @GetMapping("/coach/{id}")
    public R<List<CoachPerformance>> getByCoachId(@PathVariable Long id) {
        List<CoachPerformance> list = performanceService.getByCoachId(id);
        return R.ok(list);
    }

    /**
     * 获取指定月份的教练绩效排名
     * 按综合得分从高到低排列
     *
     * @param month 统计月份，格式：YYYY-MM
     * @return 按得分排序的绩效记录列表
     */
    @Operation(summary = "教练绩效排名")
    @GetMapping("/ranking")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<List<CoachPerformance>> getRanking(@RequestParam String month) {
        List<CoachPerformance> ranking = performanceService.getRanking(month);
        return R.ok(ranking);
    }
}
