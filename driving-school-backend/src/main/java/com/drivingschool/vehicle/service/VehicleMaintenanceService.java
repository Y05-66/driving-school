package com.drivingschool.vehicle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.vehicle.entity.Vehicle;
import com.drivingschool.vehicle.entity.VehicleMaintenance;
import com.drivingschool.vehicle.mapper.VehicleMaintenanceMapper;
import com.drivingschool.vehicle.mapper.VehicleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 车辆维保管理服务类
 * <p>
 * 负责驾校车辆的维修保养记录管理，包括：
 * - 维保记录的创建与查询
 * - 按车辆查看维保历史
 * - 查询即将到期的维保项目（如保险、年检等），用于提前提醒
 * </p>
 */
@Service
@RequiredArgsConstructor
public class VehicleMaintenanceService {

    /** 车辆维保记录数据访问层 */
    private final VehicleMaintenanceMapper maintenanceMapper;
    /** 车辆数据访问层（用于校验车辆是否存在） */
    private final VehicleMapper vehicleMapper;

    /**
     * 分页查询维保记录列表
     *
     * @param page     分页参数对象
     * @param vehicleId 车辆ID（可选筛选条件）
     * @param type      维保类型（可选，如保养、维修、保险、年检等）
     * @return 分页结果，按维保日期降序排列
     */
    public Page<VehicleMaintenance> page(Page<VehicleMaintenance> page, Long vehicleId, Integer type) {
        LambdaQueryWrapper<VehicleMaintenance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(vehicleId != null, VehicleMaintenance::getVehicleId, vehicleId)
                .eq(type != null, VehicleMaintenance::getType, type)
                .orderByDesc(VehicleMaintenance::getMaintenanceDate);
        return maintenanceMapper.selectPage(page, wrapper);
    }

    /**
     * 创建维保记录
     * <p>
     * 创建前校验车辆是否存在
     * </p>
     *
     * @param maintenance 维保记录实体
     * @throws BusinessException 车辆不存在时抛出
     */
    public void create(VehicleMaintenance maintenance) {
        // 校验车辆是否存在
        Vehicle vehicle = vehicleMapper.selectById(maintenance.getVehicleId());
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }
        maintenanceMapper.insert(maintenance);
    }

    /**
     * 获取指定车辆的所有维保记录
     *
     * @param vehicleId 车辆ID
     * @return 维保记录列表，按维保日期降序排列
     */
    public List<VehicleMaintenance> getByVehicleId(Long vehicleId) {
        return maintenanceMapper.selectList(
                new LambdaQueryWrapper<VehicleMaintenance>()
                        .eq(VehicleMaintenance::getVehicleId, vehicleId)
                        .orderByDesc(VehicleMaintenance::getMaintenanceDate)
        );
    }

    /**
     * 查询即将到期的维保记录（如保险到期、年检到期等）
     * <p>
     * 查询条件：
     * 1. nextDueDate不为空
     * 2. nextDueDate在当前日期到 当前日期+daysAhead 之间
     * 结果按到期日期升序排列，方便优先处理最紧急的项目
     * </p>
     *
     * @param daysAhead 提前天数（查询未来多少天内到期的记录）
     * @return 即将到期的维保记录列表
     */
    public List<VehicleMaintenance> getExpiringSoon(int daysAhead) {
        LocalDate deadline = LocalDate.now().plusDays(daysAhead);
        return maintenanceMapper.selectList(
                new LambdaQueryWrapper<VehicleMaintenance>()
                        .isNotNull(VehicleMaintenance::getNextDueDate)
                        .le(VehicleMaintenance::getNextDueDate, deadline)
                        .ge(VehicleMaintenance::getNextDueDate, LocalDate.now())
                        .orderByAsc(VehicleMaintenance::getNextDueDate)
        );
    }
}
