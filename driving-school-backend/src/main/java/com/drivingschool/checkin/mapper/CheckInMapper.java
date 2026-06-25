package com.drivingschool.checkin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.checkin.entity.CheckIn;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学时签到Mapper接口
 * <p>
 * 提供签到表的基础数据库操作，继承MyBatis-Plus的BaseMapper，
 * 包含增删改查、分页查询等常用方法。
 * </p>
 *
 * @author drivingschool
 * @since 1.0.0
 */
@Mapper
public interface CheckInMapper extends BaseMapper<CheckIn> {
}
