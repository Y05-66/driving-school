package com.drivingschool.channel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学员渠道关联实体类
 * 记录学员与招生渠道的绑定关系，用于统计各渠道的招生效果
 *
 * @author drivingschool
 */
@Data
@TableName("student_channel")
public class StudentChannel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 学员ID
     */
    @TableField("student_id")
    private Long studentId;

    /**
     * 渠道ID
     */
    @TableField("channel_id")
    private Long channelId;

    /**
     * 绑定时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除标志：0-未删除，1-已删除
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
