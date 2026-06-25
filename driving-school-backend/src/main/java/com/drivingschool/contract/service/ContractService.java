package com.drivingschool.contract.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.common.utils.RedisUtils;
import com.drivingschool.contract.entity.Contract;
import com.drivingschool.contract.mapper.ContractMapper;
import com.drivingschool.student.entity.Student;
import com.drivingschool.student.mapper.StudentMapper;
import com.drivingschool.system.entity.SysUser;
import com.drivingschool.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * 电子合同服务类
 * <p>
 * 提供合同的核心业务逻辑，包括合同创建、查询、签署、分页等功能。
 * 合同编号自动生成，格式为：HT + 日期(yyyyMMdd) + 4位序号。
 * </p>
 *
 * @author drivingschool
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ContractService extends ServiceImpl<ContractMapper, Contract> {

    /** Redis工具类，用于生成合同编号的原子序号 */
    private final RedisUtils redisUtils;
    /** 学员Mapper，用于根据用户ID查找学员信息 */
    private final StudentMapper studentMapper;
    /** 用户Mapper，用于根据用户ID查找用户信息 */
    private final SysUserMapper sysUserMapper;

    /**
     * 合同编号日期格式
     */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 创建合同
     * <p>
     * 自动生成合同编号，设置初始状态为待签署(0)，保存合同信息。
     * </p>
     *
     * @param contract 合同信息（id、contractNo、status、signedTime由系统自动设置）
     * @return 创建成功的合同信息
     * @throws BusinessException 如果必填参数缺失
     */
    @Transactional(rollbackFor = Exception.class)
    public Contract create(Contract contract) {
        // 参数校验
        if (contract.getStudentId() == null) {
            throw new BusinessException("学员ID不能为空");
        }
        if (!StringUtils.hasText(contract.getTitle())) {
            throw new BusinessException("合同标题不能为空");
        }
        if (contract.getAmount() == null) {
            throw new BusinessException("合同金额不能为空");
        }

        // 生成合同编号：HT + 日期 + 4位序号
        contract.setContractNo(generateContractNo());

        // 设置初始状态为待签署
        contract.setStatus(0);

        // 保存合同
        this.save(contract);
        return contract;
    }

    /**
     * 根据ID获取合同详情
     *
     * @param id 合同ID
     * @return 合同信息
     * @throws BusinessException 如果合同不存在
     */
    public Contract getById(Long id) {
        Contract contract = super.getById(id);
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }
        return contract;
    }

    /**
     * 根据学员ID获取合同列表
     * <p>查询指定学员的所有合同，按创建时间倒序排列</p>
     *
     * @param studentId 学员ID
     * @return 该学员的合同列表
     */
    public java.util.List<Contract> getByStudentId(Long studentId) {
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Contract::getStudentId, studentId)
               .orderByDesc(Contract::getCreateTime);
        return this.list(wrapper);
    }

    /**
     * 签署合同
     * <p>
     * 学员签署合同，更新合同状态为已签署(1)，记录签署时间和签署数据。
     * 只有状态为待签署(0)的合同才能被签署。
     * STUDENT 角色只能签署自己的合同，STAFF/ADMIN 可以签署任意合同。
     * </p>
     *
     * @param id            合同ID
     * @param signData      签署数据（如签名图片Base64、签署IP等）
     * @param authentication 当前认证信息
     * @return 签署后的合同信息
     * @throws BusinessException 如果合同不存在、状态不允许签署或无权签署
     */
    @Transactional(rollbackFor = Exception.class)
    public Contract sign(Long id, String signData, Authentication authentication) {
        // 查询合同（getById 已在不存在时抛出 BusinessException）
        Contract contract = this.getById(id);

        // 校验合同状态
        if (contract.getStatus() != 0) {
            throw new BusinessException("当前合同状态不允许签署");
        }

        // 验证签署权限：STUDENT 只能签署自己的合同
        Long userId = (Long) authentication.getPrincipal();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        boolean isStudent = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        if (isStudent) {
            Student student = studentMapper.selectById(contract.getStudentId());
            if (student == null || !student.getUserId().equals(userId)) {
                throw new BusinessException("无权签署此合同");
            }
        }

        // 更新合同签署信息
        contract.setStatus(1);
        contract.setSignedTime(LocalDateTime.now());
        contract.setSignData(signData);
        this.updateById(contract);

        return contract;
    }

    /**
     * 分页查询合同
     * <p>
     * 支持按学员ID、合同状态、关键词进行筛选。
     * </p>
     *
     * @param page      当前页码（从1开始）
     * @param size      每页大小
     * @param studentId 学员ID（可选）
     * @param status    合同状态（可选）
     * @param keyword   搜索关键词（可选，匹配合同编号或标题）
     * @return 分页结果
     */
    public Page<Contract> page(long page, long size, Long studentId, Integer status, String keyword) {
        Page<Contract> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();

        // 按学员ID筛选
        if (studentId != null) {
            wrapper.eq(Contract::getStudentId, studentId);
        }

        // 按状态筛选
        if (status != null) {
            wrapper.eq(Contract::getStatus, status);
        }

        // 关键词搜索（匹配合同编号或标题）
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Contract::getContractNo, keyword)
                            .or()
                            .like(Contract::getTitle, keyword));
        }

        // 按创建时间倒序排列
        wrapper.orderByDesc(Contract::getCreateTime);

        return this.page(pageParam, wrapper);
    }

    /**
     * 生成合同编号
     * <p>
     * 格式：HT + 日期(yyyyMMdd) + 4位序号
     * 示例：HT202401150001
     * 使用 Redis INCR 保证多实例部署下的唯一性。
     * </p>
     *
     * @return 唯一的合同编号
     */
    private String generateContractNo() {
        String dateStr = LocalDateTime.now().format(DATE_FORMAT);
        String redisKey = "contract:seq:" + dateStr;
        long seq = redisUtils.increment(redisKey);
        // 每次都设置过期时间（幂等操作），防止 Redis 内存压力淘汰 key 后过期时间丢失
        redisUtils.expire(redisKey, 2, TimeUnit.DAYS);
        // 使用6位序号格式，支持每天生成超过100万份合同（修复：原4位序号最多仅支持9999份）
        return String.format("HT%s%06d", dateStr, seq);
    }
}
