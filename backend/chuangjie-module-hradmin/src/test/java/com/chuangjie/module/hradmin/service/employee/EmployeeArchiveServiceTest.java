package com.chuangjie.module.hradmin.service.employee;

import com.chuangjie.framework.common.exception.ServiceException;
import com.chuangjie.framework.security.core.service.SecurityFrameworkService;
import com.chuangjie.module.hradmin.controller.admin.employee.vo.EmployeeSaveReqVO;
import com.chuangjie.module.hradmin.dal.dataobject.employee.EmployeeArchiveDO;
import com.chuangjie.module.hradmin.dal.dataobject.employee.OnboardingTaskDO;
import com.chuangjie.module.hradmin.dal.mysql.employee.EmployeeArchiveMapper;
import com.chuangjie.module.hradmin.dal.mysql.employee.EmployeeSalaryMapper;
import com.chuangjie.module.hradmin.dal.mysql.employee.OnboardingTaskMapper;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.chuangjie.module.hradmin.enums.ErrorCodeConstants.EMPLOYEE_ARRIVAL_INVALID;
import static com.chuangjie.module.hradmin.enums.ErrorCodeConstants.EMPLOYEE_USER_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeArchiveServiceTest {
    @InjectMocks private EmployeeArchiveService service;
    @Mock private EmployeeArchiveMapper mapper;
    @Mock private EmployeeSalaryMapper salaryMapper;
    @Mock private OnboardingTaskMapper onboardingMapper;
    @Mock private SecurityFrameworkService security;
    @Mock private AdminUserApi userApi;
    @Mock private AdminUserService userService;
    @Mock private WecomUserMapper wecomUserMapper;
    @Mock private DeptApi deptApi;

    @Test
    void newArchiveGetsNumberAndOnboardingButNoAccount() {
        service.create(request());
        verify(mapper).insert(argThat((EmployeeArchiveDO row) -> row.getEmployeeNo().startsWith("CJ-")
                && row.getUserId() == null && row.getEmploymentStatus() == 0));
        verify(onboardingMapper, org.mockito.Mockito.times(18)).insert(any(OnboardingTaskDO.class));
        verify(userService, never()).updateUserStatus(any(), any());
    }

    @Test
    void existingEmployeeKeepsActualStatusWithoutOnboarding() {
        EmployeeSaveReqVO input = request();
        input.setEmploymentStatus(1);
        input.setHireDate(LocalDate.now().minusYears(1));
        service.createExisting(input);
        verify(mapper).insert(argThat((EmployeeArchiveDO row) -> row.getEmploymentStatus() == 1
                && row.getUserId() == null && row.getEmployeeNo().startsWith("CJ-")));
        verify(onboardingMapper, never()).insert(any(OnboardingTaskDO.class));
    }

    @Test
    void prehireTasksMustFinishBeforeArrivalAndAccount() {
        EmployeeArchiveDO employee = pending(LocalDate.now());
        when(mapper.selectById(11L)).thenReturn(employee);
        when(onboardingMapper.byEmployee(11L)).thenReturn(tasks(false));
        assertThrows(ServiceException.class, () -> service.completeOnboarding(11L, "arrival", "到岗证明", null, null));
        assertThrows(ServiceException.class, () -> service.completeOnboarding(11L, "access", "账号证明", 141L, null));
        verify(userService, never()).updateUserStatus(any(), any());
    }

    @Test
    void arrivalAndActivationAreSeparateAndRequireIdentity() {
        EmployeeArchiveDO employee = pending(LocalDate.now());
        when(mapper.selectById(11L)).thenReturn(employee);
        List<OnboardingTaskDO> taskList = tasks(true);
        when(onboardingMapper.byEmployee(11L)).thenReturn(taskList);
        when(security.hasPermission("hradmin:onboarding:hr")).thenReturn(true);
        when(security.hasPermission("system:user:update")).thenReturn(true);
        when(onboardingMapper.completeIfPending(isNull(), isNull(), any(), any(), isNull())).thenReturn(1);
        service.completeOnboarding(11L, "arrival", "本人身份核验凭证", null, null);
        assertEquals(4, employee.getEmploymentStatus());
        verify(userService, never()).updateUserStatus(any(), any());
        taskList.get(1).setDoneAt(LocalDateTime.now());
        AdminUserRespDTO user = new AdminUserRespDTO(); user.setId(141L); user.setStatus(1);
        when(userApi.getUser(141L)).thenReturn(user);
        WecomUserDO wecom = new WecomUserDO(); wecom.setName("员工");
        when(wecomUserMapper.selectBySystemUserId(141L)).thenReturn(wecom);
        service.completeOnboarding(11L, "access", "账号核验凭证", 141L, null);
        verify(userService).updateUserStatus(141L, 0);
        assertEquals(141L, employee.getUserId());
    }

    @Test
    void duplicateAccountIsRejected() {
        EmployeeArchiveDO employee = pending(LocalDate.now()); employee.setEmploymentStatus(1);
        List<OnboardingTaskDO> taskList = tasks(true); taskList.get(1).setDoneAt(LocalDateTime.now());
        when(mapper.selectById(11L)).thenReturn(employee);
        when(onboardingMapper.byEmployee(11L)).thenReturn(taskList);
        when(security.hasPermission("hradmin:onboarding:hr")).thenReturn(true);
        when(security.hasPermission("system:user:update")).thenReturn(true);
        AdminUserRespDTO user = new AdminUserRespDTO(); user.setId(141L); user.setStatus(1);
        when(userApi.getUser(141L)).thenReturn(user);
        EmployeeArchiveDO existing = new EmployeeArchiveDO(); existing.setId(10L);
        when(mapper.selectByUserId(141L)).thenReturn(existing);
        ServiceException error = assertThrows(ServiceException.class, () -> service.completeOnboarding(11L, "access", "账号核验", 141L, null));
        assertEquals(EMPLOYEE_USER_EXISTS.getCode(), error.getCode());
        verify(userService, never()).updateUserStatus(any(), any());
    }

    @Test
    void futureArrivalIsRejected() {
        EmployeeArchiveDO employee = pending(LocalDate.now().plusDays(1));
        when(mapper.selectById(11L)).thenReturn(employee);
        when(onboardingMapper.byEmployee(11L)).thenReturn(tasks(true));
        when(security.hasPermission("hradmin:onboarding:hr")).thenReturn(true);
        ServiceException error = assertThrows(ServiceException.class, () -> service.completeOnboarding(11L, "arrival", "到岗证明", null, null));
        assertEquals(EMPLOYEE_ARRIVAL_INVALID.getCode(), error.getCode());
    }

    @Test
    void editingArchiveCannotBypassArrival() {
        EmployeeArchiveDO employee = pending(LocalDate.now());
        when(mapper.selectById(11L)).thenReturn(employee);
        EmployeeSaveReqVO input = request(); input.setId(11L); input.setEmploymentStatus(1);
        assertThrows(ServiceException.class, () -> service.update(input));
        verify(mapper, never()).updateById(any(EmployeeArchiveDO.class));
    }

    private EmployeeSaveReqVO request() {
        EmployeeSaveReqVO input = new EmployeeSaveReqVO();
        input.setName("员工"); input.setEmploymentStatus(0); input.setHireDate(LocalDate.now());
        input.setProbationEndDate(LocalDate.now().plusDays(90));
        input.setContractEndDate(LocalDate.now().plusYears(2));
        input.setAgreedMonthlySalary(new BigDecimal("8000"));
        return input;
    }
    private EmployeeArchiveDO pending(LocalDate date) {
        EmployeeArchiveDO employee = new EmployeeArchiveDO();
        employee.setId(11L); employee.setName("员工"); employee.setEmploymentStatus(0); employee.setHireDate(date);
        return employee;
    }
    private List<OnboardingTaskDO> tasks(boolean prehireDone) {
        List<OnboardingTaskDO> out = new ArrayList<>();
        OnboardingTaskDO prehire = new OnboardingTaskDO();
        prehire.setStageNo(0); prehire.setSequenceNo(0); prehire.setTaskKey("info");
        if (prehireDone) prehire.setDoneAt(LocalDateTime.now());
        out.add(prehire);
        OnboardingTaskDO arrival = new OnboardingTaskDO();
        arrival.setStageNo(1); arrival.setSequenceNo(1); arrival.setTaskKey("arrival"); arrival.setOwner("人事专员");
        out.add(arrival);
        OnboardingTaskDO access = new OnboardingTaskDO();
        access.setStageNo(1); access.setSequenceNo(2); access.setTaskKey("access"); access.setOwner("人事专员");
        out.add(access);
        return out;
    }
}
