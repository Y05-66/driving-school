package com.drivingschool.student.controller;

import com.drivingschool.common.result.R;
import com.drivingschool.student.entity.StudentAttachment;
import com.drivingschool.student.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 学员档案附件控制器
 * <p>
 * 功能说明：管理学员档案的各类附件文件，包括身份证扫描件、体检报告、
 * 证件照、培训合同等的上传、查看和删除。
 * </p>
 * 基础路径：/students/{id}/attachments
 * <p>
 * 接口权限说明：
 * - 上传和删除附件：需要管理员（ADMIN）或前台（STAFF）角色
 * - 查看附件列表：需要管理员（ADMIN）、前台（STAFF）或教练（COACH）角色
 * </p>
 *
 * @author drivingschool
 */
@Tag(name = "学员档案附件")
@RestController
@RequestMapping("/students/{id}/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    /** 学员附件服务，处理附件上传、查询和删除的业务逻辑 */
    private final AttachmentService attachmentService;

    /**
     * 上传学员附件接口
     * <p>
     * 为指定学员上传档案附件文件。支持上传身份证、体检报告、照片、合同等类型的附件。
     * 文件会保存到服务器的指定目录，并在数据库中创建附件记录。
     * </p>
     *
     * @param id             学员ID（路径参数）
     * @param type           附件类型，1-身份证 2-体检 3-照片 4-合同 5-其他（表单参数）
     * @param file           上传的文件（multipart/form-data格式）
     * @param remark         备注说明（可选的表单参数）
     * @param authentication Spring Security认证对象，用于获取当前操作者的用户ID
     * @return 创建成功的附件记录
     * @summary 上传学员附件
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "上传附件")
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<StudentAttachment> upload(@PathVariable("id") Long id,
                                        @RequestParam Integer type,
                                        @RequestParam MultipartFile file,
                                        @RequestParam(required = false) String remark,
                                        Authentication authentication) {
        Long uploaderId = (Long) authentication.getPrincipal();
        return R.ok(attachmentService.upload(id, type, file, remark, uploaderId));
    }

    /**
     * 获取学员附件列表接口
     * <p>
     * 查询指定学员的所有附件列表，按创建时间倒序排列。
     * </p>
     *
     * @param id 学员ID（路径参数）
     * @return 该学员的所有附件列表
     * @summary 查询学员附件列表
     * @权限要求 管理员（ADMIN）、前台（STAFF）、教练（COACH）
     */
    @Operation(summary = "学员附件列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','COACH')")
    public R<List<StudentAttachment>> list(@PathVariable("id") Long id) {
        return R.ok(attachmentService.getByStudentId(id));
    }

    /**
     * 删除学员附件接口
     * <p>
     * 逻辑删除指定的附件记录。删除后附件将不再出现在列表中，但数据库记录仍然保留。
     * </p>
     *
     * @param id           学员ID（路径参数）
     * @param attachmentId 附件ID（路径参数）
     * @return 操作成功返回R.ok()
     * @summary 删除学员附件
     * @权限要求 管理员（ADMIN）、前台（STAFF）
     */
    @Operation(summary = "删除附件")
    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<Void> delete(@PathVariable("id") Long id,
                           @PathVariable Long attachmentId) {
        attachmentService.delete(attachmentId);
        return R.ok();
    }
}
