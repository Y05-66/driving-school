package com.drivingschool.channel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.channel.entity.Channel;
import com.drivingschool.channel.entity.StudentChannel;
import com.drivingschool.channel.mapper.ChannelMapper;
import com.drivingschool.channel.mapper.StudentChannelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 招生渠道服务类
 * 提供渠道管理、学员绑定和渠道统计功能
 *
 * @author drivingschool
 */
@Service
@RequiredArgsConstructor
public class ChannelService extends ServiceImpl<ChannelMapper, Channel> {

    /** 渠道Mapper，用于渠道的增删改查 */
    private final ChannelMapper channelMapper;
    /** 学员渠道关联Mapper，用于管理学员与渠道的绑定关系 */
    private final StudentChannelMapper studentChannelMapper;

    /**
     * 创建招生渠道
     * 新增渠道默认状态为启用
     *
     * @param channel 渠道信息（name, type, contact, phone, cost）
     * @return 创建成功的渠道
     */
    public Channel createChannel(Channel channel) {
        // 默认启用状态
        if (channel.getStatus() == null) {
            channel.setStatus(1);
        }
        channelMapper.insert(channel);
        return channel;
    }

    /**
     * 查询所有启用的渠道列表
     * 按创建时间倒序排列
     *
     * @return 启用状态的渠道列表
     */
    public List<Channel> listChannels() {
        LambdaQueryWrapper<Channel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Channel::getStatus, 1)
               .orderByDesc(Channel::getCreateTime);
        return channelMapper.selectList(wrapper);
    }

    /**
     * 绑定学员到渠道
     * 记录学员是通过哪个渠道报名的，用于后续统计分析
     *
     * @param studentId 学员ID
     * @param channelId 渠道ID
     * @return 创建的绑定记录
     */
    @Transactional(rollbackFor = Exception.class)
    public StudentChannel bindStudent(Long studentId, Long channelId) {
        // 验证渠道是否存在且启用
        Channel channel = channelMapper.selectById(channelId);
        if (channel == null || channel.getStatus() == 0) {
            throw new BusinessException("渠道不存在或已停用");
        }

        // 检查是否已绑定
        LambdaQueryWrapper<StudentChannel> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(StudentChannel::getStudentId, studentId)
                    .eq(StudentChannel::getChannelId, channelId);
        Long count = studentChannelMapper.selectCount(checkWrapper);
        if (count > 0) {
            throw new BusinessException("该学员已绑定此渠道");
        }

        // 创建绑定关系
        StudentChannel studentChannel = new StudentChannel();
        studentChannel.setStudentId(studentId);
        studentChannel.setChannelId(channelId);
        studentChannelMapper.insert(studentChannel);

        return studentChannel;
    }

    /**
     * 获取各渠道的招生统计数据
     * 统计每个渠道绑定的学员数量和人均获客成本
     *
     * @return 渠道统计数据列表，包含渠道名称、学员数量、成本、人均成本
     */
    public List<Map<String, Object>> getChannelStats() {
        // 查询所有启用的渠道
        List<Channel> channels = listChannels();

        // 统计每个渠道的学员数量
        List<Map<String, Object>> stats = channels.stream().map(channel -> {
            Map<String, Object> stat = new HashMap<>();
            stat.put("channelId", channel.getId());
            stat.put("channelName", channel.getName());
            stat.put("channelType", channel.getType());
            stat.put("cost", channel.getCost());

            // 查询该渠道绑定的学员数量
            LambdaQueryWrapper<StudentChannel> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(StudentChannel::getChannelId, channel.getId());
            Long studentCount = studentChannelMapper.selectCount(wrapper);
            stat.put("studentCount", studentCount);

            // 计算人均获客成本
            if (studentCount > 0 && channel.getCost() != null) {
                stat.put("costPerStudent", channel.getCost().divide(
                        new java.math.BigDecimal(studentCount), 2, java.math.RoundingMode.HALF_UP));
            } else {
                stat.put("costPerStudent", java.math.BigDecimal.ZERO);
            }

            return stat;
        }).collect(Collectors.toList());

        return stats;
    }
}
