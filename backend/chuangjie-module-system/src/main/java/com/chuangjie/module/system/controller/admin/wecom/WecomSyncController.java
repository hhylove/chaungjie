package com.chuangjie.module.system.controller.admin.wecom;

import com.chuangjie.framework.common.pojo.CommonResult;
import com.chuangjie.framework.common.util.object.BeanUtils;
import com.chuangjie.module.system.dal.dataobject.wecom.WecomDeptDO;
import com.chuangjie.module.system.dal.dataobject.wecom.WecomSyncRunDO;
import com.chuangjie.module.system.dal.dataobject.wecom.WecomUserDO;
import com.chuangjie.module.system.service.wecom.WecomSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static com.chuangjie.framework.common.pojo.CommonResult.success;

/** 管理员查看和手动同步当前租户的企业微信通讯录。 */
@Tag(name = "管理后台 - 企业微信通讯录同步")
@RestController
@RequestMapping("/system/wecom-sync")
public class WecomSyncController {

    @Resource private WecomSyncService service;

    @PostMapping("/execute")
    @Operation(summary = "手动同步企业微信部门和成员", description = "记录部门负责人及应用实际返回的员工手机、邮箱、性别；只更新企微镜像，不开通账号或授予角色")
    @PreAuthorize("@ss.hasPermission('system:wecom-sync:execute')")
    public CommonResult<RunRespVO> execute() {
        return success(BeanUtils.toBean(service.sync(), RunRespVO.class));
    }

    @PostMapping("/apply")
    @Operation(summary = "应用企微通讯录到系统组织和账号", description = "根据企微映射创建或更新部门和员工；更新姓名、所属部门及实际返回的员工资料，不覆盖角色、账号状态、账号名或密码")
    @PreAuthorize("@ss.hasPermission('system:wecom-sync:apply')")
    public CommonResult<WecomSyncService.ApplyResult> apply() {
        return success(service.apply());
    }

    @PutMapping("/link")
    @Operation(summary = "核对并关联企微员工与现有系统账号")
    @PreAuthorize("@ss.hasPermission('system:wecom-sync:link')")
    public CommonResult<Boolean> link(@Valid @RequestBody LinkReqVO request) {
        service.linkUser(request.getWecomUserId(), request.getSystemUserId());
        return success(true);
    }

    @PutMapping("/link-department")
    @Operation(summary = "核对并关联企微部门与现有系统部门", description = "只记录对应关系；应用时才更新系统部门")
    @PreAuthorize("@ss.hasPermission('system:wecom-sync:link')")
    public CommonResult<Boolean> linkDepartment(@Valid @RequestBody LinkDepartmentReqVO request) {
        service.linkDepartment(request.getWecomDeptId(), request.getSystemDeptId());
        return success(true);
    }
    @GetMapping("/latest")
    @Operation(summary = "最近一次成功的同步结果")
    @PreAuthorize("@ss.hasPermission('system:wecom-sync:query')")
    public CommonResult<RunRespVO> latest() {
        return success(BeanUtils.toBean(service.getLatestRun(), RunRespVO.class));
    }

    @GetMapping("/departments")
    @Operation(summary = "企微部门镜像列表", description = "leaderUserIds 可能含多个企微 userid，系统部门仅记录第一个已关联账号的负责人")
    @PreAuthorize("@ss.hasPermission('system:wecom-sync:query')")
    public CommonResult<List<DeptRespVO>> departments() {
        List<WecomDeptDO> rows = service.getDepartments();
        return success(BeanUtils.toBean(rows, DeptRespVO.class));
    }

    @GetMapping("/users")
    @Operation(summary = "企微成员镜像列表", description = "systemUserId 为空表示尚未关联系统账号；敏感字段只返回是否获取到，不返回原文")
    @PreAuthorize("@ss.hasPermission('system:wecom-sync:query')")
    public CommonResult<List<UserRespVO>> users() {
        List<WecomUserDO> rows = service.getUsers();
        return success(rows.stream().map(row -> {
            UserRespVO response = BeanUtils.toBean(row, UserRespVO.class);
            response.setHasMobile(row.getMobile() != null);
            response.setHasEmail(row.getEmail() != null);
            response.setHasSex(row.getSex() != null);
            return response;
        }).toList());
    }

    @Data
    @Schema(description = "企业微信同步结果")
    public static class RunRespVO {
        private Long id;
        private Integer departmentCount;
        private Integer userCount;
        private LocalDateTime createTime;
    }

    @Data
    @Schema(description = "企业微信部门镜像")
    public static class DeptRespVO {
        private Long wecomDeptId;
        private Long systemDeptId;
        private Long parentWecomDeptId;
        private String name;
        private String leaderUserIds;
        private Integer sort;
        private LocalDateTime lastSeenAt;
    }

    @Data
    @Schema(description = "企业微信成员镜像")
    public static class UserRespVO {
        private String wecomUserId;
        private String name;
        private String departmentIds;
        @Schema(description = "本次企微同步是否提供手机号，不返回手机号原文")
        private Boolean hasMobile;
        @Schema(description = "本次企微同步是否提供邮箱，不返回邮箱原文")
        private Boolean hasEmail;
        @Schema(description = "本次企微同步是否提供性别")
        private Boolean hasSex;
        private Long systemUserId;
        private LocalDateTime lastSeenAt;
    }
    @Data
    @Schema(description = "企微员工关联现有系统账号")
    public static class LinkReqVO {
        @NotBlank private String wecomUserId;
        @NotNull private Long systemUserId;
    }

    @Data
    @Schema(description = "企微部门关联现有系统部门")
    public static class LinkDepartmentReqVO {
        @NotNull private Long wecomDeptId;
        @NotNull private Long systemDeptId;
    }
}

