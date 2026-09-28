package com.chuangjie.module.bpm.service.task;

import com.chuangjie.framework.security.core.service.SecurityFrameworkService;
import com.chuangjie.framework.tenant.core.context.TenantContextHolder;
import com.chuangjie.framework.test.core.ut.BaseMockitoUnitTest;
import com.chuangjie.module.bpm.dal.mysql.task.BpmProcessInstanceCopyMapper;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static com.chuangjie.framework.test.core.util.AssertUtils.assertServiceException;
import static com.chuangjie.module.bpm.enums.ErrorCodeConstants.PROCESS_INSTANCE_ACCESS_DENIED;
import static com.chuangjie.module.bpm.enums.ErrorCodeConstants.PROCESS_INSTANCE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BpmProcessInstanceAccessServiceTest extends BaseMockitoUnitTest {

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final String PROCESS_INSTANCE_ID = "process-instance-id";

    @InjectMocks
    private BpmProcessInstanceAccessService accessService;

    @Mock
    private HistoryService historyService;
    @Mock
    private BpmProcessInstanceCopyMapper processInstanceCopyMapper;
    @Mock
    private SecurityFrameworkService securityFrameworkService;
    @Mock
    private HistoricProcessInstanceQuery processInstanceQuery;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void testValidateProcessInstanceViewPermission_starterAllowed() {
        HistoricProcessInstance processInstance = mockProcessInstance(String.valueOf(USER_ID));

        HistoricProcessInstance result = accessService.validateProcessInstanceViewPermission(
                USER_ID, PROCESS_INSTANCE_ID);

        assertSame(processInstance, result);
        verify(historyService, never()).createHistoricTaskInstanceQuery();
        verify(processInstanceCopyMapper, never()).selectCount(any());
    }

    @Test
    void testValidateProcessInstanceViewPermission_unrelatedUserDenied() {
        mockProcessInstance("99");
        HistoricTaskInstanceQuery assigneeQuery = mock(HistoricTaskInstanceQuery.class);
        HistoricTaskInstanceQuery ownerQuery = mock(HistoricTaskInstanceQuery.class);
        when(historyService.createHistoricTaskInstanceQuery()).thenReturn(assigneeQuery, ownerQuery);
        mockParticipantQuery(assigneeQuery);
        mockParticipantQuery(ownerQuery);
        when(assigneeQuery.taskAssignee(String.valueOf(USER_ID))).thenReturn(assigneeQuery);
        when(ownerQuery.taskOwner(String.valueOf(USER_ID))).thenReturn(ownerQuery);
        when(assigneeQuery.count()).thenReturn(0L);
        when(ownerQuery.count()).thenReturn(0L);
        when(processInstanceCopyMapper.selectCount(any())).thenReturn(0L);

        assertServiceException(() -> accessService.validateProcessInstanceViewPermission(
                USER_ID, PROCESS_INSTANCE_ID), PROCESS_INSTANCE_ACCESS_DENIED);
    }

    @Test
    void testValidateProcessInstanceViewPermission_crossTenantLooksNotFound() {
        mockProcessQuery();
        when(processInstanceQuery.singleResult()).thenReturn(null);

        assertServiceException(() -> accessService.validateProcessInstanceViewPermission(
                USER_ID, PROCESS_INSTANCE_ID), PROCESS_INSTANCE_NOT_EXISTS);
        verify(processInstanceQuery).processInstanceTenantId(String.valueOf(TENANT_ID));
    }

    private HistoricProcessInstance mockProcessInstance(String startUserId) {
        mockProcessQuery();
        HistoricProcessInstance processInstance = mock(HistoricProcessInstance.class);
        when(processInstance.getStartUserId()).thenReturn(startUserId);
        when(processInstanceQuery.singleResult()).thenReturn(processInstance);
        when(securityFrameworkService.hasAnyPermissions(any(String[].class))).thenReturn(false);
        return processInstance;
    }

    private void mockProcessQuery() {
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceId(PROCESS_INSTANCE_ID)).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceTenantId(String.valueOf(TENANT_ID))).thenReturn(processInstanceQuery);
    }

    private void mockParticipantQuery(HistoricTaskInstanceQuery query) {
        when(query.processInstanceId(PROCESS_INSTANCE_ID)).thenReturn(query);
        when(query.taskTenantId(String.valueOf(TENANT_ID))).thenReturn(query);
    }

}
