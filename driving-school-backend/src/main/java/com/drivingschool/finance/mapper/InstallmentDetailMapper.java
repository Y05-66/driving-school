package com.drivingschool.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.finance.entity.InstallmentDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分期付款明细Mapper接口
 * 提供对installment_detail表的基本CRUD操作
 *
 * @author drivingschool
 */
@Mapper
public interface InstallmentDetailMapper extends BaseMapper<InstallmentDetail> {
}
