package com.chuangjie.module.hradmin.service.employee;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.common.util.object.BeanUtils;
import com.chuangjie.module.hradmin.controller.admin.employee.vo.EmployeePageReqVO;
import com.chuangjie.module.hradmin.controller.admin.employee.vo.EmployeeRespVO;
import com.chuangjie.module.hradmin.controller.admin.employee.vo.EmployeeSaveReqVO;
import com.chuangjie.module.hradmin.dal.dataobject.employee.EmployeeArchiveDO;
import com.chuangjie.module.hradmin.dal.dataobject.employee.EmployeeSalaryDO;
import com.chuangjie.module.hradmin.dal.dataobject.employee.OnboardingTaskDO;
import com.chuangjie.module.hradmin.dal.mysql.employee.EmployeeArchiveMapper;
import com.chuangjie.module.hradmin.dal.mysql.employee.EmployeeSalaryMapper;
import com.chuangjie.module.hradmin.dal.mysql.employee.OnboardingTaskMapper;
import com.chuangjie.framework.security.core.service.SecurityFrameworkService;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.chuangjie.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.chuangjie.module.hradmin.enums.ErrorCodeConstants.*;
import static com.chuangjie.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static com.chuangjie.framework.tenant.core.context.TenantContextHolder.getTenantId;

/** 人事档案业务；账号启停和角色变更统一由 System 服务办理。 */
@Service
public class EmployeeArchiveService {
    @Resource private EmployeeArchiveMapper mapper;
    @Resource private EmployeeSalaryMapper salaryMapper;
    @Resource private OnboardingTaskMapper onboardingMapper;
    @Resource private SecurityFrameworkService security;
    @Resource private AdminUserApi userApi;
    @Resource private AdminUserService userService;
    @Resource private WecomUserMapper wecomUserMapper;
    @Resource private DeptApi deptApi;

