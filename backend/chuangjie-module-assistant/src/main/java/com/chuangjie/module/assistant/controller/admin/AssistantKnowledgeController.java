package com.chuangjie.module.assistant.controller.admin;

import com.chuangjie.framework.common.pojo.CommonResult;
import com.chuangjie.module.assistant.dal.KnowledgeRepository;
import com.chuangjie.module.assistant.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.chuangjie.framework.common.pojo.CommonResult.success;
import static com.chuangjie.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/** 租户管理员维护独立知识库；员工端不暴露文档正文或授权配置。 */
@RestController
@RequestMapping("/assistant/admin/knowledge")
@PreAuthorize("@ss.hasAnyRoles('tenant_admin','super_admin')")
@Validated
@Tag(name = "管理后台 - 员工智能助理独立知识库")
public class AssistantKnowledgeController {
    private final KnowledgeService service;

    public AssistantKnowledgeController(KnowledgeService service) { this.service = service; }

    @GetMapping("/embedding-config")
    @Operation(summary = "获取当前租户内网向量模型配置")
    public CommonResult<KnowledgeRepository.EmbeddingConfig> embeddingConfig() { return success(service.getEmbeddingConfig()); }

    @PutMapping("/embedding-config")
    @Operation(summary = "保存当前租户内网向量模型配置")
    public CommonResult<Boolean> saveEmbeddingConfig(@Valid @RequestBody EmbeddingRequest request) {
        service.saveEmbeddingConfig(request.baseUrl(), request.modelName());
        return success(true);
    }

    @PostMapping("/embedding-test")
    @Operation(summary = "测试向量模型连接")
    public CommonResult<String> testEmbedding() { return success(service.testEmbedding()); }

    @GetMapping("/bases")
    @Operation(summary = "查询当前租户知识库列表")
    public CommonResult<List<Map<String, Object>>> bases() { return success(service.bases()); }

    @PostMapping("/bases")
    @Operation(summary = "创建知识库")
    public CommonResult<Long> createBase(@Valid @RequestBody BaseRequest request) {
        return success(service.createBase(getLoginUserId(), request.name(), request.description(), request.visibility()));
    }

    @PutMapping("/bases/{id}")
    @Operation(summary = "更新知识库或停用知识库")
    public CommonResult<Boolean> updateBase(@PathVariable long id, @Valid @RequestBody BaseRequest request) {
        service.updateBase(id, request.name(), request.description(), request.visibility(), request.enabledOrDefault());
        return success(true);
    }

    @GetMapping("/bases/{id}/grants")
    @Operation(summary = "查询知识库员工与部门授权")
    public CommonResult<List<Map<String, Object>>> grants(@PathVariable long id) { return success(service.grants(id)); }

    @PutMapping("/bases/{id}/grants")
    @Operation(summary = "替换知识库员工与部门授权", description = "授权后仍受租户、账号状态及助理开通状态约束")
    public CommonResult<Boolean> replaceGrants(@PathVariable long id, @Valid @RequestBody GrantRequest request) {
        service.replaceGrants(id, request.userIds(), request.deptIds(), request.roleIds());
        return success(true);
    }

    @GetMapping("/bases/{id}/documents")
    @Operation(summary = "查询知识库文档列表")
    public CommonResult<List<Map<String, Object>>> documents(@PathVariable long id) { return success(service.documents(id)); }

    @PostMapping("/bases/{id}/documents")
    @Operation(summary = "上传或更新文档", description = "支持 TXT、Markdown、PDF、DOCX；更新时传 documentId")
    public CommonResult<Long> upload(@PathVariable long id, @RequestParam(required = false) Long documentId,
                                     @RequestPart MultipartFile file) throws IOException {
        return success(service.upload(id, documentId, file));
    }

    @PutMapping("/bases/{id}/documents/{documentId}/enabled")
    @Operation(summary = "启用或停用文档")
    public CommonResult<Boolean> setDocumentEnabled(@PathVariable long id, @PathVariable long documentId,
                                                     @RequestParam boolean enabled) {
        service.setDocumentEnabled(id, documentId, enabled);
        return success(true);
    }

    @DeleteMapping("/bases/{id}/documents/{documentId}")
    @Operation(summary = "删除文档及其知识索引")
    public CommonResult<Boolean> deleteDocument(@PathVariable long id, @PathVariable long documentId) {
        service.deleteDocument(id, documentId);
        return success(true);
    }

    @Schema(description = "内网向量模型配置")
    public record EmbeddingRequest(@Schema(description = "OpenAI 兼容 /v1 根地址") @NotBlank String baseUrl,
                                   @Schema(description = "向量模型名称") @NotBlank String modelName) {}

    @Schema(description = "知识库配置")
    public record BaseRequest(@Schema(description = "名称") @NotBlank String name,
                              @Schema(description = "用途说明") String description,
                              @Schema(description = "all 全租户可见；restricted 按授权可见") @NotBlank String visibility,
                              @Schema(description = "是否启用，新建时默认 true") Boolean enabled) {
        public boolean enabledOrDefault() { return enabled == null || enabled; }
    }

    @Schema(description = "知识库授权清单")
    public record GrantRequest(@Schema(description = "员工账号 ID") @NotNull @Size(max = 100) List<Long> userIds,
                               @Schema(description = "部门 ID") @NotNull @Size(max = 100) List<Long> deptIds,
                               @Schema(description = "系统角色 ID，作为授权群组") @NotNull @Size(max = 100) List<Long> roleIds) {}
}
