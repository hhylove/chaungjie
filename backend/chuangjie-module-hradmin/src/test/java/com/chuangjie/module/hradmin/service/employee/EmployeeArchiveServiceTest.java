package com.chuangjie.module.hradmin.service.employee;

import com.chuangjie.framework.common.exception.ServiceException;
import com.chuangjie.module.hradmin.controller.admin.employee.vo.EmployeeSaveReqVO;
import com.chuangjie.module.hradmin.dal.dataobject.employee.EmployeeArchiveDO;
import com.chuangjie.module.hradmin.dal.mysql.employee.EmployeeArchiveMapper;
import com.chuangjie.module.system.api.dept.DeptApi;
import com.chuangjie.module.system.api.user.AdminUserApi;
import com.chuangjie.module.system.api.user.dto.AdminUserRespDTO;
import com.chuangjie.module.system.dal.dataobject.wecom.WecomUserDO;
import com.chuangjie.module.system.dal.mysql.wecom.WecomUserMapper;
import com.chuangjie.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.List;

import static com.chuangjie.module.hradmin.enums.ErrorCodeConstants.EMPLOYEE_USER_EXISTS;
import static com.chuangjie.module.hradmin.enums.ErrorCodeConstants.EMPLOYEE_ARRIVAL_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(MockitoExtension.class)
class EmployeeArchiveServiceTest {
    @InjectMocks private EmployeeArchiveService service;
    @Mock private EmployeeArchiveMapper mapper;
    @Mock private AdminUserApi userApi;
    @Mock private AdminUserService userService;
    @Mock private WecomUserMapper wecomUserMapper;
    @Mock private DeptApi deptApi;

    @Test
    void duplicateAccountCannotCreateSecondArchive() {
        EmployeeArchiveDO pending = new EmployeeArchiveDO();
        pending.setId(11L);
        pending.setName("员工");
        pending.setEmploymentStatus(0);
        pending.setHireDate(LocalDate.now());
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(141L);
        user.setStatus(1);
        EmployeeArchiveDO existing = new EmployeeArchiveDO();
        existing.setId(10L);
        when(mapper.selectById(11L)).thenReturn(pending);
        when(userApi.getUser(141L)).thenReturn(user);
        when(mapper.selectByUserId(141L)).thenReturn(existing);

        ServiceException error = assertThrows(ServiceException.class, () -> service.confirmArrival(11L, 141L));

        assertEquals(EMPLOYEE_USER_EXISTS.getCode(), error.getCode());
        verify(userService, never()).updateUserStatus(any(), any());
    }

    @Test
    void newArchiveGetsNumberAndRemainsPendingWithoutAccount() {
        EmployeeSaveReqVO request = request(null);

        service.create(request);

        verify(mapper).insert(argThat((EmployeeArchiveDO row) -> row.getEmployeeNo().startsWith("CJ-")
                && row.getUserId() == null && row.getEmploymentStatus() == 0));
        verify(userService, never()).updateUserStatus(any(), any());
    }

    @Test
    void arrivalBindsAndEnablesDisabledAccount() {
        EmployeeArchiveDO pending = new EmployeeArchiveDO();
        pending.setId(11L);
        pending.setName("员工");
        pending.setEmploymentStatus(0);
        pending.setHireDate(LocalDate.now());
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(141L);
        user.setStatus(1);
        when(mapper.selectById(11L)).thenReturn(pending);
        when(userApi.getUser(141L)).thenReturn(user);
        WecomUserDO wecomUser = new WecomUserDO();
        wecomUser.setName("员工");
        when(wecomUserMapper.selectBySystemUserId(141L)).thenReturn(wecomUser);

        service.confirmArrival(11L, 141L);

        verify(mapper).updateById(argThat((EmployeeArchiveDO row) -> row.getUserId().equals(141L) && row.getEmploymentStatus() == 1));
        verify(userService).updateUserStatus(141L, 0);
    }

    @Test
    void arrivalRejectsUnmatchedAccount() {
        EmployeeArchiveDO pending = new EmployeeArchiveDO();
        pending.setId(11L);
        pending.setName("员工");
        pending.setEmploymentStatus(0);
        pending.setHireDate(LocalDate.now());
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(141L);
        user.setStatus(1);
        when(mapper.selectById(11L)).thenReturn(pending);
        when(userApi.getUser(141L)).thenReturn(user);

        assertThrows(ServiceException.class, () -> service.confirmArrival(11L, 141L));
        verify(userService, never()).updateUserStatus(any(), any());
    }

    @Test
    void earlyArrivalCannotEnableAccount() {
        EmployeeArchiveDO pending = new EmployeeArchiveDO();
        pending.setId(11L);
        pending.setEmploymentStatus(0);
        pending.setHireDate(LocalDate.now().plusDays(1));
        when(mapper.selectById(11L)).thenReturn(pending);

        ServiceException error = assertThrows(ServiceException.class, () -> service.confirmArrival(11L, 141L));

        assertEquals(EMPLOYEE_ARRIVAL_INVALID.getCode(), error.getCode());
        verify(userService, never()).updateUserStatus(any(), any());
    }

    @Test
    void overviewCountsOnlyArchivedPeopleAndRegisteredRisk() {
        EmployeeArchiveDO active = new EmployeeArchiveDO();
        active.setEmploymentStatus(1);
        active.setProbationEndDate(LocalDate.now().plusDays(30));
        active.setSocialStatus(0);
        active.setContractEndDate(LocalDate.now().plusDays(3));
        EmployeeArchiveDO pending = new EmployeeArchiveDO();
        pending.setEmploymentStatus(0);
        when(mapper.selectList()).thenReturn(List.of(active, pending));

        var result = service.getOverview();

        assertEquals(2L, result.get("total"));
        assertEquals(1L, result.get("active"));
        assertEquals(1L, result.get("probation"));
        assertEquals(1L, result.get("attention"));
        assertEquals(1L, result.get("dueSoon"));
    }

    private EmployeeSaveReqVO request(Long userId) {
        EmployeeSaveReqVO request = new EmployeeSaveReqVO();
        request.setUserId(userId);
        request.setName("员工");
        request.setEmploymentStatus(0);
        request.setHireDate(LocalDate.now());
        return request;
    }
}
