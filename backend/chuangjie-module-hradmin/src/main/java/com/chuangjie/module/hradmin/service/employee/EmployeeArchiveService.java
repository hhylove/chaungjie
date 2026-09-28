package com.chuangjie.module.hradmin.service.employee;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.common.util.object.BeanUtils;
import com.chuangjie.module.hradmin.controller.admin.employee.vo.EmployeePageReqVO;
import com.chuangjie.module.hradmin.controller.admin.employee.vo.EmployeeRespVO;
import com.chuangjie.module.hradmin.controller.admin.employee.vo.EmployeeSaveReqVO;
import com.chuangjie.module.hradmin.dal.dataobject.employee.EmployeeArchiveDO;
import com.chuangjie.module.hradmin.dal.mysql.employee.EmployeeArchiveMapper;
import com.chuangjie.module.system.api.dept.DeptApi;
import com.chuangjie.module.system.api.user.AdminUserApi;
import com.chuangjie.module.system.api.user.dto.AdminUserRespDTO;
import com.chuangjie.module.system.dal.dataobject.wecom.WecomUserDO;
import com.chuangjie.module.system.dal.mysql.wecom.WecomUserMapper;
import com.chuangjie.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.chuangjie.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.chuangjie.module.hradmin.enums.ErrorCodeConstants.*;

/** 人事档案业务；账号启停和角色变更统一由 System 服务办理。 */
@Service
public class EmployeeArchiveService {
    @Resource private EmployeeArchiveMapper mapper;
    @Resource private AdminUserApi userApi;
    @Resource private AdminUserService userService;
    @Resource private WecomUserMapper wecomUserMapper;
    @Resource private DeptApi deptApi;

