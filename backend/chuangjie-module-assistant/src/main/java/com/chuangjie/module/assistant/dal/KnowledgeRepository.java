package com.chuangjie.module.assistant.dal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Statement;
import java.util.List;
import java.util.Map;

/** 独立知识库持久化。所有读取均带租户条件；员工检索还需匹配本人或部门授权。 */
@Repository
public class KnowledgeRepository {
    private final JdbcTemplate jdbc;

    public KnowledgeRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record EmbeddingConfig(String baseUrl, String modelName) {}

    public EmbeddingConfig embeddingConfig(long tenantId) {
        var rows = jdbc.query("SELECT embedding_base_url,embedding_model FROM assistant_tenant_config WHERE tenant_id=?",
                (rs, row) -> new EmbeddingConfig(rs.getString(1), rs.getString(2)), tenantId);
        return rows.isEmpty() ? new EmbeddingConfig(null, null) : rows.get(0);
    }

    public void saveEmbeddingConfig(long tenantId, String baseUrl, String modelName) {
        jdbc.update("INSERT INTO assistant_tenant_config (tenant_id,embedding_base_url,embedding_model) VALUES (?,?,?) " +
                "ON DUPLICATE KEY UPDATE embedding_base_url=VALUES(embedding_base_url),embedding_model=VALUES(embedding_model)",
                tenantId, baseUrl, modelName);
    }

