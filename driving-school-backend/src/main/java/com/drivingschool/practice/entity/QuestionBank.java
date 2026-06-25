package com.drivingschool.practice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题库实体类
 * 对应数据库表：question_bank
 * 用于存储驾考理论题库中的所有题目信息，包括科目一和科目四的各类题型
 *
 * <p>题型说明：</p>
 * <ul>
 *   <li>1 - 单选题</li>
 *   <li>2 - 多选题</li>
 *   <li>3 - 判断题</li>
 * </ul>
 *
 * <p>科目说明：</p>
 * <ul>
 *   <li>1 - 科目一（道路交通安全法律、法规和相关知识）</li>
 *   <li>4 - 科目四（安全文明驾驶常识）</li>
 * </ul>
 *
 * <p>难度等级：</p>
 * <ul>
 *   <li>1 - 简单</li>
 *   <li>2 - 中等</li>
 *   <li>3 - 困难</li>
 * </ul>
 */
@Data
@TableName("question_bank")
public class QuestionBank {

    /** 题目ID，主键，使用雪花算法自动生成分布式唯一ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属科目，1-科目一 4-科目四 */
    private Integer subject;

    /** 题目类型，1-单选题 2-多选题 3-判断题 */
    private Integer questionType;

    /** 题目内容，题干描述文本 */
    private String content;

    /**
     * 选项列表，JSON格式存储
     * <p>示例格式：[{"label":"A","content":"选项A内容"},{"label":"B","content":"选项B内容"}]</p>
     * <p>判断题可省略选项或使用固定选项（正确/错误）</p>
     */
    private String options;

    /** 正确答案，单选为单个字母（如"A"），多选为多个字母（如"ABD"），判断为"A"（正确）或"B"（错误） */
    private String answer;

    /** 答案解析，对正确答案的详细解释说明，帮助学员理解知识点 */
    private String explanation;

    /** 难度等级，1-简单 2-中等 3-困难 */
    private Integer difficulty;

    /** 创建时间，记录题目的创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除标志，0-未删除 1-已删除，实现软删除功能 */
    @TableLogic
    private Integer deleted;
}
