package com.drivingschool.vehicle.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.common.result.PageResult;
import com.drivingschool.common.result.R;
import com.drivingschool.vehicle.dto.VehicleQueryDTO;
import com.drivingschool.vehicle.entity.Vehicle;
import com.drivingschool.vehicle.entity.VehicleMaintenance;
import com.drivingschool.vehicle.service.VehicleMaintenanceService;
import com.drivingschool.vehicle.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 车辆管理控制器
 * <p>
 * 功能说明：管理驾校训练车辆的全生命周期，包括车辆信息的增删改查、
 * 车辆状态管理、可用车辆查询、维修保养记录管理以及保险/年检到期提醒等功能。
 * </p>
 * 基础路径：/vehicles
 * <p>
 * 接口权限说明：
 * - 车辆列表和详情：登录即可访问
 * - 可用车辆查询：登录即可访问
 * - 车辆CRUD操作和维修记录管理：需要ADMIN角色
 * - 到期提醒：需要ADMIN或STAFF角色
 * </p>
 *
 * @author drivingschool
 */
@Tag(name = "车辆管理")
@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    /** 车辆服务，处理车辆信息的CRUD操作 */
    private final VehicleService vehicleService;

    /** 车辆维修保养服务，处理车辆的维修保养记录管理 */
    private final VehicleMaintenanceService vehicleMaintenanceService;

    /**
     * 车辆列表查询接口
     * <p>
     * 分页查询车辆列表，支持按车牌号、品牌、车型、状态进行筛选。
     * </p>
     *
     * @param query 查询条件DTO，包含车牌号、品牌、车型、状态及分页参数
     * @return 分页后的车辆列表（PageResult格式）
     * @summary 查询车辆列表
     * @权限要求 无需特殊权限（登录即可访问）
     */
    @Operation(summary = "车辆列表")
    @GetMapping
    public R<PageResult<Vehicle>> list(VehicleQueryDTO query) {
        // 构建分页对象
        Page<Vehicle> page = new Page<>(query.getPageNum(), query.getPageSize());
        // 执行分页查询
        Page<Vehicle> result = vehicleService.page(page, query.getPlateNo(), query.getBrand(),
                query.getCarType(), query.getStatus());
        return R.ok(PageResult.of(result));
    }

    /**
     * 车辆详情接口
     * <p>
     * 根据车辆ID查询车辆的详细信息。
     * </p>
     *
     * @param id 车辆ID（路径参数）
     * @return 车辆的详细信息
     * @summary 查询车辆详情
     * @权限要求 无需特殊权限（登录即可访问）
     */
    @Operation(summary = "车辆详情")
    @GetMapping("/{id}")
    public R<Vehicle> detail(@PathVariable Long id) {
        Vehicle vehicle = vehicleService.getById(id);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }
        return R.ok(vehicle);
    }

    /**
     * 添加车辆接口
     * <p>
     * 录入新的训练车辆信息，包括车牌号、品牌、车型等。
     * </p>
     *
     * @param vehicle 车辆实体对象，包含车辆的基本信息
     * @return 操作成功返回R.ok()
     * @summary 添加车辆
     * @权限要求 仅管理员（ADMIN）
     */
    @Operation(summary = "添加车辆")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> create(@RequestBody Vehicle vehicle) {
        vehicleService.create(vehicle);
        return R.ok();
    }

    /**
     * 修改车辆信息接口
     * <p>
     * 根据车辆ID修改车辆的基本信息。
     * </p>
     *
     * @param id      车辆ID（路径参数）
     * @param vehicle 车辆实体对象，包含需要更新的字段
     * @return 操作成功返回R.ok()
     * @summary 修改车辆信息
     * @权限要求 仅管理员（ADMIN）
     */
    @Operation(summary = "修改车辆信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> update(@PathVariable Long id, @RequestBody Vehicle vehicle) {
        // 白名单方式仅更新允许的字段，防止批量赋值攻击（mass assignment）
        Vehicle existing = vehicleService.getById(id);
        if (existing == null) {
            throw new BusinessException("车辆不存在");
        }
        existing.setPlateNo(vehicle.getPlateNo());
        existing.setBrand(vehicle.getBrand());
        existing.setModel(vehicle.getModel());
        existing.setCarType(vehicle.getCarType());
        existing.setColor(vehicle.getColor());
        existing.setBuyDate(vehicle.getBuyDate());
        existing.setInsuranceExpire(vehicle.getInsuranceExpire());
        existing.setInspectionExpire(vehicle.getInspectionExpire());
        existing.setMileage(vehicle.getMileage());
        vehicleService.update(existing);
        return R.ok();
    }

    /**
     * 变更车辆状态接口
     * <p>
     * 变更车辆的状态（如：正常、维修中、报废等）。
     * </p>
     *
     * @param id     车辆ID（路径参数）
     * @param status 目标状态值
     * @return 操作成功返回R.ok()
     * @summary 变更车辆状态
     * @权限要求 仅管理员（ADMIN）
     */
    @Operation(summary = "变更车辆状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        // 校验状态值合法性：仅允许0（不可用）或1（可用）
        if (status != 0 && status != 1) {
            return R.fail(400, "状态值只能为0或1");
        }
        vehicleService.updateStatus(id, status);
        return R.ok();
    }

    /**
     * 可用车辆查询接口
     * <p>
     * 查询当前可用的训练车辆列表，可按车型筛选。
     * 用于预约课时时选择可用车辆。
     * </p>
     *
     * @param carType 车型筛选条件（可选）
     * @return 可用车辆列表
     * @summary 查询可用车辆
     * @权限要求 无需特殊权限（登录即可访问）
     */
    @Operation(summary = "可用车辆查询")
    @GetMapping("/available")
    public R<List<Vehicle>> available(@RequestParam(required = false) String carType) {
        return R.ok(vehicleService.getAvailable(carType));
    }

    /**
     * 添加维修保养记录接口
     * <p>
     * 为指定车辆添加维修保养记录。
     * </p>
     *
     * @param vehicleId   车辆ID（路径参数）
     * @param maintenance 维修保养记录实体对象
     * @return 操作成功返回R.ok()
     * @summary 添加车辆维修保养记录
     * @权限要求 仅管理员（ADMIN）
     */
    @Operation(summary = "添加维修保养记录")
    @PostMapping("/{vehicleId}/maintenance")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> addMaintenance(@PathVariable Long vehicleId, @RequestBody VehicleMaintenance maintenance) {
        maintenance.setVehicleId(vehicleId);
        vehicleMaintenanceService.create(maintenance);
        return R.ok();
    }

    /**
     * 车辆维修保养记录列表接口
     * <p>
     * 查询指定车辆的所有维修保养记录。
     * </p>
     *
     * @param vehicleId 车辆ID（路径参数）
     * @return 车辆的维修保养记录列表
     * @summary 查询车辆维修保养记录
     * @权限要求 无需特殊权限（登录即可访问）
     */
    @Operation(summary = "车辆维修保养记录")
    @GetMapping("/{vehicleId}/maintenance")
    public R<List<VehicleMaintenance>> maintenanceList(@PathVariable Long vehicleId) {
        return R.ok(vehicleMaintenanceService.getByVehicleId(vehicleId));
    }

    /**
     * 保险/年检即将到期提醒接口
     * <p>
     * 查询在指定天数内即将到期的保险或年检记录。
     * 默认查询30天内到期的记录，用于提前提醒管理人员处理。
     * </p>
     *
     * @param daysAhead 提前提醒天数，默认30天
     * @return 即将到期的维修保养（保险/年检）记录列表
     * @summary 查询保险/年检即将到期提醒
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "即将到期提醒（保险/年检）")
    @GetMapping("/maintenance/expiring")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<List<VehicleMaintenance>> expiringSoon(
            @RequestParam(defaultValue = "30") int daysAhead) {
        return R.ok(vehicleMaintenanceService.getExpiringSoon(daysAhead));
    }
}
