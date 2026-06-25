package com.drivingschool.student.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学员档案附件实体类
 * 对应数据库表：student_attachment
 * 用于存储学员档案相关的各类附件文件信息，如身份证扫描件、体检报告、照片等
 *
 * <p>附件类型说明：</p>
 * <ul>
 *   <li>1 - 身份证，学员身份证正反面扫描件或照片</li>
 *   <li>2 - 体检，学员体检报告扫描件或照片</li>
 *   <li>3 - 照片，学员证件照或报名照片</li>
 *   <li>4 - 合同，学员与驾校签订的培训合同</li>
 *   <li>5 - 其他，其他相关附件材料</li>
 * </ul>
 */
@Data
@TableName("student_attachment")
public class StudentAttachment {

    /** 附件ID，主键，使用雪花算法自动生成分布式唯一ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 学员ID，关联student表，标识该附件属于哪位学员 */
    private Long studentId;

    /** 附件类型，1-身份证 2-体检 3-照片 4-合同 5-其他 */
    private Integer type;

    /** 文件名称，上传时的原始文件名（含扩展名） */
    private String fileName;

    /** 文件路径，附件在服务器或对象存储中的访问路径 */
    private String filePath;

    /** 文件大小，附件的字节数（byte），用于前端展示和上传限制校验 */
    private Long fileSize;

    /** 备注说明，对附件的补充描述信息 */
    private String remark;

    /** 上传者ID，关联sys_user表，记录是哪位工作人员上传的 */
    private Long uploaderId;

    /** 创建时间，记录附件上传的时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除标志，0-未删除 1-已删除，实现软删除功能 */
    @TableLogic
    private Integer deleted;
}
