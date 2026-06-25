package com.drivingschool.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户Mapper接口
 * 对应实体类：{@link SysUser}（sys_user表）
 * 提供系统用户的数据库CRUD操作，继承MyBatis-Plus的BaseMapper获得通用增删改查能力
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
