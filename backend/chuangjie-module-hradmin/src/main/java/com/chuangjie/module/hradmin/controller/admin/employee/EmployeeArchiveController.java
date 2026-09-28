package com.chuangjie.module.hradmin.controller.admin.employee;

import com.chuangjie.framework.common.pojo.CommonResult;
import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.module.hradmin.controller.admin.employee.vo.EmployeePageReqVO;
import com.chuangjie.module.hradmin.controller.admin.employee.vo.EmployeeRespVO;
import com.chuangjie.module.hradmin.controller.admin.employee.vo.EmployeeSaveReqVO;
import com.chuangjie.module.hradmin.service.employee.EmployeeArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
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
    @PreAuthorize("@ss.hasPermission('hradmin:employee:create')")
    public CommonResult<Long> create(@Valid @RequestBody EmployeeSaveReqVO request) {
        return success(service.create(request));
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

    @PostMapping("/confirm-arrival")
    @Operation(summary = "确认到岗并开通已预建的禁用账号")
    @PreAuthorize("@ss.hasPermission('hradmin:employee:update') and @ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> confirmArrival(@RequestParam @NotNull Long id, @RequestParam @NotNull Long userId) {
        service.confirmArrival(id, userId);
        return success(true);
    }
}