    public long documentCount(long tenantId) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM assistant_knowledge_document WHERE tenant_id=?", Long.class, tenantId);
        return count == null ? 0 : count;
    }

    public List<Map<String, Object>> bases(long tenantId) {
        return jdbc.queryForList("SELECT k.id,k.name,k.description,k.visibility,k.enabled,k.owner_user_id,k.create_time,k.update_time," +
                "(SELECT COUNT(*) FROM assistant_knowledge_document d WHERE d.tenant_id=k.tenant_id AND d.knowledge_base_id=k.id) document_count " +
                "FROM assistant_knowledge_base k WHERE k.tenant_id=? ORDER BY k.id DESC", tenantId);
    }

    public boolean baseExists(long tenantId, long baseId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM assistant_knowledge_base WHERE tenant_id=? AND id=?",
                Integer.class, tenantId, baseId);
        return count != null && count > 0;
    }

    public long createBase(long tenantId, long ownerId, String name, String description, String visibility) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("INSERT INTO assistant_knowledge_base " +
                    "(tenant_id,owner_user_id,name,description,visibility) VALUES (?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, tenantId);
            statement.setLong(2, ownerId);
            statement.setString(3, name);
            statement.setString(4, description);
            statement.setString(5, visibility);
            return statement;
        }, key);
        return key.getKey().longValue();
    }

    public void updateBase(long tenantId, long baseId, String name, String description, String visibility, boolean enabled) {
        jdbc.update("UPDATE assistant_knowledge_base SET name=?,description=?,visibility=?,enabled=? WHERE tenant_id=? AND id=?",
                name, description, visibility, enabled, tenantId, baseId);
    }

    public List<Map<String, Object>> grants(long tenantId, long baseId) {
        return jdbc.queryForList("SELECT principal_type,principal_id FROM assistant_knowledge_grant " +
                "WHERE tenant_id=? AND knowledge_base_id=? ORDER BY principal_type,principal_id", tenantId, baseId);
    }

    public void replaceGrants(long tenantId, long baseId, List<Long> userIds, List<Long> deptIds, List<Long> roleIds) {
        jdbc.update("DELETE FROM assistant_knowledge_grant WHERE tenant_id=? AND knowledge_base_id=?", tenantId, baseId);
        for (Long id : userIds) jdbc.update("INSERT INTO assistant_knowledge_grant VALUES (?,?,?,?)", tenantId, baseId, "user", id);
        for (Long id : deptIds) jdbc.update("INSERT INTO assistant_knowledge_grant VALUES (?,?,?,?)", tenantId, baseId, "dept", id);
        for (Long id : roleIds) jdbc.update("INSERT INTO assistant_knowledge_grant VALUES (?,?,?,?)", tenantId, baseId, "role", id);
    }

    public List<Map<String, Object>> documents(long tenantId, long baseId) {
        return jdbc.queryForList("SELECT id,knowledge_base_id,file_name,version,enabled,index_status,create_time,update_time " +
                "FROM assistant_knowledge_document WHERE tenant_id=? AND knowledge_base_id=? ORDER BY id DESC", tenantId, baseId);
    }

    public boolean documentExists(long tenantId, long baseId, long documentId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM assistant_knowledge_document " +
                "WHERE tenant_id=? AND knowledge_base_id=? AND id=?", Integer.class, tenantId, baseId, documentId);
        return count != null && count > 0;
    }

    public int nextVersion(long tenantId, long baseId, long documentId) {
        return jdbc.queryForObject("SELECT version+1 FROM assistant_knowledge_document WHERE tenant_id=? AND knowledge_base_id=? AND id=?",
                Integer.class, tenantId, baseId, documentId);
    }

    public long saveDocument(long tenantId, long baseId, Long documentId, String fileName, String content, int version) {
        if (documentId != null) {
            jdbc.update("UPDATE assistant_knowledge_document SET file_name=?,content=?,version=?,enabled=1,index_status='ready' " +
                    "WHERE tenant_id=? AND knowledge_base_id=? AND id=?", fileName, content, version, tenantId, baseId, documentId);
            jdbc.update("DELETE FROM assistant_knowledge_chunk WHERE tenant_id=? AND document_id=?", tenantId, documentId);
            return documentId;
        }
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("INSERT INTO assistant_knowledge_document " +
                    "(tenant_id,knowledge_base_id,file_name,content,version) VALUES (?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, tenantId);
            statement.setLong(2, baseId);
            statement.setString(3, fileName);
            statement.setString(4, content);
            statement.setInt(5, version);
            return statement;
        }, key);
        return key.getKey().longValue();
    }

    public void addChunk(long tenantId, long baseId, long documentId, int version, int number,
                         String content, String embedding) {
        jdbc.update("INSERT INTO assistant_knowledge_chunk " +
                "(tenant_id,knowledge_base_id,document_id,document_version,chunk_no,content,embedding) VALUES (?,?,?,?,?,?,?)",
                tenantId, baseId, documentId, version, number, content, embedding);
    }

    public void setDocumentEnabled(long tenantId, long baseId, long documentId, boolean enabled) {
        jdbc.update("UPDATE assistant_knowledge_document SET enabled=? WHERE tenant_id=? AND knowledge_base_id=? AND id=?",
                enabled, tenantId, baseId, documentId);
    }

    public void deleteDocument(long tenantId, long baseId, long documentId) {
        jdbc.update("DELETE FROM assistant_knowledge_chunk WHERE tenant_id=? AND document_id=?", tenantId, documentId);
        jdbc.update("DELETE FROM assistant_knowledge_document WHERE tenant_id=? AND knowledge_base_id=? AND id=?",
                tenantId, baseId, documentId);
    }

    public List<Map<String, Object>> accessibleChunks(long tenantId, long userId, Long deptId) {
        // ponytail: 单租户最多扫描 5000 段；知识规模增长时迁移到独立向量索引。
        return jdbc.queryForList("SELECT c.content,c.embedding,d.id document_id,d.file_name,d.version,k.id knowledge_base_id " +
                "FROM assistant_knowledge_chunk c JOIN assistant_knowledge_document d " +
                "ON d.id=c.document_id AND d.tenant_id=c.tenant_id AND d.enabled=1 AND d.version=c.document_version " +
                "JOIN assistant_knowledge_base k ON k.id=c.knowledge_base_id AND k.tenant_id=c.tenant_id AND k.enabled=1 " +
                "WHERE c.tenant_id=? AND (k.visibility='all' OR EXISTS " +
                "(SELECT 1 FROM assistant_knowledge_grant g WHERE g.tenant_id=k.tenant_id AND g.knowledge_base_id=k.id " +
                "AND ((g.principal_type='user' AND g.principal_id=?) OR " +
                "(g.principal_type='dept' AND g.principal_id=?) OR " +
                "(g.principal_type='role' AND EXISTS (SELECT 1 FROM system_user_role ur JOIN system_role r " +
                "ON r.id=ur.role_id AND r.tenant_id=ur.tenant_id AND r.status=0 AND r.deleted=b'0' " +
                "WHERE ur.tenant_id=g.tenant_id AND ur.user_id=? AND ur.role_id=g.principal_id AND ur.deleted=b'0'))))) " +
                "ORDER BY c.id DESC LIMIT 5000",
                tenantId, userId, deptId == null ? -1L : deptId, userId);
    }

    public Map<Long, Integer> accessibleDocumentVersions(long tenantId, long userId, Long deptId) {
        return jdbc.query("SELECT d.id,d.version FROM assistant_knowledge_document d JOIN assistant_knowledge_base k " +
                "ON k.id=d.knowledge_base_id AND k.tenant_id=d.tenant_id AND k.enabled=1 " +
                "WHERE d.tenant_id=? AND d.enabled=1 AND (k.visibility='all' OR EXISTS " +
                "(SELECT 1 FROM assistant_knowledge_grant g WHERE g.tenant_id=k.tenant_id AND g.knowledge_base_id=k.id " +
                "AND ((g.principal_type='user' AND g.principal_id=?) OR " +
                "(g.principal_type='dept' AND g.principal_id=?) OR " +
                "(g.principal_type='role' AND EXISTS (SELECT 1 FROM system_user_role ur JOIN system_role r " +
                "ON r.id=ur.role_id AND r.tenant_id=ur.tenant_id AND r.status=0 AND r.deleted=b'0' " +
                "WHERE ur.tenant_id=g.tenant_id AND ur.user_id=? AND ur.role_id=g.principal_id AND ur.deleted=b'0')))))",
                rs -> {
                    Map<Long, Integer> result = new java.util.HashMap<>();
                    while (rs.next()) result.put(rs.getLong(1), rs.getInt(2));
                    return result;
                }, tenantId, userId, deptId == null ? -1L : deptId, userId);
    }
}
