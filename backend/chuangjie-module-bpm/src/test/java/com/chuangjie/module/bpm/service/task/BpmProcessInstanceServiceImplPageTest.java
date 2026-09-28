package com.chuangjie.module.bpm.service.task;

import com.chuangjie.framework.common.pojo.PageParam;
import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.tenant.core.context.TenantContextHolder;
import com.chuangjie.framework.test.core.ut.BaseMockitoUnitTest;
import com.chuangjie.module.bpm.controller.admin.task.vo.instance.BpmProcessInstancePageReqVO;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class BpmProcessInstanceServiceImplPageTest extends BaseMockitoUnitTest {

    private static final Long TENANT_ID = 1L;

    @InjectMocks
    private BpmProcessInstanceServiceImpl processInstanceService;

    @Mock
    private HistoryService historyService;
    @Mock
    private HistoricProcessInstanceQuery processInstanceQuery;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.includeProcessVariables()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceTenantId(String.valueOf(TENANT_ID)))
                .thenReturn(processInstanceQuery);
        when(processInstanceQuery.orderByProcessInstanceStartTime()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.desc()).thenReturn(processInstanceQuery);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void testGetProcessInstancePage_processInstanceIdUsesExactServerQuery() {
        BpmProcessInstancePageReqVO reqVO = new BpmProcessInstancePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setProcessInstanceId("  process-instance-id  ");
        HistoricProcessInstance processInstance = mock(HistoricProcessInstance.class);
        when(processInstanceQuery.processInstanceId("process-instance-id"))
                .thenReturn(processInstanceQuery);
        when(processInstanceQuery.count()).thenReturn(1L);
        when(processInstanceQuery.listPage(0, 10)).thenReturn(List.of(processInstance));

        PageResult<HistoricProcessInstance> result = processInstanceService
                .getProcessInstancePage(null, reqVO);

        assertEquals(1L, result.getTotal());
        assertSame(processInstance, result.getList().get(0));
        verify(processInstanceQuery).processInstanceId("process-instance-id");
        verify(processInstanceQuery).listPage(0, 10);
    }

    @Test
    void testGetProcessInstancePage_noPageSizeReturnsAllRowsForExport() {
        BpmProcessInstancePageReqVO reqVO = new BpmProcessInstancePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        HistoricProcessInstance processInstance = mock(HistoricProcessInstance.class);
        when(processInstanceQuery.count()).thenReturn(1L);
        when(processInstanceQuery.list()).thenReturn(List.of(processInstance));

        PageResult<HistoricProcessInstance> result = processInstanceService
                .getProcessInstancePage(null, reqVO);

        assertEquals(1L, result.getTotal());
        assertSame(processInstance, result.getList().get(0));
        verify(processInstanceQuery).list();
        verify(processInstanceQuery, never()).listPage(anyInt(), anyInt());
    }

}
