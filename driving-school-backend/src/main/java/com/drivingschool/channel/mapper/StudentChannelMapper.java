package com.drivingschool.channel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.channel.entity.StudentChannel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学员渠道关联Mapper接口
 * 提供对student_channel表的基本CRUD操作
 *
 * @author drivingschool
 */
@Mapper
public interface StudentChannelMapper extends BaseMapper<StudentChannel> {
}
