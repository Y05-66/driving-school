package com.drivingschool.registration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.registration.entity.Registration;
import org.apache.ibatis.annotations.Mapper;

/**
 * 在线报名数据访问层
 */
@Mapper
public interface RegistrationMapper extends BaseMapper<Registration> {
}
