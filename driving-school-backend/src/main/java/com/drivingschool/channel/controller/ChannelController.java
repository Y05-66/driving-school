package com.drivingschool.channel.controller;

import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.common.result.R;
import com.drivingschool.channel.entity.Channel;
import com.drivingschool.channel.entity.StudentChannel;
import com.drivingschool.channel.service.ChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 招生渠道控制器
 * 提供渠道管理、学员绑定和渠道统计的RESTful接口
 *
 * @author drivingschool
 */
@Tag(name = "招生渠道")
@RestController
@RequestMapping("/channels")
@RequiredArgsConstructor
public class ChannelController {

    /** 渠道服务，处理渠道管理和统计的核心业务逻辑 */
    private final ChannelService channelService;

    /**
     * 创建招生渠道
     * 新增一个招生渠道，包含名称、类型、联系方式、成本等信息
     *
     * @param channel 渠道信息
     * @return 创建成功的渠道
     */
    @Operation(summary = "创建招生渠道")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<Channel> createChannel(@RequestBody Channel channel) {
        Channel created = channelService.createChannel(channel);
        return R.ok(created);
    }

    /**
     * 查询所有启用的渠道
     * 返回状态为启用的渠道列表
     *
     * @return 渠道列表
     */
    @Operation(summary = "查询所有渠道")
    @GetMapping
    public R<List<Channel>> listChannels() {
        List<Channel> channels = channelService.listChannels();
        return R.ok(channels);
    }

    /**
     * 绑定学员到渠道
     * 记录学员通过哪个渠道报名，用于招生效果统计
     *
     * @param params 请求参数，包含studentId和channelId
     * @return 创建的绑定记录
     */
    @Operation(summary = "绑定学员到渠道")
    @PostMapping("/bind")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<StudentChannel> bindStudent(@RequestBody Map<String, Long> params) {
        Long studentId = params.get("studentId");
        Long channelId = params.get("channelId");
        if (studentId == null) {
            throw new BusinessException("学员ID不能为空");
        }
        if (channelId == null) {
            throw new BusinessException("渠道ID不能为空");
        }
        StudentChannel studentChannel = channelService.bindStudent(studentId, channelId);
        return R.ok(studentChannel);
    }

    /**
     * 获取各渠道招生统计
     * 统计每个渠道的学员数量、成本和人均获客成本
     *
     * @return 渠道统计数据列表
     */
    @Operation(summary = "渠道招生统计")
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<List<Map<String, Object>>> getChannelStats() {
        List<Map<String, Object>> stats = channelService.getChannelStats();
        return R.ok(stats);
    }
}
