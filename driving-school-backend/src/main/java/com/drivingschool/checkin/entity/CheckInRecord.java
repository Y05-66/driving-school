package com.drivingschool.checkin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学时打卡记录实体类
 * 对应数据库表：check_in_record
 * 学员签到签退，自动计算学时
 */
@Data
@TableName("check_in_record")
public class CheckInRecord {

    /** 记录ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 学员ID */
    private Long studentId;

    /** 教练ID */
    private Long coachId;

    /** 课时ID（关联lesson表） */
    private Long lessonId;

    /** 科目：科目一/二/三/四 */
    private String subjectName;

    /** 签到时间 */
    private LocalDateTime checkInTime;

    /** 签退时间 */
    private LocalDateTime checkOutTime;

    /** 签到经度 */
    private BigDecimal checkInLng;

    /** 签到纬度 */
    private BigDecimal checkInLat;

    /** 签到地址（逆地理编码） */
    private String checkInAddress;

    /** 签退经度 */
    private BigDecimal checkOutLng;

    /** 签退纬度 */
    private BigDecimal checkOutLat;

    /** 实际学时（小时，签退时自动计算） */
    private BigDecimal actualHours;

    /** 状态：0-已签到 1-已签退 2-异常 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
