package com.chuangjie.module.assistant.dal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Repository
public class AssistantRepository {

    // 本模块直接使用 JdbcTemplate；下列面向租户的数据查询均显式带 tenant_id 条件。

    private final JdbcTemplate jdbc;

    public AssistantRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Config(long tenantId, boolean enabled, String modelBaseUrl, String modelName,
                         int retentionDays, int requestsPerMinute) {}
    public record Message(long id, String role, String content) {}
    public record KnowledgeRef(long documentId, int version) {}

    private static final RowMapper<Config> CONFIG_MAPPER = (rs, row) -> new Config(
            rs.getLong("tenant_id"), rs.getBoolean("enabled"), rs.getString("model_base_url"),
            rs.getString("model_name"), rs.getInt("retention_days"), rs.getInt("requests_per_minute"));

    public Config getConfig(long tenantId) {
        List<Config> list = jdbc.query("SELECT tenant_id,enabled,model_base_url,model_name,retention_days,requests_per_minute " +
                "FROM assistant_tenant_config WHERE tenant_id=?", CONFIG_MAPPER, tenantId);
        return list.isEmpty() ? new Config(tenantId, false, null, null, 30, 10) : list.get(0);
    }

    public void saveConfig(Config c) {
        jdbc.update("INSERT INTO assistant_tenant_config (tenant_id,enabled,model_base_url,model_name,retention_days,requests_per_minute) " +
                        "VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE enabled=VALUES(enabled),model_base_url=VALUES(model_base_url)," +
                        "model_name=VALUES(model_name),retention_days=VALUES(retention_days),requests_per_minute=VALUES(requests_per_minute)",
                c.tenantId(), c.enabled(), c.modelBaseUrl(), c.modelName(), c.retentionDays(), c.requestsPerMinute());
    }

