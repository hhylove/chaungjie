package com.chuangjie.module.assistant.service;

import com.chuangjie.framework.common.enums.CommonStatusEnum;
import com.chuangjie.framework.common.exception.ServiceException;
import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.tenant.core.context.TenantContextHolder;
import com.chuangjie.module.assistant.dal.AssistantRepository;
import com.chuangjie.module.system.controller.admin.user.vo.user.UserPageReqVO;
import com.chuangjie.module.system.dal.dataobject.user.AdminUserDO;
import com.chuangjie.module.system.service.user.AdminUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 员工助理业务入口：所有配置、开通与会话均以当前登录租户为边界。 */
@Service
@Slf4j
public class AssistantService {

    private final AssistantRepository repository;
    private final LocalModelClient model;
    private final AdminUserService users;
    private final KnowledgeService knowledge;

    public AssistantService(AssistantRepository repository, LocalModelClient model, AdminUserService users,
                            KnowledgeService knowledge) {
        this.repository = repository;
        this.model = model;
        this.users = users;
        this.knowledge = knowledge;
    }

    private long tenantId() { return TenantContextHolder.getRequiredTenantId(); }

    public AssistantRepository.Config getConfig() { return repository.getConfig(tenantId()); }

    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(boolean enabled, String baseUrl, String modelName, int retentionDays, int perMinute,
                           long operatorId) {
        if (retentionDays < 1 || retentionDays > 365 || perMinute < 1 || perMinute > 120) {
            throw error(1, "保存期限或请求上限超出范围");
        }
        if ((baseUrl != null && baseUrl.length() > 255) || (modelName != null && modelName.length() > 128)) {
            throw error(1, "模型配置长度超出范围");
        }
        if (baseUrl != null && !baseUrl.isBlank()) {
            try { model.validateEndpoint(baseUrl); }
            catch (IllegalArgumentException ex) { throw error(1, ex.getMessage()); }
        }
        if (enabled && (baseUrl == null || baseUrl.isBlank() || modelName == null || modelName.isBlank())) {
            throw error(1, "启用助理前必须配置内网模型地址和名称");
        }
        repository.saveConfig(new AssistantRepository.Config(tenantId(), enabled,
                baseUrl == null || baseUrl.isBlank() ? null : baseUrl.trim(),
                modelName == null || modelName.isBlank() ? null : modelName.trim(), retentionDays, perMinute));
        repository.audit(tenantId(), operatorId, "config_update", "success", null, null);
    }

    public String testModel(long operatorId) {
        AssistantRepository.Config config = getConfig();
        long started = System.currentTimeMillis();
        try {
            String answer = model.test(config);
            repository.audit(tenantId(), operatorId, "model_test", "success", config.modelName(),
                    System.currentTimeMillis()-started);
            return answer;
        } catch (RuntimeException ex) {
            repository.audit(tenantId(), operatorId, "model_test", "failed", config.modelName(),
                    System.currentTimeMillis()-started);
            log.warn("[testModel] tenantId={} model connection failed", tenantId(), ex);
            throw error(6, "内网模型连接失败，请检查地址和模型服务");
        }
    }

