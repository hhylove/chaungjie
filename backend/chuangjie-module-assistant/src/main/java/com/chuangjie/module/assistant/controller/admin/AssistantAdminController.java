package com.chuangjie.module.assistant.controller.admin;

import com.chuangjie.framework.common.pojo.CommonResult;
import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.module.assistant.dal.AssistantRepository;
import com.chuangjie.module.assistant.service.AssistantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.chuangjie.framework.common.pojo.CommonResult.success;
import static com.chuangjie.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/assistant/admin")
@Validated
@PreAuthorize("@ss.hasAnyRoles('tenant_admin','super_admin')")
@Tag(name = "管理后台 - 员工智能助理配置")
public class AssistantAdminController {

    private final AssistantService service;

    public AssistantAdminController(AssistantService service) { this.service = service; }

    @Operation(summary = "获取当前租户助理总览")
    @GetMapping("/overview")
    public CommonResult<Map<String, Object>> overview() { return success(service.overview()); }

    @Operation(summary = "获取当前租户助理配置")
    @GetMapping("/config")
    public CommonResult<AssistantRepository.Config> config() { return success(service.getConfig()); }

    @Operation(summary = "保存当前租户助理配置", description = "模型地址仅支持内网 HTTP；开启前必须配置模型地址和名称")
    @PutMapping("/config")
    public CommonResult<Boolean> saveConfig(@Valid @RequestBody ConfigRequest request) {
        service.saveConfig(request.enabled(), request.modelBaseUrl(), request.modelName(),
                request.retentionDays(), request.requestsPerMinute(), getLoginUserId());
        return success(true);
    }

    @Operation(summary = "测试当前租户内网模型连接")
    @PostMapping("/model/test")
    public CommonResult<String> testModel() { return success(service.testModel(getLoginUserId())); }

    @Operation(summary = "分页查询当前租户员工及助理开通状态")
    @GetMapping("/users")
    public CommonResult<PageResult<AssistantService.UserGrant>> users(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Long deptId) {
        return success(service.pageUsers(pageNo, pageSize, username, deptId));
    }

    @Operation(summary = "批量开通或关闭员工助理", description = "每次最多配置 100 位当前租户员工")
    @PutMapping("/users/grant")
    public CommonResult<Boolean> setGrants(@Valid @RequestBody GrantRequest request) {
        service.setGrants(request.userIds(), request.enabled(), getLoginUserId());
        return success(true);
    }

    @Operation(summary = "查询当前租户助理审计", description = "仅返回操作元数据，不包含提问和回答正文")
    @GetMapping("/audit")
    public CommonResult<List<Map<String, Object>>> audit(@RequestParam(defaultValue = "50") int limit) {
        return success(service.audit(limit));
    }

    @Schema(description = "管理后台 - 保存当前租户助理配置 Request VO")
    public record ConfigRequest(
            @Schema(description = "是否开启当前租户助理") boolean enabled,
            @Schema(description = "内网模型服务根地址，如 http://192.168.1.10:8000/v1") String modelBaseUrl,
            @Schema(description = "模型标识") String modelName,
            @Schema(description = "会话及审计保存天数，1 至 365") @Min(1) @Max(365) int retentionDays,
            @Schema(description = "每位员工每分钟最大提问次数，1 至 120") @Min(1) @Max(120) int requestsPerMinute) {}

    @Schema(description = "管理后台 - 批量开通或关闭员工助理 Request VO")
    public record GrantRequest(
            @Schema(description = "当前租户的员工账号 ID，每次 1 至 100 个") @NotEmpty @Size(max = 100) List<Long> userIds,
            @Schema(description = "true 开通，false 关闭") boolean enabled) {}
}
