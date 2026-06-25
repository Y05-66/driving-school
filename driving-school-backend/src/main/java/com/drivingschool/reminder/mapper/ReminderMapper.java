package com.drivingschool.reminder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.reminder.entity.Reminder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自动提醒Mapper接口
 * <p>
 * 提供提醒表的基础数据库操作，继承MyBatis-Plus的BaseMapper，
 * 包含增删改查、分页查询等常用方法。
 * </p>
 *
 * @author drivingschool
 * @since 1.0.0
 */
@Mapper
public interface ReminderMapper extends BaseMapper<Reminder> {
}
