package com.drivingschool.vehicle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.vehicle.entity.VehicleMaintenance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 车辆维保记录Mapper接口
 * 对应实体类：{@link VehicleMaintenance}（vehicle_maintenance表）
 * 提供车辆维保记录的数据库CRUD操作，继承MyBatis-Plus的BaseMapper获得通用增删改查能力
 */
@Mapper
public interface VehicleMaintenanceMapper extends BaseMapper<VehicleMaintenance> {
}
