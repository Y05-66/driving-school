package com.drivingschool.registration.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.common.result.PageResult;
import com.drivingschool.common.result.R;
import com.drivingschool.registration.dto.RegistrationStatusVO;
import com.drivingschool.registration.dto.RegistrationSubmitDTO;
import com.drivingschool.registration.entity.Registration;
import com.drivingschool.registration.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 在线报名控制器
 * <p>
 * 功能说明：管理学员的在线报名流程，包括小程序端的报名提交和状态查询，
 * 以及PC端的报名列表查看和审核操作。审核通过后自动创建学员档案和登录账号。
 * </p>
 * 基础路径：/registrations
 * <p>
 * 接口权限说明：
 * - 提交报名（POST /registrations）：无需登录，小程序端公开接口
 * - 查询报名状态（GET /registrations/status）：无需登录，通过手机号查询
 * - 报名列表（GET /registrations）：需要ADMIN或STAFF角色
 * - 审核报名（PUT /registrations/{id}/review）：需要ADMIN或STAFF角色
 * </p>
 *
 * @author drivingschool
 */
@Tag(name = "在线报名")
@RestController
@RequestMapping("/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    /** 报名服务，处理报名的提交、查询和审核逻辑 */
    private final RegistrationService registrationService;

    /**
     * 审核请求对象
     */
    @Data
    public static class ReviewRequest {
        /** 是否通过，true-通过 false-驳回 */
        private boolean approved;
        /** 审核意见，驳回时建议填写驳回原因 */
        private String comment;
    }

    /**
     * 学员提交报名（小程序端，无需登录）
     * <p>
     * 学员填写个人信息和报考类型后提交报名申请，
     * 系统自动校验手机号是否已存在报名记录，避免重复提交。
     * 使用专用DTO接收请求，防止客户端篡改内部字段。
     * </p>
     *
     * @param dto 报名信息DTO，包含姓名、身份证号、手机号、报考类型等
     * @return 提交成功的报名记录
     * @summary 提交报名
     */
    @Operation(summary = "提交报名")
    @PostMapping
    public R<RegistrationStatusVO> submit(@Valid @RequestBody RegistrationSubmitDTO dto) {
        Registration reg = registrationService.submitFromDTO(dto);
        return R.ok(toStatusVO(reg));
    }

    /**
     * 查询报名状态（小程序端，无需登录）
     * <p>
     * 通过手机号查询报名记录，返回报名状态和审核结果。
     * 使用专用VO返回数据，仅包含客户端需要的字段。
     * </p>
     *
     * @param phone 报名时填写的手机号
     * @return 报名状态VO，包含当前状态和审核意见
     * @throws BusinessException 未找到报名记录时抛出
     * @summary 查询报名状态
     */
    @Operation(summary = "查询报名状态")
    @GetMapping("/status")
    public R<RegistrationStatusVO> getStatus(@RequestParam String phone) {
        Registration reg = registrationService.getByPhone(phone);
        if (reg == null) {
            // 返回统一结构，避免泄露手机号是否已注册
            RegistrationStatusVO empty = new RegistrationStatusVO();
            empty.setStatus(-1); // -1表示未找到报名记录
            return R.ok(empty);
        }
        return R.ok(toStatusVO(reg));
    }

    /**
     * 将Registration实体转换为RegistrationStatusVO
     */
    private RegistrationStatusVO toStatusVO(Registration reg) {
        RegistrationStatusVO vo = new RegistrationStatusVO();
        vo.setId(reg.getId());
        vo.setName(reg.getName());
        vo.setPhone(reg.getPhone());
        vo.setApplyType(reg.getApplyType());
        vo.setStatus(reg.getStatus());
        vo.setReviewComment(reg.getReviewComment());
        vo.setReviewTime(reg.getReviewTime());
        vo.setCreateTime(reg.getCreateTime());
        return vo;
    }

    /**
     * 报名列表（PC端，管理员/前台）
     * <p>
     * 分页查询报名记录，支持按状态和关键词筛选。
     * 关键词匹配姓名、手机号、身份证号。
     * </p>
     *
     * @param pageNum  页码，默认1
     * @param pageSize 每页条数，默认10
     * @param status   报名状态筛选，0-待审核 1-已通过 2-已驳回，不传则查全部
     * @param keyword  搜索关键词，匹配姓名/手机号/身份证号
     * @return 分页后的报名记录列表
     * @summary 报名列表
     * @权限要求 ADMIN、STAFF
     */
    @Operation(summary = "报名列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<PageResult<Registration>> list(
            @RequestParam(defaultValue = "1") @jakarta.validation.constraints.Min(1) Integer pageNum,
            @RequestParam(defaultValue = "10") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        Page<Registration> page = new Page<>(pageNum, pageSize);
        Page<Registration> result = registrationService.page(page, status, keyword);
        return R.ok(PageResult.of(result));
    }

    /**
     * 获取报名统计数据（PC端）
     * <p>
     * 统计各状态的报名数量，用于PC端仪表盘展示。
     * 返回：total（总数）、pending（待审核）、approved（已通过）、rejected（已驳回）。
     * </p>
     *
     * @return 统计数据Map
     * @summary 报名统计
     * @权限要求 ADMIN、STAFF
     */
    @Operation(summary = "报名统计")
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<java.util.Map<String, Long>> stats() {
        return R.ok(registrationService.getStats());
    }

    /**
     * 审核报名（PC端，管理员/前台）
     * <p>
     * 工作人员对报名申请进行审核：
     * - 通过：自动创建学员档案（Student）和登录账号（SysUser），关联到报名记录
     * - 驳回：记录驳回原因，报名者可修改后重新提交
     * </p>
     *
     * @param id             报名记录ID
     * @param authentication 当前登录用户信息，用于获取审核人ID
     * @param request        审核请求，包含是否通过和审核意见
     * @return 操作结果
     * @summary 审核报名
     * @权限要求 ADMIN、STAFF
     */
    @Operation(summary = "审核报名")
    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<Void> review(@PathVariable Long id, Authentication authentication,
                          @RequestBody ReviewRequest request) {
        Long reviewerId = (Long) authentication.getPrincipal();
        registrationService.review(id, reviewerId, request.isApproved(), request.getComment());
        return R.ok();
    }
}
