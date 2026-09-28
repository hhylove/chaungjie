package com.chuangjie.module.system.service.wecom;

import com.chuangjie.module.system.controller.admin.dept.vo.dept.DeptSaveReqVO;
import com.chuangjie.module.system.controller.admin.user.vo.profile.UserProfileUpdateReqVO;
import com.chuangjie.module.system.dal.dataobject.dept.DeptDO;
import com.chuangjie.module.system.dal.dataobject.user.AdminUserDO;
import com.chuangjie.module.system.dal.dataobject.wecom.WecomDeptDO;
import com.chuangjie.module.system.dal.dataobject.wecom.WecomSyncRunDO;
import com.chuangjie.module.system.dal.dataobject.wecom.WecomUserDO;
import com.chuangjie.module.system.dal.mysql.wecom.WecomDeptMapper;
import com.chuangjie.module.system.dal.mysql.wecom.WecomSyncRunMapper;
import com.chuangjie.module.system.dal.mysql.wecom.WecomUserMapper;
import com.chuangjie.module.system.dal.mysql.user.AdminUserMapper;
import com.chuangjie.module.system.service.dept.DeptService;
import com.chuangjie.module.system.service.permission.PermissionService;
import com.chuangjie.module.system.service.permission.RoleService;
import com.chuangjie.module.system.service.user.AdminUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WecomSyncServiceTest {
    @InjectMocks private WecomSyncService service;
    @Mock private WecomDeptMapper wecomDeptMapper;
    @Mock private WecomUserMapper wecomUserMapper;
    @Mock private WecomSyncRunMapper wecomSyncRunMapper;
    @Mock private DeptService deptService;
    @Mock private AdminUserService adminUserService;
    @Mock private AdminUserMapper adminUserMapper;
    @Mock private RoleService roleService;
    @Mock private PermissionService permissionService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void applyUpdatesLeaderAndReturnedProfileWithoutCreatingAccounts() {
        LocalDateTime now = LocalDateTime.now();
        WecomDeptDO wecomDept = new WecomDeptDO().setId(1L).setWecomDeptId(10L)
                .setSystemDeptId(100L).setParentWecomDeptId(0L).setName("销售部")
                .setSort(1).setLeaderUserIds("[\"leader\"]").setLastSeenAt(now);
        WecomUserDO leader = new WecomUserDO().setId(2L).setWecomUserId("leader").setName("张三")
                .setSystemUserId(141L).setDepartmentIds("[10]")
                .setMobile("13800000000").setEmail("leader@example.com")
                .setSex(1).setLastSeenAt(now);
        DeptDO systemDept = new DeptDO().setId(100L).setName("销售部")
                .setParentId(0L).setSort(1).setStatus(0);
        when(wecomSyncRunMapper.selectLatest()).thenReturn(new WecomSyncRunDO().setDepartmentCount(1));
        when(wecomDeptMapper.selectList()).thenReturn(List.of(wecomDept));
        when(wecomUserMapper.selectList()).thenReturn(List.of(leader));
        when(deptService.getDept(100L)).thenReturn(systemDept);
        when(adminUserService.getUser(141L)).thenReturn(new AdminUserDO().setId(141L)
                .setDeptId(999L).setNickname("旧名"));

        WecomSyncService.ApplyResult result = service.apply();

        assertEquals(0, result.createdDepartments());
        assertEquals(0, result.createdUsers());
        assertEquals(1, result.updatedDepartments());
        assertEquals(1, result.updatedUsers());
        ArgumentCaptor<UserProfileUpdateReqVO> profile = ArgumentCaptor.forClass(UserProfileUpdateReqVO.class);
        verify(adminUserService).updateUserProfile(org.mockito.ArgumentMatchers.eq(141L), profile.capture());
        assertEquals("13800000000", profile.getValue().getMobile());
        assertEquals("leader@example.com", profile.getValue().getEmail());
        assertEquals(1, profile.getValue().getSex());
        assertEquals("张三", profile.getValue().getNickname());
        ArgumentCaptor<AdminUserDO> account = ArgumentCaptor.forClass(AdminUserDO.class);
        verify(adminUserMapper).updateById(account.capture());
        assertEquals(100L, account.getValue().getDeptId());
        ArgumentCaptor<DeptSaveReqVO> department = ArgumentCaptor.forClass(DeptSaveReqVO.class);
        verify(deptService).updateDept(department.capture());
        assertEquals(141L, department.getValue().getLeaderUserId());
    }

    @Test
    void applyDoesNotEraseProfileWhenWecomOmitsSensitiveFields() {
        LocalDateTime now = LocalDateTime.now();
        WecomDeptDO dept = new WecomDeptDO().setId(1L).setWecomDeptId(10L)
                .setSystemDeptId(100L).setParentWecomDeptId(0L).setName("销售部")
                .setSort(1).setLeaderUserIds("[]").setLastSeenAt(now);
        WecomUserDO member = new WecomUserDO().setId(2L).setWecomUserId("member")
                .setSystemUserId(141L).setDepartmentIds("[10]").setLastSeenAt(now);
        when(wecomSyncRunMapper.selectLatest()).thenReturn(new WecomSyncRunDO().setDepartmentCount(1));
        when(wecomDeptMapper.selectList()).thenReturn(List.of(dept));
        when(wecomUserMapper.selectList()).thenReturn(List.of(member));
        when(deptService.getDept(100L)).thenReturn(new DeptDO().setId(100L).setName("销售部")
                .setParentId(0L).setSort(1).setStatus(0));
        when(adminUserService.getUser(141L)).thenReturn(new AdminUserDO().setId(141L).setDeptId(100L));

        service.apply();

        verify(adminUserService, never()).updateUserProfile(any(), any());
    }

    @Test
    void linkDepartmentUsesExplicitSystemDepartmentId() {
        when(wecomDeptMapper.selectByWecomDeptId(10L))
                .thenReturn(new WecomDeptDO().setId(1L).setWecomDeptId(10L));
        when(deptService.getDept(100L)).thenReturn(new DeptDO().setId(100L));

        service.linkDepartment(10L, 100L);

        ArgumentCaptor<WecomDeptDO> mapping = ArgumentCaptor.forClass(WecomDeptDO.class);
        verify(wecomDeptMapper).updateById(mapping.capture());
        assertEquals(1L, mapping.getValue().getId());
        assertEquals(100L, mapping.getValue().getSystemDeptId());
    }
}
