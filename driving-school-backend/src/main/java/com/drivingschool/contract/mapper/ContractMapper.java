package com.drivingschool.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.contract.entity.Contract;
import org.apache.ibatis.annotations.Mapper;

/**
 * 电子合同Mapper接口
 * <p>
 * 提供合同表的基础数据库操作，继承MyBatis-Plus的BaseMapper，
 * 包含增删改查、分页查询等常用方法。
 * </p>
 *
 * @author drivingschool
 * @since 1.0.0
 */
@Mapper
public interface ContractMapper extends BaseMapper<Contract> {
}
