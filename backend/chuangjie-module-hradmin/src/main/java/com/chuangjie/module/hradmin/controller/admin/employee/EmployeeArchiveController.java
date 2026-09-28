package com.chuangjie.module.hradmin.controller.admin.employee;

import com.chuangjie.framework.common.pojo.CommonResult;
import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.module.hradmin.controller.admin.employee.vo.EmployeePageReqVO;
import com.chuangjie.module.hradmin.controller.admin.employee.vo.EmployeeRespVO;
import com.chuangjie.module.hradmin.controller.admin.employee.vo.EmployeeSaveReqVO;
import com.chuangjie.module.hradmin.service.employee.EmployeeArchiveService;
import com.chuangjie.module.hradmin.dal.dataobject.employee.EmployeeSalaryDO;
import com.chuangjie.module.hradmin.dal.dataobject.employee.OnboardingTaskDO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.chuangjie.framework.common.pojo.CommonResult.success;

/** 一期人员档案仅供授权管理员办理。 */
@Tag(name = "管理后台 - 人事行政 - 人员档案")
@RestController
@RequestMapping("/hradmin/employee")
public class EmployeeArchiveController {
    @Resource private EmployeeArchiveService service;

    @PostMapping("/create")
    @Operation(summary = "创建人员档案", description = "建档不自动启用账号或授予角色")
    @PreAuthorize("@ss.hasPermission('hradmin:employee:create') and @ss.hasPermission('hradmin:salary:write')")
    public CommonResult<Long> create(@Valid @RequestBody EmployeeSaveReqVO request) {
        return success(service.create(request));
    }

    @PostMapping("/create-existing")
    @Operation(summary = "录入原有在职人员", description = "记录真实任职事实，不补造招聘和入职流程，也不启用账号")
    @PreAuthorize("@ss.hasPermission('hradmin:employee:create') and @ss.hasPermission('hradmin:salary:write')")
    public CommonResult<Long> createExisting(@Valid @RequestBody EmployeeSaveReqVO request) {
        return success(service.createExisting(request));
    }

    @PutMapping("/update")
    @Operation(summary = "更新人员档案", description = "不修改企微镜像、系统账号或角色")
    @PreAuthorize("@ss.hasPermission('hradmin:employee:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody EmployeeSaveReqVO request) {
        service.update(request);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取人员档案详情")
    @PreAuthorize("@ss.hasPermission('hradmin:employee:query')")
    public CommonResult<EmployeeRespVO> get(@RequestParam @NotNull Long id) {
        return success(service.get(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获取人员档案分页")
    @PreAuthorize("@ss.hasPermission('hradmin:employee:query')")
    public CommonResult<PageResult<EmployeeRespVO>> page(@Valid EmployeePageReqVO request) {
        return success(service.getPage(request));
    }

    @GetMapping("/overview")
    @Operation(summary = "获取人员工作台统计", description = "按真实档案统计人员、待入职、试用期和合同社保风险")
    @PreAuthorize("@ss.hasPermission('hradmin:employee:query')")
    public CommonResult<Map<String, Long>> overview() {
        return success(service.getOverview());
    }

    @GetMapping("/salary")
    @PreAuthorize("@ss.hasPermission('hradmin:salary:read')")
    public CommonResult<EmployeeSalaryDO> salary(@RequestParam @NotNull Long employeeId) {
        return success(service.getSalary(employeeId));
    }

    public record CompleteOnboardingInput(@NotNull Long employeeId, @NotNull String taskKey,
                                          @NotNull String evidence, Long userId, String socialStatus) {}
    public record ActivateExistingInput(@NotNull Long employeeId, @NotNull Long userId, @NotNull String evidence) {}

    @PostMapping("/activate-existing")
    @PreAuthorize("@ss.hasPermission('hradmin:employee:update') and @ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> activateExisting(@Valid @RequestBody ActivateExistingInput input) {
        service.activateExistingAccount(input.employeeId(), input.userId(), input.evidence());
        return success(true);
    }

    @GetMapping("/onboarding")
    @PreAuthorize("@ss.hasPermission('hradmin:employee:query')")
    public CommonResult<List<OnboardingTaskDO>> onboarding(@RequestParam @NotNull Long employeeId) {
        return success(service.getOnboarding(employeeId));
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<EmployeeRespVO> mine() { return success(service.getMyArchive()); }

    @GetMapping("/onboarding/mine")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<List<OnboardingTaskDO>> myOnboarding() { return success(service.getMyOnboarding()); }

    @PostMapping("/onboarding/complete")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> completeOnboarding(@Valid @RequestBody CompleteOnboardingInput input) {
        service.completeOnboarding(input.employeeId(), input.taskKey(), input.evidence(), input.userId(), input.socialStatus());
        return success(true);
    }
}
