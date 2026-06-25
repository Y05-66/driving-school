package com.drivingschool.reminder.controller;

import com.drivingschool.common.result.R;
import com.drivingschool.reminder.entity.Reminder;
import com.drivingschool.reminder.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 自动提醒控制器
 * <p>
 * 提供提醒相关的RESTful API接口，包括查看我的提醒、取消提醒、创建提醒等操作。
 * </p>
 *
 * @author drivingschool
 * @since 1.0.0
 */
@Tag(name = "自动提醒")
@RestController
@RequestMapping("/reminders")
@RequiredArgsConstructor
public class ReminderController {

    /** 提醒服务，处理提醒创建和管理的核心业务逻辑 */
    private final ReminderService reminderService;

    /**
     * 获取当前用户的提醒列表
     * <p>
     * 查询当前登录用户的所有提醒，按提醒时间倒序排列。
     * </p>
     *
     * @param authentication Spring Security认证对象
     * @return 提醒列表
     */
    @Operation(summary = "获取我的提醒")
    @GetMapping("/my")
    public R<List<Reminder>> getMyReminders(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<Reminder> list = reminderService.getByUserId(userId);
        return R.ok(list);
    }

    /**
     * 取消提醒
     * <p>
     * 取消指定的待发送提醒。
     * 只有状态为"待发送"的提醒才能被取消。
     * </p>
     *
     * @param id 提醒ID
     * @return 取消后的提醒信息
     */
    @Operation(summary = "取消提醒")
    @PutMapping("/{id}/cancel")
    public R<Reminder> cancel(@PathVariable Long id) {
        Reminder reminder = reminderService.cancel(id);
        return R.ok(reminder);
    }

    /**
     * 管理员创建提醒
     * <p>
     * 管理员手动创建提醒，可以指定目标用户、提醒类型、内容等。
     * </p>
     *
     * @param reminder 提醒信息
     * @return 创建成功的提醒信息
     */
    @Operation(summary = "创建提醒")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public R<Reminder> create(@RequestBody Reminder reminder) {
        Reminder created = reminderService.create(reminder);
        return R.ok(created);
    }
}