    @Transactional(rollbackFor = Exception.class)
    public Long create(EmployeeSaveReqVO request) {
        if (request.getUserId() != null || (request.getEmploymentStatus() != null && request.getEmploymentStatus() != 0)) {
            throw exception(EMPLOYEE_ARRIVAL_INVALID);
        }
        if (request.getHireDate() == null || request.getProbationEndDate() == null
                || request.getProbationEndDate().isBefore(request.getHireDate())
                || request.getContractEndDate() == null
                || !request.getContractEndDate().isAfter(request.getHireDate())) {
            throw exception(EMPLOYEE_ARRIVAL_INVALID);
        }
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
            recordSalary(row.getId(), request);
            createOnboarding(row);
        } catch (DuplicateKeyException e) {
            throw duplicateError(request, null);
        }
        return row.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createExisting(EmployeeSaveReqVO request) {
        if (request.getUserId() != null || request.getHireDate() == null
                || request.getHireDate().isAfter(LocalDate.now(ZoneId.of("Asia/Shanghai")))
                || request.getEmploymentStatus() == null || request.getEmploymentStatus() != 1) {
            throw exception(EMPLOYEE_ARRIVAL_INVALID);
        }
        request.setEmployeeNo("CJ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        normalize(request);
        validate(request, null);
        EmployeeArchiveDO row = BeanUtils.toBean(request, EmployeeArchiveDO.class);
        try {
            mapper.insert(row);
            recordSalary(row.getId(), request);
        } catch (DuplicateKeyException e) {
            throw duplicateError(request, null);
        }
        return row.getId();
    }

    private void recordSalary(Long employeeId, EmployeeSaveReqVO request) {
        if (request.getAgreedMonthlySalary() == null || request.getAgreedMonthlySalary().signum() <= 0
                || request.getAgreedMonthlySalary().scale() > 2
                || request.getAgreedMonthlySalary().compareTo(new java.math.BigDecimal("9999999999.99")) > 0) {
            throw exception(EMPLOYEE_SALARY_INVALID);
        }
        EmployeeSalaryDO salary = new EmployeeSalaryDO();
        salary.setEmployeeId(employeeId);
        salary.setAgreedMonthlySalary(request.getAgreedMonthlySalary());
        salary.setStatus(request.getEmploymentStatus() == 1 ? "已登记待核验" : "待审批");
        salaryMapper.insert(salary);
    }

    private record TaskSpec(int stageNo, String stage, String key, String title, String owner, int dueOffset) {}
    private static final List<TaskSpec> ONBOARDING = List.of(
            new TaskSpec(0,"入职前","info","身份与银行卡资料核验","人事专员",-3),
            new TaskSpec(0,"入职前","pay","岗位与薪酬审批","创业发展总监",-3),
            new TaskSpec(0,"入职前","draft","劳动合同准备","人事专员",-2),
            new TaskSpec(0,"入职前","desk","工位与电脑准备","人事专员",-1),
            new TaskSpec(0,"入职前","accessApply","OA / ERP / 业务权限申请","直属上级",-1),
            new TaskSpec(1,"入职当天","arrival","本人身份核验与到岗确认","人事专员",0),
            new TaskSpec(1,"入职当天","contract","劳动合同签署","员工本人",0),
            new TaskSpec(1,"入职当天","nda","保密及知识产权文件签署","员工本人",0),
            new TaskSpec(1,"入职当天","handbook","员工手册签收","员工本人",0),
            new TaskSpec(1,"入职当天","policy","公司制度签收","员工本人",0),
            new TaskSpec(1,"入职当天","job","岗位职责签收","员工本人",0),
            new TaskSpec(1,"入职当天","security","信息安全制度签收","员工本人",0),
            new TaskSpec(1,"入职当天","asset","办公资产领取","员工本人",0),
            new TaskSpec(1,"入职当天","access","系统权限开通","人事专员",0),
            new TaskSpec(2,"入职后","social","社保依法办理确认","人事专员",7),
            new TaskSpec(2,"入职后","training","新员工培训完成","员工本人",3),
            new TaskSpec(2,"入职后","jobTraining","岗位培训完成","员工本人",5),
            new TaskSpec(2,"入职后","manager","直属上级确认","直属上级",7));

    private void createOnboarding(EmployeeArchiveDO employee) {
        for (int index = 0; index < ONBOARDING.size(); index++) {
            TaskSpec spec = ONBOARDING.get(index);
            OnboardingTaskDO task = new OnboardingTaskDO();
            task.setEmployeeId(employee.getId()); task.setStageNo(spec.stageNo()); task.setSequenceNo(index);
            task.setStage(spec.stage()); task.setTaskKey(spec.key()); task.setTitle(spec.title());
            task.setOwner(spec.owner()); task.setDueDate(employee.getHireDate().plusDays(spec.dueOffset()));
            onboardingMapper.insert(task);
        }
    }

    public List<OnboardingTaskDO> getOnboarding(Long employeeId) {
        if (mapper.selectById(employeeId) == null) throw exception(EMPLOYEE_NOT_EXISTS);
        return onboardingMapper.byEmployee(employeeId);
    }

    public EmployeeRespVO getMyArchive() {
        EmployeeArchiveDO row = mapper.selectByUserId(getLoginUserId());
        if (row == null) throw exception(EMPLOYEE_NOT_EXISTS);
        return toResponse(row, userApi.getUser(row.getUserId()));
    }

    public List<OnboardingTaskDO> getMyOnboarding() {
        EmployeeArchiveDO row = mapper.selectByUserId(getLoginUserId());
        if (row == null) throw exception(EMPLOYEE_NOT_EXISTS);
        return onboardingMapper.byEmployee(row.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeOnboarding(Long employeeId, String taskKey, String evidence, Long userId, String socialStatus) {
        EmployeeArchiveDO employee = mapper.selectById(employeeId);
        if (employee == null) throw exception(EMPLOYEE_NOT_EXISTS);
        List<OnboardingTaskDO> tasks = onboardingMapper.byEmployee(employeeId);
        OnboardingTaskDO task = tasks.stream().filter(item -> item.getTaskKey().equals(taskKey)).findFirst().orElse(null);
        if (task == null || task.getDoneAt() != null || evidence == null || evidence.isBlank()
                || employee.getEmploymentStatus() == 3) throw exception(ONBOARDING_TASK_INVALID);
        if (tasks.stream().anyMatch(item -> item.getStageNo() < task.getStageNo() && item.getDoneAt() == null))
            throw exception(ONBOARDING_TASK_INVALID);
        String permission = switch (task.getOwner()) {
            case "人事专员" -> "hradmin:onboarding:hr";
            case "直属上级" -> "hradmin:onboarding:manager";
            case "创业发展总监" -> "hradmin:onboarding:director";
            default -> null;
        };
        if (permission == null ? employee.getUserId() == null || !Objects.equals(employee.getUserId(), getLoginUserId())
                : !security.hasPermission(permission)) throw exception(ONBOARDING_TASK_INVALID);
        if ("arrival".equals(taskKey)) {
            if (employee.getEmploymentStatus() != 0 || employee.getHireDate().isAfter(LocalDate.now(ZoneId.of("Asia/Shanghai"))))
                throw exception(EMPLOYEE_ARRIVAL_INVALID);
            employee.setEmploymentStatus(4);
            mapper.updateById(employee);
        } else if ("access".equals(taskKey)) {
            if ((employee.getEmploymentStatus() != 4 && employee.getEmploymentStatus() != 1) || tasks.stream().noneMatch(item -> "arrival".equals(item.getTaskKey()) && item.getDoneAt() != null))
                throw exception(ONBOARDING_TASK_INVALID);
            if (!security.hasPermission("system:user:update")) throw exception(ONBOARDING_TASK_INVALID);
            bindAndActivate(employee, userId, evidence);
        } else if (task.getStageNo() > 0 && employee.getEmploymentStatus() != 4 && employee.getEmploymentStatus() != 1) {
            throw exception(ONBOARDING_TASK_INVALID);
        }
        if ("social".equals(taskKey) && !"已参保".equals(socialStatus)) throw exception(ONBOARDING_TASK_INVALID);
        if ("pay".equals(taskKey)) {
            EmployeeSalaryDO salary = salaryMapper.selectByEmployeeId(employeeId);
            if (salary == null) throw exception(EMPLOYEE_SALARY_INVALID);
            salary.setStatus("已审批"); salaryMapper.updateById(salary);
        }
        if ("contract".equals(taskKey)) { employee.setContractStatus(1); mapper.updateById(employee); }
        if ("social".equals(taskKey)) { employee.setSocialStatus(1); mapper.updateById(employee); }
        if (onboardingMapper.completeIfPending(task.getId(), getTenantId(), LocalDateTime.now(),
                evidence.trim(), getLoginUserId()) != 1) throw exception(ONBOARDING_TASK_INVALID);
    }

    public EmployeeSalaryDO getSalary(Long employeeId) {
        if (mapper.selectById(employeeId) == null) throw exception(EMPLOYEE_NOT_EXISTS);
        return salaryMapper.selectByEmployeeId(employeeId);
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
        if (!Objects.equals(old.getEmploymentStatus(), request.getEmploymentStatus()) ||
                (old.getUserId() == null && request.getUserId() != null)) {
            throw exception(EMPLOYEE_ARRIVAL_INVALID);
        }
        if (!onboardingMapper.byEmployee(old.getId()).isEmpty()
                && (!Objects.equals(old.getHireDate(), request.getHireDate())
                    || !Objects.equals(old.getContractStatus(), request.getContractStatus())
                    || !Objects.equals(old.getSocialStatus(), request.getSocialStatus())
                    || !Objects.equals(old.getSocialReason(), request.getSocialReason()))) {
            throw exception(ONBOARDING_TASK_INVALID);
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
    public void activateExistingAccount(Long employeeId, Long userId, String evidence) {
        EmployeeArchiveDO employee = mapper.selectById(employeeId);
        if (employee == null) throw exception(EMPLOYEE_NOT_EXISTS);
        if (employee.getEmploymentStatus() != 1 || employee.getHireDate() == null
                || employee.getHireDate().isAfter(LocalDate.now(ZoneId.of("Asia/Shanghai")))
                || !onboardingMapper.byEmployee(employeeId).isEmpty()
                || evidence == null || evidence.isBlank()) throw exception(EMPLOYEE_ARRIVAL_INVALID);
        bindAndActivate(employee, userId, evidence);
    }

    private void bindAndActivate(EmployeeArchiveDO row, Long userId, String evidence) {
        if (userId == null || row.getUserId() != null) throw exception(EMPLOYEE_ARRIVAL_INVALID);
        AdminUserRespDTO user = userApi.getUser(userId);
        if (user == null) throw exception(EMPLOYEE_USER_NOT_EXISTS);
        if (user.getStatus() == null || user.getStatus() != 1) throw exception(EMPLOYEE_ACCOUNT_NOT_DISABLED);
        EmployeeArchiveDO bound = mapper.selectByUserId(userId);
        if (bound != null && !bound.getId().equals(row.getId())) throw exception(EMPLOYEE_USER_EXISTS);
        WecomUserDO wecomUser = wecomUserMapper.selectBySystemUserId(userId);
        if (wecomUser == null || !Objects.equals(wecomUser.getName(), row.getName())) {
            throw exception(EMPLOYEE_ACCOUNT_IDENTITY_MISMATCH);
        }
        row.setUserId(userId);
        row.setAccountActivatedAt(LocalDateTime.now());
        row.setAccountActivatedBy(getLoginUserId());
        row.setAccountActivationEvidence(evidence.trim());
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
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        Map<String, Long> result = new HashMap<>();
        result.put("total", (long) employees.size());
        result.put("active", employees.stream().filter(e -> e.getEmploymentStatus() != null && e.getEmploymentStatus() == 1).count());
        result.put("pending", employees.stream().filter(e -> e.getEmploymentStatus() != null && e.getEmploymentStatus() == 0).count());
        result.put("probation", employees.stream().filter(e -> e.getEmploymentStatus() != null && e.getEmploymentStatus() == 4).count());
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
                || request.getEmploymentStatus() < 0 || request.getEmploymentStatus() > 4) {
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