    public boolean hasGrant(long tenantId, long userId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM assistant_user_grant WHERE tenant_id=? AND user_id=? AND enabled=1",
                Integer.class, tenantId, userId);
        return count != null && count > 0;
    }

    public Map<Long, Boolean> getGrants(long tenantId, List<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        String marks = String.join(",", java.util.Collections.nCopies(userIds.size(), "?"));
        Object[] args = new Object[userIds.size() + 1];
        args[0] = tenantId;
        for (int i = 0; i < userIds.size(); i++) args[i + 1] = userIds.get(i);
        return jdbc.query("SELECT user_id,enabled FROM assistant_user_grant WHERE tenant_id=? AND user_id IN (" + marks + ")",
                rs -> {
                    java.util.Map<Long, Boolean> result = new java.util.HashMap<>();
                    while (rs.next()) result.put(rs.getLong(1), rs.getBoolean(2));
                    return result;
                }, args);
    }

    public void setGrant(long tenantId, long userId, boolean enabled, long operatorId) {
        jdbc.update("INSERT INTO assistant_user_grant (tenant_id,user_id,enabled,operator_user_id) VALUES (?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE enabled=VALUES(enabled),operator_user_id=VALUES(operator_user_id)",
                tenantId, userId, enabled, operatorId);
    }

    public int reserveRequest(long tenantId, long userId) {
        // 利用数据库唯一键原子递增，避免并发请求绕过每分钟限额。
        long minute = System.currentTimeMillis() / 60000;
        jdbc.update("INSERT INTO assistant_rate_bucket (tenant_id,user_id,minute_bucket,request_count) VALUES (?,?,?,1) " +
                "ON DUPLICATE KEY UPDATE request_count=request_count+1", tenantId, userId, minute);
        Integer count = jdbc.queryForObject("SELECT request_count FROM assistant_rate_bucket " +
                "WHERE tenant_id=? AND user_id=? AND minute_bucket=?", Integer.class, tenantId, userId, minute);
        return count == null ? Integer.MAX_VALUE : count;
    }

    public long createConversation(long tenantId, long userId, String title) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO assistant_conversation (tenant_id,user_id,title) VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, tenantId);
            statement.setLong(2, userId);
            statement.setString(3, title);
            return statement;
        }, keys);
        if (keys.getKey() == null) throw new IllegalStateException("无法创建助理会话");
        return keys.getKey().longValue();
    }

    public boolean ownsConversation(long tenantId, long userId, long conversationId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM assistant_conversation WHERE id=? AND tenant_id=? AND user_id=?",
                Integer.class, conversationId, tenantId, userId);
        return count != null && count > 0;
    }

    public List<Message> recentMessages(long tenantId, long conversationId) {
        List<Message> newest = jdbc.query("SELECT id,speaker,content FROM assistant_message WHERE tenant_id=? AND conversation_id=? " +
                        "ORDER BY id DESC LIMIT 10", (rs, row) -> new Message(rs.getLong(1), rs.getString(2), rs.getString(3)),
                tenantId, conversationId);
        java.util.Collections.reverse(newest);
        return newest;
    }

    public void addMessage(long tenantId, long conversationId, String role, String content) {
        jdbc.update("INSERT INTO assistant_message (tenant_id,conversation_id,speaker,content) VALUES (?,?,?,?)",
                tenantId, conversationId, role, content);
        jdbc.update("UPDATE assistant_conversation SET update_time=CURRENT_TIMESTAMP WHERE id=? AND tenant_id=?",
                conversationId, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void addKnowledgeAnswer(long tenantId, long conversationId, String answer, List<KnowledgeRef> references) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO assistant_message (tenant_id,conversation_id,speaker,content) VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, tenantId);
            statement.setLong(2, conversationId);
            statement.setString(3, "assistant");
            statement.setString(4, answer);
            return statement;
        }, keys);
        long messageId = keys.getKey().longValue();
        for (KnowledgeRef ref : references.stream().distinct().toList()) {
            jdbc.update("INSERT INTO assistant_message_knowledge_ref " +
                    "(tenant_id,message_id,document_id,document_version) VALUES (?,?,?,?)",
                    tenantId, messageId, ref.documentId(), ref.version());
        }
    }

    public Map<Long, List<KnowledgeRef>> messageReferences(long tenantId, List<Long> messageIds) {
        if (messageIds.isEmpty()) return Map.of();
        String marks = String.join(",", java.util.Collections.nCopies(messageIds.size(), "?"));
        Object[] args = new Object[messageIds.size() + 1];
        args[0] = tenantId;
        for (int i = 0; i < messageIds.size(); i++) args[i + 1] = messageIds.get(i);
        return jdbc.query("SELECT message_id,document_id,document_version FROM assistant_message_knowledge_ref " +
                "WHERE tenant_id=? AND message_id IN (" + marks + ")", rs -> {
            Map<Long, List<KnowledgeRef>> result = new java.util.HashMap<>();
            while (rs.next()) result.computeIfAbsent(rs.getLong(1), ignored -> new java.util.ArrayList<>())
                    .add(new KnowledgeRef(rs.getLong(2), rs.getInt(3)));
            return result;
        }, args);
    }

    public List<Map<String, Object>> listConversations(long tenantId, long userId) {
        return jdbc.queryForList("SELECT id,title,create_time,update_time FROM assistant_conversation " +
                "WHERE tenant_id=? AND user_id=? ORDER BY update_time DESC LIMIT 50", tenantId, userId);
    }

    public List<Map<String, Object>> listMessages(long tenantId, long conversationId) {
        List<Map<String, Object>> newest = jdbc.queryForList("SELECT id,speaker AS role,content,create_time FROM assistant_message " +
                "WHERE tenant_id=? AND conversation_id=? ORDER BY id DESC LIMIT 200", tenantId, conversationId);
        java.util.Collections.reverse(newest);
        return newest;
    }

    public void audit(long tenantId, long userId, String action, String outcome, String modelName, Long latencyMs) {
        jdbc.update("INSERT INTO assistant_audit (tenant_id,user_id,action,outcome,model_name,latency_ms) VALUES (?,?,?,?,?,?)",
                tenantId, userId, action, outcome, modelName, latencyMs);
    }

    public List<Map<String, Object>> listAudit(long tenantId, int limit) {
        return jdbc.queryForList("SELECT a.id,a.user_id,u.username,u.nickname,a.action,a.outcome," +
                "a.model_name,a.latency_ms,a.create_time FROM assistant_audit a " +
                "LEFT JOIN system_users u ON u.id=a.user_id AND u.tenant_id=a.tenant_id " +
                "WHERE a.tenant_id=? ORDER BY a.id DESC LIMIT ?", tenantId, limit);
    }

    public Map<String, Object> overview(long tenantId) {
        Long users = jdbc.queryForObject("SELECT COUNT(*) FROM assistant_user_grant WHERE tenant_id=? AND enabled=1",
                Long.class, tenantId);
        Long requests = jdbc.queryForObject("SELECT COUNT(*) FROM assistant_audit WHERE tenant_id=? AND action='ask' " +
                "AND create_time>=DATE_SUB(NOW(), INTERVAL 7 DAY)", Long.class, tenantId);
        Long failures = jdbc.queryForObject("SELECT COUNT(*) FROM assistant_audit WHERE tenant_id=? AND action='ask' " +
                "AND outcome<>'success' AND create_time>=DATE_SUB(NOW(), INTERVAL 7 DAY)", Long.class, tenantId);
        return Map.of("enabledUsers", users == null ? 0 : users,
                "requests7d", requests == null ? 0 : requests, "failures7d", failures == null ? 0 : failures);
    }

    public String lastModelTestOutcome(long tenantId) {
        List<String> outcomes = jdbc.query("SELECT outcome FROM assistant_audit WHERE tenant_id=? AND action='model_test' " +
                        "AND id>(SELECT COALESCE(MAX(id),0) FROM assistant_audit WHERE tenant_id=? AND action='config_update') " +
                        "ORDER BY id DESC LIMIT 1", (rs, row) -> rs.getString(1), tenantId, tenantId);
        return outcomes.isEmpty() ? "not_tested" : outcomes.get(0);
    }

    public void deleteExpiredData() {
        // 保存期限由各租户分别配置；清理任务遍历配置，不能用单一全局天数。
        List<Config> configs = jdbc.query("SELECT tenant_id,enabled,model_base_url,model_name,retention_days,requests_per_minute " +
                "FROM assistant_tenant_config", CONFIG_MAPPER);
        for (Config c : configs) {
            jdbc.update("DELETE r FROM assistant_message_knowledge_ref r LEFT JOIN assistant_message m " +
                    "ON m.id=r.message_id AND m.tenant_id=r.tenant_id " +
                    "WHERE r.tenant_id=? AND (m.id IS NULL OR m.create_time<DATE_SUB(NOW(), INTERVAL ? DAY))",
                    c.tenantId(), c.retentionDays());
            jdbc.update("DELETE FROM assistant_message WHERE tenant_id=? AND create_time<DATE_SUB(NOW(), INTERVAL ? DAY)",
                    c.tenantId(), c.retentionDays());
            jdbc.update("DELETE FROM assistant_conversation WHERE tenant_id=? AND update_time<DATE_SUB(NOW(), INTERVAL ? DAY)",
                    c.tenantId(), c.retentionDays());
            jdbc.update("DELETE FROM assistant_audit WHERE tenant_id=? AND create_time<DATE_SUB(NOW(), INTERVAL ? DAY)",
                    c.tenantId(), c.retentionDays());
        }
        jdbc.update("DELETE FROM assistant_rate_bucket WHERE minute_bucket<?", System.currentTimeMillis()/60000 - 2);
    }
}
