package com.drivingschool.student.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.student.dto.StudentCreateDTO;
import com.drivingschool.student.entity.Student;
import com.drivingschool.student.mapper.StudentMapper;
import com.drivingschool.system.entity.SysUser;
import com.drivingschool.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;

/**
 * 学员管理服务类
 * <p>
 * 负责驾校学员信息的全生命周期管理，包括：
 * - 学员信息的创建（含账号创建、年龄校验、身份证唯一性校验）
 * - 学员信息的查询与分页列表
 * - 学员状态管理（待审核、在学、已毕业、已退学）
 * - 学员个人信息修改（仅允许修改电话和地址）
 * - 新学员注册时自动初始化四科学习进度
 * </p>
 */
@Service
@RequiredArgsConstructor
public class StudentService {

    /** 学员数据访问层 */
    private final StudentMapper studentMapper;
    /** 系统用户服务（用于创建学员登录账号） */
    private final SysUserService sysUserService;
    /** 学员进度服务（用于初始化新学员的四科学习进度） */
    private final StudentProgressService studentProgressService;

    /**
     * 分页查询学员列表
     *
     * @param page      分页参数对象
     * @param name      学员姓名（模糊查询，可选）
     * @param idCard    身份证号（模糊查询，可选）
     * @param phone     手机号（模糊查询，可选）
     * @param status    学员状态（可选：0-待审核 1-在学 2-已毕业 3-已退学）
     * @param applyType 报考车型类型（可选，如"C1"、"C2"等）
     * @return 分页结果，按创建时间降序排列
     */
    public Page<Student> page(Page<Student> page, String name, String idCard, String phone,
                              Integer status, String applyType) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(name != null, Student::getName, name)
                .like(idCard != null, Student::getIdCard, idCard)
                .like(phone != null, Student::getPhone, phone)
                .eq(status != null, Student::getStatus, status)
                .eq(applyType != null, Student::getApplyType, applyType)
                .orderByDesc(Student::getCreateTime);
        Page<Student> studentPage = studentMapper.selectPage(page, wrapper);
        studentPage.setSize(page.getSize());
        studentPage.setCurrent(page.getCurrent());
        return studentPage;
    }

    /**
     * 根据ID获取学员详情
     *
     * @param id 学员ID
     * @return 学员实体，不存在时返回null
     */
    public Student getById(Long id) {
        return studentMapper.selectById(id);
    }

    /**
     * 根据用户ID获取学员信息（用于当前登录用户关联查询）
     *
     * @param userId 系统用户ID
     * @return 学员实体，不存在时返回null
     */
    public Student getByUserId(Long userId) {
        return studentMapper.selectOne(
                new LambdaQueryWrapper<Student>().eq(Student::getUserId, userId)
        );
    }

    /**
     * 创建新学员
     * <p>
     * 业务规则：
     * 1. 身份证号全局唯一，不能重复注册
     * 2. 年龄必须在18-70岁之间
     * 3. 自动创建系统登录账号（角色为STUDENT）
     * 4. 学员记录自动关联登录账号
     * 5. 新学员状态默认为0（待审核），注册日期为当天
     * 6. 自动初始化四科学习进度（根据报考类型，如C1、C2等）
     * </p>
     *
     * @param dto 学员创建数据传输对象（包含个人信息和登录账号信息）
     * @throws BusinessException 身份证号已存在或年龄不合法时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"overview", "dashboard", "studentStats"}, allEntries = true)
    public void create(StudentCreateDTO dto) {
        // 校验身份证号唯一性
        if (dto.getIdCard() != null) {
            Long count = studentMapper.selectCount(
                    new LambdaQueryWrapper<Student>().eq(Student::getIdCard, dto.getIdCard())
            );
            if (count > 0) {
                throw new BusinessException(1001, "身份证号已存在");
            }
        }

        // 校验年龄范围（18-70岁）
        if (dto.getBirthday() != null) {
            int age = Period.between(dto.getBirthday(), LocalDate.now()).getYears();
            if (age < 18 || age > 70) {
                throw new BusinessException("年龄必须在18-70岁之间");
            }
        }

        // 创建登录账号（角色设为STUDENT）
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setRealName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setRole("STUDENT");
        sysUserService.create(user);

        // 创建学员记录并关联登录账号
        Student student = new Student();
        student.setName(dto.getName());
        student.setIdCard(dto.getIdCard());
        student.setPhone(dto.getPhone());
        student.setGender(dto.getGender());
        student.setBirthday(dto.getBirthday());
        student.setAddress(dto.getAddress());
        student.setApplyType(dto.getApplyType());
        student.setRemark(dto.getRemark());
        student.setStatus(0);
        student.setRegisterDate(LocalDate.now());
        student.setUserId(user.getId());
        studentMapper.insert(student);

        // 初始化四科学习进度（根据报考类型，默认C1）
        String applyType = dto.getApplyType() != null ? dto.getApplyType() : "C1";
        studentProgressService.initProgress(student.getId(), applyType);
    }

    /**
     * 更新学员信息
     *
     * @param student 学员实体（需包含ID）
     */
    public void update(Student student) {
        studentMapper.updateById(student);
    }

    /**
     * 更新学员状态
     * <p>
     * 状态变更规则：
     * - 状态值：0-待审核 1-在学 2-已毕业 3-已退学
     * - 当变更为退学状态(3)时，自动在备注中追加退学时间
     * </p>
     *
     * @param id     学员ID
     * @param status 目标状态值
     * @throws BusinessException 学员不存在时抛出
     */
    public void updateStatus(Long id, Integer status) {
        Student student = getById(id);
        if (student == null) {
            throw new BusinessException("学员不存在");
        }

        // 退学时自动追加退学时间到备注
        if (status == 3 && student.getStatus() != 3) {
            student.setStatus(status);
            student.setRemark((student.getRemark() != null ? student.getRemark() + "\n" : "") + "退学时间: " + LocalDate.now());
        } else {
            student.setStatus(status);
        }
        studentMapper.updateById(student);
    }

    /**
     * 学员修改个人信息（仅允许修改电话和地址）
     * <p>
     * 安全说明：通过userId查找当前登录用户对应的学员记录，
     * 仅更新允许修改的字段，防止越权修改其他敏感信息
     * </p>
     *
     * @param userId 当前登录用户的ID
     * @param update 包含新电话和地址的学员实体
     * @throws BusinessException 学员信息不存在时抛出
     */
    public void updateMyInfo(Long userId, Student update) {
        Student student = getByUserId(userId);
        if (student == null) {
            throw new BusinessException("学员信息不存在");
        }
        // 只更新允许修改的字段（电话和地址）
        Student entity = new Student();
        entity.setId(student.getId());
        entity.setPhone(update.getPhone());
        entity.setAddress(update.getAddress());
        studentMapper.updateById(entity);
    }

    /**
     * 获取学员统计数据
     * <p>
     * 统计各状态的学员数量，用于PC端仪表盘展示。
     * </p>
     *
     * @return 统计数据Map：total、pending、studying、graduated、withdrawn
     */
    public java.util.Map<String, Long> getStats() {
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        stats.put("total", studentMapper.selectCount(null));
        stats.put("pending", studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getStatus, 0)));
        stats.put("studying", studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getStatus, 1)));
        stats.put("graduated", studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getStatus, 2)));
        stats.put("withdrawn", studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getStatus, 3)));
        return stats;
    }
}
