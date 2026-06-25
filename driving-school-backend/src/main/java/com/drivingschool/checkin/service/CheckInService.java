package com.drivingschool.checkin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drivingschool.checkin.entity.CheckIn;
import com.drivingschool.checkin.mapper.CheckInMapper;
import com.drivingschool.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 学时签到服务类
 * <p>
 * 提供签到签退的核心业务逻辑，包括签到、签退、查询、二维码生成等功能。
 * 支持扫码签到和手动签到两种方式。
 * </p>
 *
 * @author drivingschool
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class CheckInService extends ServiceImpl<CheckInMapper, CheckIn> {

    /**
     * 学员签到
     * <p>
     * 为指定课程创建签到记录，设置签到时间和地点。
     * 如果该学员对该课程已有签到记录，则抛出异常。
     * </p>
     *
     * @param lessonId  课程ID
     * @param studentId 学员ID
     * @param coachId   教练ID
     * @param location  签到地点
     * @param checkInType 签到方式（1扫码/2手动）
     * @return 签到记录
     * @throws BusinessException 如果参数缺失或已存在签到记录
     */
    @Transactional(rollbackFor = Exception.class)
    public CheckIn checkIn(Long lessonId, Long studentId, Long coachId, String location, Integer checkInType) {
        // 参数校验
        if (lessonId == null || studentId == null) {
            throw new BusinessException("课程ID和学员ID不能为空");
        }

        // 检查是否已有签到记录
        LambdaQueryWrapper<CheckIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckIn::getLessonId, lessonId)
               .eq(CheckIn::getStudentId, studentId)
               .in(CheckIn::getStatus, 0, 1); // 待签到或已签到状态
        Long count = this.count(wrapper);
        if (count > 0) {
            throw new BusinessException("该学员对此课程已有签到记录");
        }

        // 创建签到记录
        CheckIn checkIn = new CheckIn();
        checkIn.setLessonId(lessonId);
        checkIn.setStudentId(studentId);
        checkIn.setCoachId(coachId);
        checkIn.setCheckInTime(LocalDateTime.now());
        checkIn.setCheckInLocation(location);
        checkIn.setCheckInType(checkInType != null ? checkInType : 1); // 默认扫码签到
        checkIn.setStatus(1); // 已签到

        this.save(checkIn);
        return checkIn;
    }

    /**
     * 学员签退
     * <p>
     * 更新签到记录，设置签退时间，状态变更为已签退。
     * 只有状态为"已签到"的记录才能进行签退。
     * </p>
     *
     * @param lessonId  课程ID
     * @param studentId 学员ID
     * @return 签退后的签到记录
     * @throws BusinessException 如果签到记录不存在或状态不允许签退
     */
    @Transactional(rollbackFor = Exception.class)
    public CheckIn checkOut(Long lessonId, Long studentId) {
        // 查询签到记录
        LambdaQueryWrapper<CheckIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckIn::getLessonId, lessonId)
               .eq(CheckIn::getStudentId, studentId)
               .eq(CheckIn::getStatus, 1); // 已签到状态
        CheckIn checkIn = this.getOne(wrapper);

        if (checkIn == null) {
            throw new BusinessException("未找到有效的签到记录");
        }

        // 更新签退信息
        checkIn.setCheckOutTime(LocalDateTime.now());
        checkIn.setStatus(2); // 已签退
        this.updateById(checkIn);

        return checkIn;
    }

    /**
     * 根据课程ID获取签到记录列表
     * <p>查询指定课程的所有签到记录，按签到时间倒序排列</p>
     *
     * @param lessonId 课程ID
     * @return 签到记录列表
     */
    public java.util.List<CheckIn> getByLessonId(Long lessonId) {
        LambdaQueryWrapper<CheckIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckIn::getLessonId, lessonId)
               .orderByDesc(CheckIn::getCheckInTime);
        return this.list(wrapper);
    }

    /**
     * 生成签到二维码数据
     * <p>
     * 为指定课程生成签到二维码的编码数据。
     * 二维码包含课程ID、时间戳和唯一令牌，用于验证签到的有效性。
     * </p>
     *
     * @param lessonId 课程ID
     * @return 二维码数据（包含lessonId、timestamp、token）
     */
    public Map<String, Object> generateQrCode(Long lessonId) {
        if (lessonId == null) {
            throw new BusinessException("课程ID不能为空");
        }

        // 生成二维码数据
        Map<String, Object> qrData = new HashMap<>();
        qrData.put("lessonId", lessonId);
        qrData.put("timestamp", System.currentTimeMillis());
        qrData.put("token", UUID.randomUUID().toString()); // 唯一令牌，防重复签到

        return qrData;
    }
}
