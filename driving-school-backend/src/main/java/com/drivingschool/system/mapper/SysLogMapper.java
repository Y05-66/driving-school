package com.drivingschool.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.system.entity.SysLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统操作日志Mapper接口
 * 对应实体类：{@link SysLog}（sys_log表）
 * 提供系统操作日志的数据库CRUD操作，继承MyBatis-Plus的BaseMapper获得通用增删改查能力
 */
@Mapper
public interface SysLogMapper extends BaseMapper<SysLog> {
}
