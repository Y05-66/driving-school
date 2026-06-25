package com.drivingschool.student.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.student.entity.StudentAttachment;
import com.drivingschool.student.mapper.StudentAttachmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * 学员档案附件服务类
 * <p>
 * 负责学员档案附件的管理，包括：
 * - 上传附件文件（身份证、体检报告、照片、合同等）
 * - 查询学员的附件列表
 * - 删除附件
 * </p>
 */
@Service
@RequiredArgsConstructor
public class AttachmentService {

    /** 学员附件数据访问层 */
    private final StudentAttachmentMapper attachmentMapper;

    /** 文件上传的基础存储路径（可通过 application.yml 的 attachment.upload-dir 配置） */
    @Value("${attachment.upload-dir:./uploads/students/}")
    private String uploadBasePath;

    /**
     * 上传学员附件
     * <p>
     * 将上传的文件保存到服务器，并在数据库中创建附件记录。
     * 业务规则：
     * 1. 学员ID不能为空
     * 2. 附件类型必须在合法范围内（1-5）
     * 3. 文件不能为空
     * 4. 文件名使用UUID重命名以避免冲突，保留原始扩展名
     * 5. 创建附件记录并返回
     * </p>
     *
     * @param studentId  学员ID
     * @param type       附件类型，1-身份证 2-体检 3-照片 4-合同 5-其他
     * @param file       上传的文件
     * @param remark     备注说明（可选）
     * @param uploaderId 上传者ID
     * @return 创建成功的附件记录
     * @throws BusinessException 参数校验失败或文件保存失败时抛出
     */
    public StudentAttachment upload(Long studentId, Integer type, MultipartFile file,
                                    String remark, Long uploaderId) {
        // 参数校验
        if (studentId == null) {
            throw new BusinessException("学员ID不能为空");
        }
        if (type == null || type < 1 || type > 5) {
            throw new BusinessException("附件类型不合法，仅支持1-5");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        // 获取原始文件名和扩展名
        String originalFileName = file.getOriginalFilename();
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        // 生成唯一的文件名（UUID + 原始扩展名）
        String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;

        // 确保上传目录存在
        String datePath = java.time.LocalDate.now().toString().replace("-", "/");
        String uploadDir = uploadBasePath + datePath;
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 保存文件到服务器
        String filePath = uploadDir + "/" + newFileName;
        try {
            file.transferTo(new File(filePath));
        } catch (IOException e) {
            throw new BusinessException("文件上传失败：" + e.getMessage());
        }

        // 创建附件记录并保存到数据库
        StudentAttachment attachment = new StudentAttachment();
        attachment.setStudentId(studentId);
        attachment.setType(type);
        attachment.setFileName(originalFileName);
        attachment.setFilePath(filePath);
        attachment.setFileSize(file.getSize());
        attachment.setRemark(remark);
        attachment.setUploaderId(uploaderId);
        attachmentMapper.insert(attachment);

        return attachment;
    }

    /**
     * 获取学员的附件列表
     * <p>
     * 查询指定学员的所有未删除附件，按创建时间倒序排列。
     * </p>
     *
     * @param studentId 学员ID
     * @return 该学员的所有附件列表
     */
    public List<StudentAttachment> getByStudentId(Long studentId) {
        LambdaQueryWrapper<StudentAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentAttachment::getStudentId, studentId)
                .orderByDesc(StudentAttachment::getCreateTime);
        return attachmentMapper.selectList(wrapper);
    }

    /**
     * 删除附件
     * <p>
     * 逻辑删除指定的附件记录（将deleted标记设为1）。
     * 业务规则：
     * 1. 附件ID不能为空
     * 2. 附件必须存在且未被删除
     * </p>
     *
     * @param attachmentId 附件ID
     * @throws BusinessException 附件不存在时抛出
     */
    public void delete(Long attachmentId) {
        if (attachmentId == null) {
            throw new BusinessException("附件ID不能为空");
        }

        // 查询附件是否存在
        StudentAttachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException("附件不存在");
        }

        // 逻辑删除（MyBatis-Plus的@TableLogic注解会自动处理为UPDATE操作）
        attachmentMapper.deleteById(attachmentId);
    }
}
