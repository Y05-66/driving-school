package com.drivingschool.checkin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 学时签到实体类
 * <p>
 * 用于记录学员的课程签到签退信息，支持扫码签到和手动签到两种方式。
 * 记录签到地点、签到签退时间等关键信息，用于学时统计和管理。
 * </p>
 *
 * @author drivingschool
 * @since 1.0.0
 */
@Data
@TableName("check_in")
public class CheckIn {

    /**
     * 签到记录主键ID（雪花算法生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 课程/课时ID
     * <p>关联课程表，标识本次签到对应的课程</p>
     */
    private Long lessonId;

    /**
     * 学员ID
     * <p>关联学员表，标识签到的学员</p>
     */
    private Long studentId;

    /**
     * 教练ID
     * <p>关联教练表，标识本次课程的教练</p>
     */
    private Long coachId;

    /**
     * 签到时间
     * <p>学员签到的时间，系统自动记录</p>
     */
    private LocalDateTime checkInTime;

    /**
     * 签退时间
     * <p>学员签退的时间，签到时为空</p>
     */
    private LocalDateTime checkOutTime;

    /**
     * 签到地点
     * <p>签到时的地理位置信息，如GPS坐标或地址描述</p>
     */
    private String checkInLocation;

    /**
     * 签到方式
     * <ul>
     *   <li>1 - 扫码签到：通过扫描二维码进行签到</li>
     *   <li>2 - 手动签到：由教练或管理员手动录入</li>
     * </ul>
     */
    private Integer checkInType;

    /**
     * 签到状态
     * <ul>
     *   <li>0 - 待签到：已创建记录但尚未签到</li>
     *   <li>1 - 已签到：学员已完成签到</li>
     *   <li>2 - 已签退：学员已完成签退</li>
     * </ul>
     */
    private Integer status;

    /**
     * 创建时间
     * <p>记录插入时自动填充</p>
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     * <p>记录插入或更新时自动填充</p>
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标志
     * <ul>
     *   <li>0 - 未删除</li>
     *   <li>1 - 已删除</li>
     * </ul>
     */
    @TableLogic
    private Integer deleted;
}
