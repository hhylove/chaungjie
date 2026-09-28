package com.chuangjie.module.assistant.controller.admin;

import com.chuangjie.framework.common.pojo.CommonResult;
import com.chuangjie.module.assistant.service.AssistantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.chuangjie.framework.common.pojo.CommonResult.success;
import static com.chuangjie.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/assistant/chat")
@PreAuthorize("isAuthenticated()")
@Tag(name = "管理后台 - 员工智能助理对话")
public class AssistantChatController {

    private final AssistantService service;

    public AssistantChatController(AssistantService service) { this.service = service; }

    @Operation(summary = "查询当前员工是否可使用助理")
    @GetMapping("/availability")
    public CommonResult<Boolean> availability() { return success(service.available(getLoginUserId())); }

    @Operation(summary = "向内网模型提问", description = "仅限已开通员工；已有会话必须属于当前租户及本人")
    @PostMapping("/ask")
    public CommonResult<AssistantService.ChatAnswer> ask(@Valid @RequestBody AskRequest request) {
        return success(service.ask(getLoginUserId(), request.conversationId(), request.question()));
    }

    @Operation(summary = "查询本人最近会话", description = "最多返回 50 条")
    @GetMapping("/conversations")
    public CommonResult<List<Map<String, Object>>> conversations() {
        return success(service.myConversations(getLoginUserId()));
    }

    @Operation(summary = "查询本人会话消息", description = "最多返回最近 200 条，按时间正序")
    @GetMapping("/messages")
    public CommonResult<List<Map<String, Object>>> messages(@RequestParam long conversationId) {
        return success(service.myMessages(getLoginUserId(), conversationId));
    }

    @Schema(description = "管理后台 - 员工智能助理提问 Request VO")
    public record AskRequest(
            @Schema(description = "已有会话 ID；新会话不传") Long conversationId,
            @Schema(description = "问题内容，1 至 1000 字") @NotBlank @Size(max = 1000) String question) {}
}
