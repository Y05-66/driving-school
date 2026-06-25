package com.drivingschool.video.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教学视频实体类
 * 对应数据库表：video
 * <p>
 * 用于存储驾校教学视频资源，支持按科目和分类筛选。
 * 学员可在小程序端观看视频学习驾驶理论和实操技巧。
 * </p>
 *
 * @author drivingschool
 */
@Data
@TableName("video")
public class Video {

    /** 视频ID，主键，使用雪花算法自动生成分布式唯一ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 视频标题 */
    private String title;

    /** 视频描述 */
    private String description;

    /** 科目：科目一/二/三/四 */
    private String subject;

    /** 分类：倒车入库/侧方停车/坡道起步/曲线行驶/直角转弯 */
    private String category;

    /** 视频URL */
    private String videoUrl;

    /** 封面图URL */
    private String coverUrl;

    /** 视频时长（秒） */
    private Integer duration;

    /** 播放次数 */
    private Integer viewCount;

    /** 状态：0-下架 1-上架 */
    private Integer status;

    /** 排序号 */
    private Integer sortOrder;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标志，0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
