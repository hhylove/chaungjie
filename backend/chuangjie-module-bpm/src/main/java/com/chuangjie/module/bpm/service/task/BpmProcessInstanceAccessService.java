package com.chuangjie.module.bpm.service.task;

import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.framework.security.core.service.SecurityFrameworkService;
import com.chuangjie.module.bpm.dal.dataobject.task.BpmProcessInstanceCopyDO;
import com.chuangjie.module.bpm.dal.mysql.task.BpmProcessInstanceCopyMapper;
import com.chuangjie.module.bpm.framework.flowable.core.util.FlowableUtils;
import jakarta.annotation.Resource;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static com.chuangjie.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.chuangjie.module.bpm.enums.ErrorCodeConstants.PROCESS_INSTANCE_ACCESS_DENIED;
import static com.chuangjie.module.bpm.enums.ErrorCodeConstants.PROCESS_INSTANCE_NOT_EXISTS;
import static com.chuangjie.module.bpm.enums.ErrorCodeConstants.TASK_NOT_EXISTS;

/**
 * BPM 流程实例访问权限 Service。
 *
 * <p>Flowable 表不经过 MyBatis 多租户拦截器，所以所有面向用户的按 ID 查询都必须先经过本服务。</p>
 */
@Service
public class BpmProcessInstanceAccessService {

    private static final String PROCESS_INSTANCE_MANAGER_PERMISSION = "bpm:process-instance:manager-query";
    private static final String TASK_MANAGER_PERMISSION = "bpm:task:manager-query";

    @Resource
    private HistoryService historyService;
    @Resource
    private BpmProcessInstanceCopyMapper processInstanceCopyMapper;
    @Resource
    private SecurityFrameworkService securityFrameworkService;

    /**
     * 校验当前用户是否可以查看指定流程实例。
     */
    public HistoricProcessInstance validateProcessInstanceViewPermission(Long userId, String processInstanceId) {
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .processInstanceTenantId(FlowableUtils.getTenantId())
                .singleResult();
        if (processInstance == null) {
            throw exception(PROCESS_INSTANCE_NOT_EXISTS);
        }
        if (securityFrameworkService.hasAnyPermissions(PROCESS_INSTANCE_MANAGER_PERMISSION, TASK_MANAGER_PERMISSION)
                || Objects.equals(processInstance.getStartUserId(), String.valueOf(userId))
                || isTaskParticipant(userId, processInstanceId)
                || isCopyUser(userId, processInstanceId)) {
            return processInstance;
        }
        throw exception(PROCESS_INSTANCE_ACCESS_DENIED);
    }

    /**
     * 校验当前用户是否可以查看指定任务所属的流程实例。
     */
    public HistoricTaskInstance validateTaskViewPermission(Long userId, String taskId) {
        HistoricTaskInstance task = historyService.createHistoricTaskInstanceQuery()
                .taskId(taskId)
                .taskTenantId(FlowableUtils.getTenantId())
                .singleResult();
        if (task == null) {
            throw exception(TASK_NOT_EXISTS);
        }
        validateProcessInstanceViewPermission(userId, task.getProcessInstanceId());
        return task;
    }

    private boolean isTaskParticipant(Long userId, String processInstanceId) {
        String userIdStr = String.valueOf(userId);
        long assigneeCount = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .taskTenantId(FlowableUtils.getTenantId())
                .taskAssignee(userIdStr)
                .count();
        if (assigneeCount > 0) {
            return true;
        }
        return historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .taskTenantId(FlowableUtils.getTenantId())
                .taskOwner(userIdStr)
                .count() > 0;
    }

    private boolean isCopyUser(Long userId, String processInstanceId) {
        return processInstanceCopyMapper.selectCount(new LambdaQueryWrapperX<BpmProcessInstanceCopyDO>()
                .eq(BpmProcessInstanceCopyDO::getProcessInstanceId, processInstanceId)
                .eq(BpmProcessInstanceCopyDO::getUserId, userId)) > 0;
    }

}