    public boolean available(Long userId) {
        // 悬浮窗状态只是展示；每次问答和历史读取仍必须重新执行此服务端校验。
        if (userId == null) return false;
        long tenantId = tenantId();
        AssistantRepository.Config config = repository.getConfig(tenantId);
        AdminUserDO user = users.getUser(userId);
        return config.enabled() && repository.hasGrant(tenantId, userId) && user != null &&
                Objects.equals(user.getTenantId(), tenantId) &&
                CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus());
    }

    public record UserGrant(long id, String username, String nickname, Long deptId, Integer status, boolean enabled) {}

    public PageResult<UserGrant> pageUsers(int pageNo, int pageSize, String username, Long deptId) {
        if (pageNo < 1 || pageSize < 1 || pageSize > 100) throw error(1, "分页参数无效");
        UserPageReqVO request = new UserPageReqVO();
        request.setPageNo(pageNo);
        request.setPageSize(pageSize);
        request.setUsername(username);
        request.setDeptId(deptId);
        PageResult<AdminUserDO> page = users.getUserPage(request);
        List<Long> ids = page.getList().stream().map(AdminUserDO::getId).toList();
        Map<Long, Boolean> grants = repository.getGrants(tenantId(), ids);
        List<UserGrant> result = page.getList().stream().map(user -> new UserGrant(user.getId(),
                user.getUsername(), user.getNickname(), user.getDeptId(), user.getStatus(),
                grants.getOrDefault(user.getId(), false))).toList();
        return new PageResult<>(result, page.getTotal());
    }

    @Transactional(rollbackFor = Exception.class)
    public void setGrants(List<Long> userIds, boolean enabled, long operatorId) {
        if (userIds == null || userIds.isEmpty() || userIds.size() > 100 || userIds.stream().anyMatch(Objects::isNull)) {
            throw error(1, "一次只能配置 1 至 100 个员工");
        }
        List<Long> ids = userIds.stream().distinct().toList();
        // 批量配置前验证所有账号归属，避免跨租户用户 ID 被传入后写入授权表。
        List<AdminUserDO> selected = users.getUserList(ids);
        long tenantId = tenantId();
        if (selected.size() != ids.size() || selected.stream().anyMatch(user ->
                !Objects.equals(user.getTenantId(), tenantId))) {
            throw error(2, "员工不存在或不属于当前租户");
        }
        for (Long id : ids) repository.setGrant(tenantId, id, enabled, operatorId);
        repository.audit(tenantId, operatorId, enabled ? "user_enable" : "user_disable", "success", null, null);
    }

    public record ChatAnswer(long conversationId, String answer) {}

    public ChatAnswer ask(long userId, Long conversationId, String question) {
        long tenantId = tenantId();
        if (!available(userId)) {
            repository.audit(tenantId, userId, "ask", "not_enabled", null, null);
            throw error(3, "当前员工未开通助理");
        }
        if (question == null || question.isBlank() || question.length() > 1000) {
            throw error(1, "问题长度必须为 1 至 1000 字");
        }
        AssistantRepository.Config config = repository.getConfig(tenantId);
        if (repository.reserveRequest(tenantId, userId) > config.requestsPerMinute()) {
            repository.audit(tenantId, userId, "ask", "rate_limited", config.modelName(), null);
            throw error(4, "提问过于频繁，请稍后重试");
        }
        // 会话 ID 由客户端提交，不能仅凭 ID 读取历史；必须同时匹配租户与本人。
        if (conversationId != null && !repository.ownsConversation(tenantId, userId, conversationId)) {
            repository.audit(tenantId, userId, "ask", "forbidden_conversation", config.modelName(), null);
            throw error(5, "会话不存在或无权访问");
        }
        List<AssistantRepository.Message> history = conversationId == null ? List.of() :
                visibleHistory(userId, repository.recentMessages(tenantId, conversationId));
        long started = System.currentTimeMillis();
        try {
            List<KnowledgeService.Evidence> evidence = knowledge.search(userId, question.trim());
            String answer = evidence.isEmpty() ? model.ask(config, history, question.trim()) :
                    model.ask(config, history, question.trim(), evidencePrompt(evidence));
            if (!evidence.isEmpty()) answer += "\n\n来源：" + evidence.stream()
                    .map(item -> item.fileName() + "（版本 " + item.version() + "）")
                    .distinct().collect(java.util.stream.Collectors.joining("；"));
            // 推理可能持续数秒；期间若管理员关闭权限，答案不得保存或返回。
            if (!available(userId) || !knowledge.stillAccessible(userId, evidence)) {
                repository.audit(tenantId, userId, "ask", "revoked_during_request", config.modelName(),
                        System.currentTimeMillis()-started);
                throw new RevokedException();
            }
            long id = conversationId == null ? repository.createConversation(tenantId, userId,
                    question.length() > 60 ? question.substring(0, 60) : question) : conversationId;
            repository.addMessage(tenantId, id, "user", question.trim());
            if (evidence.isEmpty()) repository.addMessage(tenantId, id, "assistant", answer);
            else repository.addKnowledgeAnswer(tenantId, id, answer,
                    evidence.stream().map(item -> new AssistantRepository.KnowledgeRef(item.documentId(), item.version())).toList());
            repository.audit(tenantId, userId, "ask", "success", config.modelName(), System.currentTimeMillis()-started);
            return new ChatAnswer(id, answer);
        } catch (RevokedException ex) {
            throw error(3, ex.getMessage());
        } catch (RuntimeException ex) {
            repository.audit(tenantId, userId, "ask", "model_or_storage_error", config.modelName(), System.currentTimeMillis()-started);
            log.warn("[ask] tenantId={} userId={} assistant failed", tenantId, userId, ex);
            throw error(6, "助理暂时不可用，请稍后重试");
        }
    }

    public List<Map<String, Object>> myConversations(long userId) {
        if (!available(userId)) throw error(3, "当前员工未开通助理");
        return repository.listConversations(tenantId(), userId);
    }

    public List<Map<String, Object>> myMessages(long userId, long conversationId) {
        if (!available(userId) || !repository.ownsConversation(tenantId(), userId, conversationId)) {
            throw error(5, "会话不存在或无权访问");
        }
        List<Map<String, Object>> messages = repository.listMessages(tenantId(), conversationId);
        var refs = repository.messageReferences(tenantId(), messages.stream()
                .map(item -> ((Number) item.get("id")).longValue()).toList());
        var allowed = knowledge.accessibleDocumentVersions(userId);
        return messages.stream().filter(item -> refs.getOrDefault(((Number) item.get("id")).longValue(), List.of())
                .stream().allMatch(ref -> Objects.equals(allowed.get(ref.documentId()), ref.version()))).toList();
    }

    private List<AssistantRepository.Message> visibleHistory(long userId, List<AssistantRepository.Message> history) {
        var refs = repository.messageReferences(tenantId(), history.stream().map(AssistantRepository.Message::id).toList());
        var allowed = knowledge.accessibleDocumentVersions(userId);
        return history.stream().filter(item -> refs.getOrDefault(item.id(), List.of()).stream()
                .allMatch(ref -> Objects.equals(allowed.get(ref.documentId()), ref.version()))).toList();
    }

    private static String evidencePrompt(List<KnowledgeService.Evidence> evidence) {
        StringBuilder prompt = new StringBuilder();
        for (var item : evidence) prompt.append("[文档 ").append(item.fileName()).append(" 版本 ")
                .append(item.version()).append("]\n").append(item.content()).append("\n");
        return prompt.toString();
    }

    public Map<String, Object> overview() {
        Map<String, Object> result = new java.util.HashMap<>(repository.overview(tenantId()));
        AssistantRepository.Config config = getConfig();
        result.put("enabled", config.enabled());
        result.put("modelConfigured", config.modelBaseUrl() != null && config.modelName() != null);
        result.put("lastModelTestOutcome", repository.lastModelTestOutcome(tenantId()));
        return result;
    }

    public List<Map<String, Object>> audit(int limit) {
        if (limit < 1 || limit > 200) throw error(1, "查询条数无效");
        return repository.listAudit(tenantId(), limit);
    }

    /** 按各租户设置的保存期限清理会话、消息和审计元数据。 */
    @Scheduled(cron = "0 15 3 * * ?")
    public void cleanupExpired() { repository.deleteExpiredData(); }

    private static final class RevokedException extends IllegalStateException {
        private RevokedException() { super("助理权限已关闭"); }
    }

    private static ServiceException error(int suffix, String message) {
        return new ServiceException(1_060_000_000 + suffix, message);
    }
}
