package com.drivingschool.contract.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.common.result.PageResult;
import com.drivingschool.common.result.R;
import com.drivingschool.contract.entity.Contract;
import com.drivingschool.contract.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 电子合同控制器
 * <p>
 * 提供合同相关的RESTful API接口，包括合同的创建、查询、签署等操作。
 * </p>
 *
 * @author drivingschool
 * @since 1.0.0
 */
@Tag(name = "电子合同")
@RestController
@RequestMapping("/contracts")
@RequiredArgsConstructor
public class ContractController {

    /** 合同服务，处理合同创建、签署和查询的核心业务逻辑 */
    private final ContractService contractService;

    /**
     * 创建合同
     * <p>
     * 创建新的电子合同，系统自动生成合同编号。
     * </p>
     *
     * @param contract 合同信息
     * @return 创建成功的合同信息
     */
    @Operation(summary = "创建合同")
    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public R<Contract> create(@RequestBody Contract contract) {
        Contract created = contractService.create(contract);
        return R.ok(created);
    }

    /**
     * 根据ID获取合同详情
     *
     * @param id 合同ID
     * @return 合同详情
     */
    @Operation(summary = "获取合同详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','STUDENT')")
    public R<Contract> getById(@PathVariable Long id) {
        Contract contract = contractService.getById(id);
        return R.ok(contract);
    }

    /**
     * 根据学员ID获取合同列表
     * <p>查询指定学员的所有合同，按创建时间倒序排列</p>
     *
     * @param studentId 学员ID
     * @return 合同列表
     */
    @Operation(summary = "查询学员合同")
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','STUDENT')")
    public R<java.util.List<Contract>> getByStudentId(@PathVariable Long studentId) {
        java.util.List<Contract> list = contractService.getByStudentId(studentId);
        return R.ok(list);
    }

    /**
     * 签署数据请求对象
     */
    @Data
    public static class SignRequest {
        /** 签署数据 */
        private String signData;
    }

    /**
     * 签署合同
     * <p>
     * 学员签署指定合同，需要提供签署数据。
     * 只有状态为"待签署"的合同才能被签署。
     * </p>
     *
     * @param id       合同ID
     * @param signData 签署数据（请求体）
     * @return 签署后的合同信息
     */
    @Operation(summary = "签署合同")
    @PutMapping("/{id}/sign")
    @PreAuthorize("hasAnyRole('STUDENT','STAFF','ADMIN')")
    public R<Contract> sign(@PathVariable Long id, @RequestBody SignRequest request, Authentication authentication) {
        Contract contract = contractService.sign(id, request.getSignData(), authentication);
        return R.ok(contract);
    }

    /**
     * 分页查询合同
     * <p>
     * 支持按学员ID、合同状态、关键词进行筛选。
     * </p>
     *
     * @param page      当前页码（默认1）
     * @param size      每页大小（默认10）
     * @param studentId 学员ID（可选）
     * @param status    合同状态（可选，0待签/1已签/2作废）
     * @param keyword   搜索关键词（可选）
     * @return 分页结果
     */
    @Operation(summary = "分页查询合同")
    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','STUDENT')")
    public R<PageResult<Contract>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        Page<Contract> result = contractService.page(page, size, studentId, status, keyword);
        return R.ok(PageResult.of(result));
    }
}
