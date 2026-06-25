package com.drivingschool.vehicle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.vehicle.entity.Vehicle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 教练车Mapper接口
 * 对应实体类：{@link Vehicle}（vehicle表）
 * 提供教练车的数据库CRUD操作，继承MyBatis-Plus的BaseMapper获得通用增删改查能力
 * 同时包含车辆状态统计相关的自定义查询方法
 */
@Mapper
public interface VehicleMapper extends BaseMapper<Vehicle> {

    /**
     * 按车辆状态统计数量
     * 查询指定状态下的车辆总数（排除已逻辑删除的记录）
     * 用于首页仪表盘展示车辆状态分布
     *
     * @param status 车辆状态值（如：0-停用 1-正常 2-维修中等）
     * @return 该状态下的车辆数量
     */
    @Select("SELECT COUNT(*) FROM vehicle WHERE status = #{status} AND deleted = 0")
    long countByStatus(@Param("status") int status);
}
