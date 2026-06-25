package com.drivingschool.common.config;

import com.drivingschool.finance.service.InstallmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 定时任务配置
 * <p>
 * 包含系统中所有定时任务的定义。
 * </p>
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledTaskConfig {

    /** 分期付款服务，用于执行逾期检查等定时任务 */
    private final InstallmentService installmentService;

    /**
     * 每天凌晨2点检查分期逾期情况
     * <p>
     * 遍历所有进行中的分期计划，将超过还款日未支付的明细标记为逾期。
     * </p>
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkInstallmentOverdue() {
        log.info("开始检查分期逾期情况...");
        try {
            int overdueCount = installmentService.checkOverdue();
            log.info("分期逾期检查完成，本次标记逾期明细{}条", overdueCount);
        } catch (Exception e) {
            log.error("分期逾期检查异常", e);
        }
    }
}
