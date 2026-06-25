package com.drivingschool.coach.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.coach.entity.Coach;
import com.drivingschool.coach.mapper.CoachMapper;
import com.drivingschool.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

/**
 * 教练管理服务类
 * <p>
 * 负责驾校教练信息的管理，包括：
 * - 教练信息的创建、查询、更新
 * - 教练状态管理（启用/禁用）
 * - 教练列表分页查询（支持按姓名、电话、教练类型、状态筛选）
 * - 教练驾照有效期检查（是否过期）
 * </p>
 */
@Service
@RequiredArgsConstructor
public class CoachService {

    /** 教练数据访问层 */
    private final CoachMapper coachMapper;

    /**
     * 分页查询教练列表
     *
     * @param page      分页参数对象
     * @param name      教练姓名（模糊查询，可选）
     * @param phone     手机号（模糊查询，可选）
     * @param coachType 教练类型（可选，如"C1教练"、"C2教练"等）
     * @param status    教练状态（可选：1-启用 0-禁用）
     * @return 分页结果，按创建时间降序排列
     */
    public Page<Coach> page(Page<Coach> page, String name, String phone, String coachType, Integer status) {
        LambdaQueryWrapper<Coach> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(name != null, Coach::getName, name)
                .like(phone != null, Coach::getPhone, phone)
                .eq(coachType != null, Coach::getCoachType, coachType)
                .eq(status != null, Coach::getStatus, status)
                .orderByDesc(Coach::getCreateTime);
        return coachMapper.selectPage(page, wrapper);
    }

    /**
     * 根据ID获取教练详情
     *
     * @param id 教练ID
     * @return 教练实体，不存在时返回null
     */
    public Coach getById(Long id) {
        return coachMapper.selectById(id);
    }

    /**
     * 根据用户ID获取教练信息（用于当前登录用户关联查询）
     *
     * @param userId 系统用户ID
     * @return 教练实体，不存在时返回null
     */
    public Coach getByUserId(Long userId) {
        return coachMapper.selectOne(
                new LambdaQueryWrapper<Coach>().eq(Coach::getUserId, userId)
        );
    }

    /**
     * 创建教练记录
     * <p>
     * 新建教练默认状态为1（启用）
     * </p>
     *
     * @param coach 教练实体
     */
    public void create(Coach coach) {
        // 必填字段验证
        if (!StringUtils.hasText(coach.getName())) {
            throw new BusinessException("教练姓名不能为空");
        }
        if (!StringUtils.hasText(coach.getPhone())) {
            throw new BusinessException("教练手机号不能为空");
        }
        // 检查手机号是否已存在
        Long count = coachMapper.selectCount(
                new LambdaQueryWrapper<Coach>().eq(Coach::getPhone, coach.getPhone())
        );
        if (count > 0) {
            throw new BusinessException("该手机号已注册为教练");
        }
        coach.setStatus(1);
        coachMapper.insert(coach);
    }

    /**
     * 更新教练信息
     *
     * @param coach 教练实体（需包含ID）
     */
    // 使用事务保证教练信息更新的原子性，防止部分字段更新失败导致数据不一致
    @Transactional(rollbackFor = Exception.class)
    public void update(Coach coach) {
        coachMapper.updateById(coach);
    }

    /**
     * 更新教练状态（启用/禁用）
     *
     * @param id     教练ID
     * @param status 目标状态（1-启用 0-禁用）
     */
    public void updateStatus(Long id, Integer status) {
        Coach coach = new Coach();
        coach.setId(id);
        coach.setStatus(status);
        coachMapper.updateById(coach);
    }

    /**
     * 检查教练驾照是否已过期
     *
     * @param coachId 教练ID
     * @return true-已过期，false-未过期或无驾照信息
     */
    public boolean isLicenseExpired(Long coachId) {
        Coach coach = getById(coachId);
        return coach != null && coach.getLicenseExpire() != null && coach.getLicenseExpire().isBefore(LocalDate.now());
    }
}
