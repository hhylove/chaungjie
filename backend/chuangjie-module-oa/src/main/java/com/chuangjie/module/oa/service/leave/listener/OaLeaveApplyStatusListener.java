package com.chuangjie.module.oa.service.leave.listener;

import com.chuangjie.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import com.chuangjie.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import com.chuangjie.module.oa.enums.BpmModelConstants;
import com.chuangjie.module.oa.service.leave.OaLeaveApplyService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 请假申请审批结果监听器
 *
 * @author 芋道源码
 */
@Component
public class OaLeaveApplyStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private OaLeaveApplyService leaveApplyService;

    @Override
    protected String getProcessDefinitionKey() {
        return BpmModelConstants.LEAVE_APPLY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        leaveApplyService.updateLeaveApplyStatus(Long.valueOf(event.getBusinessKey()), event.getStatus());
    }

}
