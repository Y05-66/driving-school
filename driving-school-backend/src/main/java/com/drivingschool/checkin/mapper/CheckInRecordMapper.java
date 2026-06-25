package com.drivingschool.checkin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.checkin.entity.CheckInRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CheckInRecordMapper extends BaseMapper<CheckInRecord> {
}
