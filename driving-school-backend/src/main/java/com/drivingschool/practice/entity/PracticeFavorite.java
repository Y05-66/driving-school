package com.drivingschool.practice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目收藏实体类
 * 对应数据库表：practice_favorite
 */
@Data
@TableName("practice_favorite")
public class PracticeFavorite {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 学员用户ID */
    private Long userId;

    /** 题目ID */
    private Long questionId;

    /** 收藏时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
