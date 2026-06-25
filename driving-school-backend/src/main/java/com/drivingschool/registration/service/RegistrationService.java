package com.drivingschool.registration.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.registration.dto.RegistrationSubmitDTO;
import com.drivingschool.registration.entity.Registration;
import com.drivingschool.registration.mapper.RegistrationMapper;
import com.drivingschool.student.entity.Student;
import com.drivingschool.student.mapper.StudentMapper;
import com.drivingschool.student.service.StudentProgressService;
import com.drivingschool.system.entity.SysUser;
import com.drivingschool.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 在线报名服务类
 * <p>
 * 处理学员的在线报名流程，包括：
 * - 提交报名申请（校验重复提交）
 * - 分页查询报名记录（支持状态和关键词筛选）
 * - 审核报名（通过时自动创建学员档案和登录账号）
 * - 按手机号查询报名状态
 * </p>
 *
 * @author drivingschool
 */
@Service
@RequiredArgsConstructor
public class RegistrationService extends ServiceImpl<RegistrationMapper, Registration> {

    /** 学员Mapper，用于审核通过时创建学员档案 */
    private final StudentMapper studentMapper;

    /** 用户服务，用于审核通过时创建登录账号 */
    private final SysUserService userService;

    /** 学员进度服务，用于初始化四科学习进度 */
    private final StudentProgressService studentProgressService;

