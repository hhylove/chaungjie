package com.chuangjie.module.hrm.service.attendance.record.listener;

import com.chuangjie.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import com.chuangjie.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import com.chuangjie.module.hrm.service.attendance.record.HrmAttendanceLeaveService;
import com.chuangjie.module.hrm.service.attendance.record.HrmAttendanceLeaveServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * HRM 请假审批状态监听器
 *
 * @author hhy
 */
@Component
public class HrmAttendanceLeaveStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private HrmAttendanceLeaveService attendanceLeaveService;

    @Override
    protected String getProcessDefinitionKey() {
        return HrmAttendanceLeaveServiceImpl.LEAVE_PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        attendanceLeaveService.updateLeaveApprovalStatus(
                Long.parseLong(event.getBusinessKey()), event.getId(), event.getStatus(), event.getReason());
    }

}