    @Transactional(rollbackFor = Exception.class)
    public Long create(EmployeeSaveReqVO request) {
        if (request.getUserId() != null || (request.getEmploymentStatus() != null && request.getEmploymentStatus() != 0)) {
            throw exception(EMPLOYEE_ARRIVAL_INVALID);
        }
        if (request.getHireDate() == null) throw exception(EMPLOYEE_ARRIVAL_INVALID);
        request.setEmployeeNo("CJ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        request.setEmploymentStatus(0);
        request.setContractStatus(null);
        request.setSocialStatus(null);
        request.setSocialReason(null);
        normalize(request);
        validate(request, null);
        EmployeeArchiveDO row = BeanUtils.toBean(request, EmployeeArchiveDO.class);
        try {
            mapper.insert(row);
        } catch (DuplicateKeyException e) {
            throw duplicateError(request, null);
        }
        return row.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(EmployeeSaveReqVO request) {
        EmployeeArchiveDO old = request.getId() == null ? null : mapper.selectById(request.getId());
        if (old == null) {
            throw exception(EMPLOYEE_NOT_EXISTS);
        }
        if (old.getUserId() != null && !old.getUserId().equals(request.getUserId())) {
            throw exception(EMPLOYEE_USER_LOCKED);
        }
        if (old.getEmploymentStatus() == 0 && (!Objects.equals(request.getEmploymentStatus(), 0) || request.getUserId() != null)) {
            throw exception(EMPLOYEE_ARRIVAL_INVALID);
        }
        request.setEmployeeNo(old.getEmployeeNo());
        normalize(request);
        validate(request, request.getId());
        EmployeeArchiveDO row = BeanUtils.toBean(request, EmployeeArchiveDO.class);
        try {
            mapper.updateById(row);
        } catch (DuplicateKeyException e) {
            throw duplicateError(request, request.getId());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmArrival(Long id, Long userId) {
        EmployeeArchiveDO row = mapper.selectById(id);
        if (row == null) throw exception(EMPLOYEE_NOT_EXISTS);
        if (row.getEmploymentStatus() != 0 || row.getHireDate() == null
                || row.getHireDate().isAfter(LocalDate.now(ZoneId.of("Asia/Shanghai")))) {
            throw exception(EMPLOYEE_ARRIVAL_INVALID);
        }
        AdminUserRespDTO user = userApi.getUser(userId);
        if (user == null) throw exception(EMPLOYEE_USER_NOT_EXISTS);
        if (user.getStatus() == null || user.getStatus() != 1) throw exception(EMPLOYEE_ACCOUNT_NOT_DISABLED);
        EmployeeArchiveDO bound = mapper.selectByUserId(userId);
        if (bound != null && !bound.getId().equals(id)) throw exception(EMPLOYEE_USER_EXISTS);
        WecomUserDO wecomUser = wecomUserMapper.selectBySystemUserId(userId);
        if (wecomUser == null || !Objects.equals(wecomUser.getName(), row.getName())) {
            throw exception(EMPLOYEE_ACCOUNT_IDENTITY_MISMATCH);
        }
        row.setUserId(userId);
        row.setEmploymentStatus(1);
        mapper.updateById(row);
        userService.updateUserStatus(userId, 0);
    }

    public EmployeeRespVO get(Long id) {
        EmployeeArchiveDO row = mapper.selectById(id);
        if (row == null) {
            throw exception(EMPLOYEE_NOT_EXISTS);
        }
        return toResponse(row, row.getUserId() == null ? null : userApi.getUser(row.getUserId()));
    }

    public PageResult<EmployeeRespVO> getPage(EmployeePageReqVO request) {
        PageResult<EmployeeArchiveDO> page = mapper.selectPage(request);
        Set<Long> userIds = page.getList().stream().map(EmployeeArchiveDO::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> users = userIds.isEmpty() ? Map.of() : userApi.getUserMap(userIds);
        return new PageResult<>(page.getList().stream()
                .map(row -> toResponse(row, users.get(row.getUserId()))).toList(), page.getTotal());
    }

    /** 人员工作台统计只基于已建档人员，避免把企微账号误算为在职员工。 */
    public Map<String, Long> getOverview() {
        // ponytail: 当前人员规模小；超过千人时改用数据库聚合查询。
        var employees = mapper.selectList();
        LocalDate today = LocalDate.now();
        Map<String, Long> result = new HashMap<>();
        result.put("total", (long) employees.size());
        result.put("active", employees.stream().filter(e -> e.getEmploymentStatus() != null && e.getEmploymentStatus() == 1).count());
        result.put("pending", employees.stream().filter(e -> e.getEmploymentStatus() != null && e.getEmploymentStatus() == 0).count());
        result.put("probation", employees.stream().filter(e -> e.getEmploymentStatus() != null && e.getEmploymentStatus() == 1
                && e.getProbationEndDate() != null && !e.getProbationEndDate().isBefore(today)).count());
        result.put("attention", employees.stream().filter(e -> needsAttention(e, today)).count());
        result.put("dueSoon", employees.stream().filter(e -> e.getEmploymentStatus() != null && e.getEmploymentStatus() != 3
                && e.getContractEndDate() != null && !e.getContractEndDate().isBefore(today)
                && !e.getContractEndDate().isAfter(today.plusDays(7))).count());
        return result;
    }

    private boolean needsAttention(EmployeeArchiveDO employee, LocalDate today) {
        if (employee.getEmploymentStatus() == null || employee.getEmploymentStatus() == 3) return false;
        return (employee.getContractEndDate() != null && employee.getContractEndDate().isBefore(today))
                || (employee.getSocialStatus() != null && employee.getSocialStatus() == 0);
    }

    private void validate(EmployeeSaveReqVO request, Long currentId) {
        if (request.getEmploymentStatus() == null
                || request.getEmploymentStatus() < 0 || request.getEmploymentStatus() > 3) {
            throw exception(EMPLOYEE_STATUS_INVALID);
        }
        if (request.getDeptId() != null && deptApi.getDept(request.getDeptId()) == null) {
            throw exception(EMPLOYEE_DEPT_NOT_EXISTS);
        }
        if (request.getManagerUserId() != null && userApi.getUser(request.getManagerUserId()) == null) {
            throw exception(EMPLOYEE_MANAGER_NOT_EXISTS);
        }
        EmployeeArchiveDO byNo = mapper.selectByEmployeeNo(request.getEmployeeNo());
        if (byNo != null && !byNo.getId().equals(currentId)) {
            throw exception(EMPLOYEE_NO_EXISTS);
        }
        if (request.getUserId() != null) {
            if (userApi.getUser(request.getUserId()) == null) {
                throw exception(EMPLOYEE_USER_NOT_EXISTS);
            }
            EmployeeArchiveDO byUser = mapper.selectByUserId(request.getUserId());
            if (byUser != null && !byUser.getId().equals(currentId)) {
                throw exception(EMPLOYEE_USER_EXISTS);
            }
        }
    }

    private void normalize(EmployeeSaveReqVO request) {
        if (request.getEmployeeNo() != null) {
            request.setEmployeeNo(request.getEmployeeNo().trim());
        }
        if (request.getName() != null) {
            request.setName(request.getName().trim());
        }
        if (request.getUserId() != null) {
            // 已有关联账号时，部门以企微同步后的 System 账号为准。
            request.setDeptId(null);
        }
    }

    private RuntimeException duplicateError(EmployeeSaveReqVO request, Long currentId) {
        EmployeeArchiveDO byUser = request.getUserId() == null ? null : mapper.selectByUserId(request.getUserId());
        return exception(byUser != null && !byUser.getId().equals(currentId)
                ? EMPLOYEE_USER_EXISTS : EMPLOYEE_NO_EXISTS);
    }

    private EmployeeRespVO toResponse(EmployeeArchiveDO row, AdminUserRespDTO user) {
        EmployeeRespVO response = BeanUtils.toBean(row, EmployeeRespVO.class);
        if (user != null) {
            response.setAccountName(user.getNickname());
            response.setAccountDeptId(user.getDeptId());
            response.setAccountStatus(user.getStatus());
        }
        return response;
    }
}