    /**
     * 学员提交报名申请（通过DTO）
     * <p>
     * 将DTO转换为实体后调用submit方法。
     * 使用DTO接收客户端数据，避免客户端篡改内部字段。
     * </p>
     *
     * @param dto 报名信息DTO
     * @return 保存后的报名记录
     * @throws BusinessException 该手机号已有待审核的报名记录时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public Registration submitFromDTO(RegistrationSubmitDTO dto) {
        Registration registration = new Registration();
        registration.setName(dto.getName());
        registration.setIdCard(dto.getIdCard());
        registration.setPhone(dto.getPhone());
        registration.setGender(dto.getGender());
        registration.setBirthday(dto.getBirthday());
        registration.setAddress(dto.getAddress());
        registration.setApplyType(dto.getApplyType());
        registration.setClassType(dto.getClassType());
        registration.setIdCardFront(dto.getIdCardFront());
        registration.setIdCardBack(dto.getIdCardBack());
        registration.setRemark(dto.getRemark());
        return submit(registration);
    }

    /**
     * 学员提交报名申请
     * <p>
     * 业务规则：
     * 1. 校验该手机号是否已有待审核的报名记录，避免重复提交
     * 2. 设置初始状态为"待审核"（status=0）
     * 3. 保存报名记录
     * </p>
     *
     * @param registration 报名信息，包含姓名、身份证号、手机号、报考类型等
     * @return 保存后的报名记录
     * @throws BusinessException 该手机号已有待审核的报名记录时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public Registration submit(Registration registration) {
        Long count = count(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getPhone, registration.getPhone())
                .eq(Registration::getStatus, 0));
        if (count > 0) {
            throw new BusinessException("您已提交过报名申请，请等待审核");
        }
        registration.setStatus(0);
        save(registration);
        return registration;
    }

    /**
     * 分页查询报名记录
     * <p>
     * 支持按状态筛选和关键词搜索（匹配姓名或手机号），
     * 结果按创建时间倒序排列。
     * </p>
     *
     * @param page    分页参数
     * @param status  报名状态筛选，0-待审核 1-已通过 2-已驳回，不传则查全部
     * @param keyword 搜索关键词，匹配姓名或手机号
     * @return 分页后的报名记录
     */
    public Page<Registration> page(Page<Registration> page, Integer status, String keyword) {
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Registration::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Registration::getName, keyword)
                    .or().like(Registration::getPhone, keyword));
        }
        wrapper.orderByDesc(Registration::getCreateTime);
        return page(page, wrapper);
    }

    /**
     * 审核报名申请
     * <p>
     * 审核流程：
     * 1. 校验报名记录是否存在且状态为"待审核"
     * 2. 记录审核人、审核意见和审核时间
     * 3. 如果通过：
     *    a. 校验身份证号有效性
     *    b. 创建学员档案（Student），关联报名信息
     *    c. 创建登录账号（SysUser），用户名=手机号，密码=身份证后6位
     *    d. 建立学员档案与登录账号的关联
     * 4. 如果驳回：设置状态为"已驳回"
     * </p>
     *
     * @param id           报名记录ID
     * @param reviewerId   审核人ID
     * @param approved     是否通过，true-通过 false-驳回
     * @param reviewComment 审核意见
     * @throws BusinessException 报名记录不存在、已审核、或身份证号无效时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void review(Long id, Long reviewerId, boolean approved, String reviewComment) {
        Registration reg = getById(id);
        if (reg == null) {
            throw new BusinessException("报名记录不存在");
        }
        if (reg.getStatus() != 0) {
            throw new BusinessException("该报名已审核，不可重复操作");
        }

        reg.setReviewerId(reviewerId);
        reg.setReviewComment(reviewComment);
        reg.setReviewTime(LocalDateTime.now());

        if (approved) {
            // 创建学员档案
            Student student = new Student();
            student.setName(reg.getName());
            student.setIdCard(reg.getIdCard());
            student.setPhone(reg.getPhone());
            student.setGender(reg.getGender());
            student.setBirthday(reg.getBirthday());
            student.setAddress(reg.getAddress());
            student.setApplyType(reg.getApplyType());
            student.setStatus(1);
            student.setRegisterDate(java.time.LocalDate.now());
            studentMapper.insert(student);

            // 创建登录账号（用户名=手机号，初始密码=身份证后6位）
            // 初始密码为身份证后6位，管理员可告知学员，学员登录后应修改密码
            if (reg.getIdCard() == null || reg.getIdCard().length() < 6) {
                throw new BusinessException("身份证号无效，无法创建登录账号");
            }
            SysUser user = new SysUser();
            user.setUsername(reg.getPhone());
            String initialPassword = reg.getIdCard().substring(reg.getIdCard().length() - 6);
            user.setPassword(initialPassword);
            user.setRealName(reg.getName());
            user.setPhone(reg.getPhone());
            user.setRole("STUDENT");
            user.setStatus(1);
            userService.create(user);

            student.setUserId(user.getId());
            studentMapper.updateById(student);

            // 初始化四科学习进度
            String applyType = reg.getApplyType() != null ? reg.getApplyType() : "C1";
            studentProgressService.initProgress(student.getId(), applyType);

            reg.setStudentId(student.getId());
            reg.setUserId(user.getId());
            reg.setStatus(1);
            // 仅在响应中返回密码，不在remark中存储密码（安全修复）
            String existingRemark = reg.getRemark() != null ? reg.getRemark() + "\n" : "";
            reg.setRemark(existingRemark + "审核通过");
        } else {
            reg.setStatus(2);
        }

        updateById(reg);
    }

    /**
     * 查询指定手机号的报名状态
     * <p>
     * 返回该手机号最近一次的报名记录（按创建时间倒序取第一条）。
     * 用于学员在小程序端查看自己的报名进度。
     * </p>
     *
     * @param phone 手机号
     * @return 报名记录，如果不存在则返回null
     */
    public Registration getByPhone(String phone) {
        return getOne(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getPhone, phone)
                .orderByDesc(Registration::getCreateTime)
                .last("LIMIT 1"));
    }

    /**
     * 获取报名统计数据
     * <p>
     * 统计各状态的报名数量，用于PC端仪表盘展示。
     * 返回的Map包含：total（总数）、pending（待审核）、approved（已通过）、rejected（已驳回）。
     * </p>
     *
     * @return 统计数据Map
     */
    public java.util.Map<String, Long> getStats() {
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        stats.put("total", count());
        stats.put("pending", count(new LambdaQueryWrapper<Registration>().eq(Registration::getStatus, 0)));
        stats.put("approved", count(new LambdaQueryWrapper<Registration>().eq(Registration::getStatus, 1)));
        stats.put("rejected", count(new LambdaQueryWrapper<Registration>().eq(Registration::getStatus, 2)));
        return stats;
    }

}
