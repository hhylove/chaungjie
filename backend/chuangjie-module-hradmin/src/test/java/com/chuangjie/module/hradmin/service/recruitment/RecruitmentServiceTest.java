package com.chuangjie.module.hradmin.service.recruitment;

import com.chuangjie.framework.common.exception.ServiceException;
import com.chuangjie.framework.security.core.service.SecurityFrameworkService;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.chuangjie.module.hradmin.controller.admin.employee.vo.EmployeeSaveReqVO;
import com.chuangjie.module.hradmin.dal.dataobject.recruitment.CandidateDO;
import com.chuangjie.module.hradmin.dal.dataobject.recruitment.HiringRequestDO;
import com.chuangjie.module.hradmin.dal.mysql.recruitment.CandidateHistoryMapper;
import com.chuangjie.module.hradmin.dal.mysql.recruitment.CandidateMapper;
import com.chuangjie.module.hradmin.dal.mysql.recruitment.HiringRequestMapper;
import com.chuangjie.module.hradmin.service.employee.EmployeeArchiveService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecruitmentServiceTest {
    @InjectMocks private RecruitmentService service;
    @Mock private HiringRequestMapper requests;
    @Mock private CandidateMapper candidates;
    @Mock private CandidateHistoryMapper history;
    @Mock private EmployeeArchiveService employees;
    @Mock private SecurityFrameworkService security;

    @Test
    void candidateCannotConvertBeforeApproval() {
        CandidateDO candidate = new CandidateDO(); candidate.setId(2L); candidate.setStage("录用审批");
        when(candidates.selectById(2L)).thenReturn(candidate);
        assertThrows(ServiceException.class, () -> service.convert(2L, new EmployeeSaveReqVO()));
        verify(employees, never()).create(any());
    }

    @Test
    void approvedCandidateBecomesPendingEmployeeOnce() {
        CandidateDO candidate = new CandidateDO(); candidate.setId(2L); candidate.setRequestId(3L);
        candidate.setName("候选人"); candidate.setStage("待入职");
        HiringRequestDO request = new HiringRequestDO(); request.setId(3L); request.setDeptId(4L);
        request.setJob("运营"); request.setCount(1); request.setStatus("招聘中");
        when(candidates.selectById(2L)).thenReturn(candidate);
        when(requests.lockById(eq(3L), isNull())).thenReturn(request);
        when(candidates.selectList(org.mockito.ArgumentMatchers.<SFunction<CandidateDO, ?>>any(), eq(3L))).thenReturn(List.of(candidate));
        when(employees.create(any())).thenReturn(8L);
        when(candidates.markConverted(eq(2L), eq(8L), isNull())).thenReturn(1);
        EmployeeSaveReqVO archive = new EmployeeSaveReqVO();
        assertEquals(8L, service.convert(2L, archive));
        assertEquals("候选人", archive.getName());
        assertEquals("运营", archive.getPositionName());
        assertEquals(0, archive.getEmploymentStatus());
    }
}
