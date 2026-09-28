package com.chuangjie.module.oa.service.officialdoc.listener;

import com.chuangjie.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import com.chuangjie.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import com.chuangjie.module.oa.enums.BpmModelConstants;
import com.chuangjie.module.oa.service.officialdoc.OaOfficialDocReceiveService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 公文收文审批结果监听器
 *
 * @author 芋道源码
 */
@Component
public class OaOfficialDocReceiveStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private OaOfficialDocReceiveService officialDocReceiveService;

    @Override
    protected String getProcessDefinitionKey() {
        return BpmModelConstants.OFFICIAL_DOC_RECEIVE;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        officialDocReceiveService.updateOfficialDocReceiveStatus(Long.parseLong(event.getBusinessKey()), event.getStatus());
    }

}
