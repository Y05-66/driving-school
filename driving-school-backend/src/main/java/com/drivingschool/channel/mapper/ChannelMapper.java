package com.drivingschool.channel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.channel.entity.Channel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 招生渠道Mapper接口
 * 提供对channel表的基本CRUD操作
 *
 * @author drivingschool
 */
@Mapper
public interface ChannelMapper extends BaseMapper<Channel> {
}
