package com.drivingschool.vehicle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.vehicle.entity.Vehicle;
import com.drivingschool.vehicle.mapper.VehicleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 车辆管理服务类
 * <p>
 * 负责驾校车辆信息的管理，包括：
 * - 车辆信息的创建（含车牌号唯一性校验）
 * - 车辆信息的查询与更新
 * - 车辆状态管理（启用/维修中/报废等）
 * - 获取可用车辆列表（按车型筛选）
 * </p>
 * <p>
 * 状态说明：1-可用 0-维修中/不可用
 * </p>
 */
@Service
@RequiredArgsConstructor
public class VehicleService {

    /** 车辆数据访问层 */
    private final VehicleMapper vehicleMapper;

    /**
     * 分页查询车辆列表
     *
     * @param page     分页参数对象
     * @param plateNo  车牌号（模糊查询，可选）
     * @param brand    品牌（模糊查询，可选）
     * @param carType  车型（可选，如"C1"、"C2"等）
     * @param status   车辆状态（可选：1-可用 0-不可用）
     * @return 分页结果，按创建时间降序排列
     */
    public Page<Vehicle> page(Page<Vehicle> page, String plateNo, String brand, String carType, Integer status) {
        LambdaQueryWrapper<Vehicle> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(plateNo != null, Vehicle::getPlateNo, plateNo)
                .like(brand != null, Vehicle::getBrand, brand)
                .eq(carType != null, Vehicle::getCarType, carType)
                .eq(status != null, Vehicle::getStatus, status)
                .orderByDesc(Vehicle::getCreateTime);
        return vehicleMapper.selectPage(page, wrapper);
    }

    /**
     * 根据ID获取车辆详情
     *
     * @param id 车辆ID
     * @return 车辆实体，不存在时返回null
     */
    public Vehicle getById(Long id) {
        return vehicleMapper.selectById(id);
    }

    /**
     * 创建车辆记录
     * <p>
     * 业务规则：
     * 1. 车牌号全局唯一，不能重复录入
     * 2. 新建车辆默认状态为1（可用）
     * </p>
     *
     * @param vehicle 车辆实体
     * @throws BusinessException 车牌号已存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(Vehicle vehicle) {
        // 必填字段验证
        if (!StringUtils.hasText(vehicle.getPlateNo())) {
            throw new BusinessException("车牌号不能为空");
        }
        if (!StringUtils.hasText(vehicle.getBrand())) {
            throw new BusinessException("车辆品牌不能为空");
        }
        // 校验车牌号唯一性
        Long count = vehicleMapper.selectCount(
                new LambdaQueryWrapper<Vehicle>().eq(Vehicle::getPlateNo, vehicle.getPlateNo())
        );
        if (count > 0) {
            throw new BusinessException("车牌号已存在");
        }
        vehicle.setStatus(1);
        vehicleMapper.insert(vehicle);
    }

    /**
     * 更新车辆信息
     *
     * @param vehicle 车辆实体（需包含ID）
     */
    // 使用事务保证车辆信息更新的原子性，防止部分字段更新失败导致数据不一致
    @Transactional(rollbackFor = Exception.class)
    public void update(Vehicle vehicle) {
        vehicleMapper.updateById(vehicle);
    }

    /**
     * 更新车辆状态
     *
     * @param id     车辆ID
     * @param status 目标状态
     */
    public void updateStatus(Long id, Integer status) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setStatus(status);
        vehicleMapper.updateById(vehicle);
    }

    /**
     * 获取可用车辆列表
     * <p>
     * 查询状态为可用(1)的车辆，可按车型进一步筛选
     * </p>
     *
     * @param carType 车型（可选，如"C1"、"C2"等）
     * @return 可用车辆列表
     */
    public List<Vehicle> getAvailable(String carType) {
        LambdaQueryWrapper<Vehicle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Vehicle::getStatus, 1)
                .eq(carType != null, Vehicle::getCarType, carType);
        return vehicleMapper.selectList(wrapper);
    }
}
