package com.drivingschool.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.finance.entity.InstallmentPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分期付款计划Mapper接口
 * 提供对installment_plan表的基本CRUD操作
 *
 * @author drivingschool
 */
@Mapper
public interface InstallmentPlanMapper extends BaseMapper<InstallmentPlan> {
}
