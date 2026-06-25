package com.drivingschool.video.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.video.entity.Video;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VideoMapper extends BaseMapper<Video> {
}
