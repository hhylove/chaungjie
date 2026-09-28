package com.chuangjie.module.oa.job.schedule;

import cn.hutool.core.util.StrUtil;
import com.chuangjie.framework.quartz.core.handler.JobHandler;
import com.chuangjie.framework.tenant.core.job.TenantJob;
import com.chuangjie.module.oa.service.schedule.OaScheduleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * OA 日程提醒 Job
 *
 * @author 芋道源码
 */
@Component
public class OaScheduleReminderJob implements JobHandler {

    @Resource
    private OaScheduleService scheduleService;

    @Override
    @TenantJob
    public String execute(String param) {
        int count = scheduleService.sendScheduleReminders();
        return StrUtil.format("发送日程提醒 {} 条", count);
    }

}
