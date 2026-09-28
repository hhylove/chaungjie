package com.chuangjie.module.bpm.service.task;

import com.chuangjie.framework.tenant.core.context.TenantContextHolder;
import com.chuangjie.framework.test.core.ut.BaseMockitoUnitTest;
import com.chuangjie.module.bpm.controller.admin.task.vo.task.BpmTaskTransferReqVO;
import com.chuangjie.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import com.chuangjie.module.bpm.enums.task.BpmTaskStatusEnum;
import com.chuangjie.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import com.chuangjie.module.bpm.service.definition.BpmModelService;
import com.chuangjie.module.bpm.service.definition.BpmProcessDefinitionService;
import com.chuangjie.module.bpm.service.task.dto.BpmTaskWithdrawInfoDTO;
import com.chuangjie.module.system.api.user.AdminUserApi;
import com.chuangjie.module.system.api.user.dto.AdminUserRespDTO;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.chuangjie.module.bpm.enums.ErrorCodeConstants.TASK_WITHDRAW_FAIL_COMPLEX_FLOW;
import static com.chuangjie.module.bpm.enums.ErrorCodeConstants.TASK_WITHDRAW_FAIL_NOT_ALLOW;
import static com.chuangjie.module.bpm.enums.ErrorCodeConstants.TASK_TRANSFER_FAIL_USER_DISABLED;
import static com.chuangjie.framework.test.core.util.AssertUtils.assertServiceException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BpmTaskServiceImplWithdrawTest extends BaseMockitoUnitTest {

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final String PROCESS_INSTANCE_ID = "process-instance-id";
    private static final String PROCESS_DEFINITION_ID = "process-definition-id";
    private static final String SOURCE_TASK_ID = "source-task-id";
    private static final String SOURCE_ACTIVITY_ID = "source-user-task";
    private static final String NEXT_ACTIVITY_ID = "next-user-task";

    @InjectMocks
    private BpmTaskServiceImpl taskServiceImpl;

    @Mock
    private TaskService taskService;
    @Mock
    private HistoryService historyService;
    @Mock
    private RuntimeService runtimeService;
    @Mock
    private BpmProcessDefinitionService bpmProcessDefinitionService;
    @Mock
    private BpmModelService modelService;
    @Mock
    private AdminUserApi adminUserApi;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void testGetTaskWithdrawInfoMap_directSerialTaskAllowed() {
        HistoricTaskInstance sourceTask = mockWithdrawContext(true, 1);

        BpmTaskWithdrawInfoDTO result = taskServiceImpl
                .getTaskWithdrawInfoMap(USER_ID, List.of(sourceTask)).get(SOURCE_TASK_ID);

        assertNotNull(result);
        assertTrue(result.getWithdrawable());
        assertNull(result.getReason());
    }

    @Test
    void testGetTaskWithdrawInfoMap_parallelRunningTasksDenied() {
        HistoricTaskInstance sourceTask = mockWithdrawContext(true, 2);

        BpmTaskWithdrawInfoDTO result = taskServiceImpl
                .getTaskWithdrawInfoMap(USER_ID, List.of(sourceTask)).get(SOURCE_TASK_ID);

        assertNotNull(result);
        assertFalse(result.getWithdrawable());
        assertEquals(TASK_WITHDRAW_FAIL_COMPLEX_FLOW.getMsg(), result.getReason());
    }

    @Test
    void testGetTaskWithdrawInfoMap_definitionDisablesWithdrawDenied() {
        HistoricTaskInstance sourceTask = mockWithdrawContext(false, 1);

        BpmTaskWithdrawInfoDTO result = taskServiceImpl
                .getTaskWithdrawInfoMap(USER_ID, List.of(sourceTask)).get(SOURCE_TASK_ID);

        assertNotNull(result);
        assertFalse(result.getWithdrawable());
        assertEquals(TASK_WITHDRAW_FAIL_NOT_ALLOW.getMsg(), result.getReason());
    }

    @Test
    void testTransferTask_disabledUserDenied() {
        Task task = mock(Task.class);
        when(task.getTenantId()).thenReturn(String.valueOf(TENANT_ID));
        when(task.getAssignee()).thenReturn(String.valueOf(USER_ID));
        TaskQuery taskQuery = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskId(SOURCE_TASK_ID)).thenReturn(taskQuery);
        when(taskQuery.includeTaskLocalVariables()).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(task);
        Long disabledUserId = 20L;
        when(adminUserApi.getUser(disabledUserId)).thenReturn(new AdminUserRespDTO()
                .setId(disabledUserId).setStatus(1));

        BpmTaskTransferReqVO reqVO = new BpmTaskTransferReqVO()
                .setId(SOURCE_TASK_ID).setAssigneeUserId(disabledUserId).setReason("转办");

        assertServiceException(() -> taskServiceImpl.transferTask(USER_ID, reqVO),
                TASK_TRANSFER_FAIL_USER_DISABLED);
    }

    private HistoricTaskInstance mockWithdrawContext(boolean allowWithdraw, int runningTaskCount) {
        HistoricTaskInstance sourceTask = mock(HistoricTaskInstance.class);
        when(sourceTask.getId()).thenReturn(SOURCE_TASK_ID);
        when(sourceTask.getProcessInstanceId()).thenReturn(PROCESS_INSTANCE_ID);
        when(sourceTask.getProcessDefinitionId()).thenReturn(PROCESS_DEFINITION_ID);
        if (allowWithdraw) {
            when(sourceTask.getTaskDefinitionKey()).thenReturn(SOURCE_ACTIVITY_ID);
        }
        when(sourceTask.getTenantId()).thenReturn(String.valueOf(TENANT_ID));
        when(sourceTask.getAssignee()).thenReturn(String.valueOf(USER_ID));
        when(sourceTask.getTaskLocalVariables()).thenReturn(Map.of(
                BpmnVariableConstants.TASK_VARIABLE_STATUS, BpmTaskStatusEnum.APPROVE.getStatus()));

        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getId()).thenReturn(PROCESS_INSTANCE_ID);
        ProcessInstanceQuery processInstanceQuery = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceIds(anySet())).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceTenantId(String.valueOf(TENANT_ID))).thenReturn(processInstanceQuery);
        when(processInstanceQuery.list()).thenReturn(List.of(processInstance));

        BpmProcessDefinitionInfoDO definitionInfo = new BpmProcessDefinitionInfoDO()
                .setAllowWithdrawTask(allowWithdraw);
        when(bpmProcessDefinitionService.getProcessDefinitionInfoMap(anySet()))
                .thenReturn(Map.of(PROCESS_DEFINITION_ID, definitionInfo));

        TaskQuery taskQuery = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.processInstanceIdIn(anyList())).thenReturn(taskQuery);
        when(taskQuery.taskTenantId(String.valueOf(TENANT_ID))).thenReturn(taskQuery);
        when(taskQuery.active()).thenReturn(taskQuery);
        List<Task> runningTasks = java.util.stream.IntStream.range(0, runningTaskCount)
                .mapToObj(index -> mockRunningTask("execution-" + index, allowWithdraw && runningTaskCount == 1))
                .toList();
        when(taskQuery.list()).thenReturn(runningTasks);

        if (allowWithdraw) {
            when(modelService.getBpmnModelByDefinitionId(PROCESS_DEFINITION_ID)).thenReturn(buildSerialModel());
        }
        if (allowWithdraw && runningTaskCount == 1) {
            Date sourceEndTime = new Date();
            when(sourceTask.getEndTime()).thenReturn(sourceEndTime);
            HistoricTaskInstanceQuery historyQuery = mock(HistoricTaskInstanceQuery.class);
            when(historyService.createHistoricTaskInstanceQuery()).thenReturn(historyQuery);
            when(historyQuery.processInstanceId(PROCESS_INSTANCE_ID)).thenReturn(historyQuery);
            when(historyQuery.taskTenantId(String.valueOf(TENANT_ID))).thenReturn(historyQuery);
            when(historyQuery.taskDefinitionKey(NEXT_ACTIVITY_ID)).thenReturn(historyQuery);
            when(historyQuery.taskCreatedAfter(sourceEndTime)).thenReturn(historyQuery);
            when(historyQuery.finished()).thenReturn(historyQuery);
            when(historyQuery.count()).thenReturn(0L);
        }
        return sourceTask;
    }

    private Task mockRunningTask(String executionId, boolean includeActivityDetails) {
        Task task = mock(Task.class);
        when(task.getProcessInstanceId()).thenReturn(PROCESS_INSTANCE_ID);
        if (includeActivityDetails) {
            when(task.getTaskDefinitionKey()).thenReturn(NEXT_ACTIVITY_ID);
            when(task.getExecutionId()).thenReturn(executionId);
        }
        return task;
    }

    private BpmnModel buildSerialModel() {
        UserTask sourceTask = new UserTask();
        sourceTask.setId(SOURCE_ACTIVITY_ID);
        UserTask nextTask = new UserTask();
        nextTask.setId(NEXT_ACTIVITY_ID);
        SequenceFlow sequenceFlow = new SequenceFlow(SOURCE_ACTIVITY_ID, NEXT_ACTIVITY_ID);
        sequenceFlow.setSourceFlowElement(sourceTask);
        sequenceFlow.setTargetFlowElement(nextTask);
        sourceTask.setOutgoingFlows(List.of(sequenceFlow));
        nextTask.setIncomingFlows(List.of(sequenceFlow));

        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        process.setId("process");
        process.addFlowElement(sourceTask);
        process.addFlowElement(nextTask);
        process.addFlowElement(sequenceFlow);
        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.addProcess(process);
        return bpmnModel;
    }

}
