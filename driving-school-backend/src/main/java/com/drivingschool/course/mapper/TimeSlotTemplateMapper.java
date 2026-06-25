package com.drivingschool.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.course.entity.TimeSlotTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 时间段模板Mapper接口
 * 对应实体类：{@link TimeSlotTemplate}（time_slot_template表）
 * 提供课程时间段模板的数据库CRUD操作，继承MyBatis-Plus的BaseMapper获得通用增删改查能力
 */
@Mapper
public interface TimeSlotTemplateMapper extends BaseMapper<TimeSlotTemplate> {
}
