package com.chuangjie.module.assistant.service;

import com.chuangjie.framework.common.exception.ServiceException;
import com.chuangjie.framework.tenant.core.context.TenantContextHolder;
import com.chuangjie.module.assistant.dal.KnowledgeRepository;
import com.chuangjie.module.system.dal.dataobject.user.AdminUserDO;
import com.chuangjie.module.system.service.dept.DeptService;
import com.chuangjie.module.system.service.permission.RoleService;
import com.chuangjie.module.system.service.user.AdminUserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** B 阶段知识库：管理员写入，员工只检索当前租户且已授权的文档。 */
@Service
public class KnowledgeService {
    private final KnowledgeRepository repository;
    private final LocalModelClient model;
    private final AdminUserService users;
    private final DeptService departments;
    private final RoleService roles;
    private final ObjectMapper json;

    public KnowledgeService(KnowledgeRepository repository, LocalModelClient model, AdminUserService users,
                            DeptService departments, RoleService roles, ObjectMapper json) {
        this.repository = repository;
        this.model = model;
        this.users = users;
        this.departments = departments;
        this.roles = roles;
        this.json = json;
    }

    private long tenantId() { return TenantContextHolder.getRequiredTenantId(); }
    private static ServiceException error(String message) { return new ServiceException(1_060_000_020, message); }

    public KnowledgeRepository.EmbeddingConfig getEmbeddingConfig() { return repository.embeddingConfig(tenantId()); }

    public void saveEmbeddingConfig(String baseUrl, String modelName) {
        if (baseUrl == null || baseUrl.length() > 255 || modelName == null || modelName.isBlank() || modelName.length() > 128) {
            throw error("请填写有效的内网向量模型地址和名称");
        }
        model.validateEndpoint(baseUrl);
        var existing = repository.embeddingConfig(tenantId());
        if (repository.documentCount(tenantId()) > 0 &&
                (!Objects.equals(existing.baseUrl(), baseUrl.trim()) || !Objects.equals(existing.modelName(), modelName.trim()))) {
            throw error("已有索引文档；更换向量模型前须清空文档并重新上传");
        }
        repository.saveEmbeddingConfig(tenantId(), baseUrl.trim(), modelName.trim());
    }

    public String testEmbedding() {
        var config = requireEmbeddingConfig();
        model.embed(config.baseUrl(), config.modelName(), "知识库连接测试");
        return "连接成功";
    }

    private KnowledgeRepository.EmbeddingConfig requireEmbeddingConfig() {
        var config = repository.embeddingConfig(tenantId());
        if (config == null || config.baseUrl() == null || config.modelName() == null) {
            throw error("请先配置内网向量模型");
        }
        return config;
    }

    public List<Map<String, Object>> bases() { return repository.bases(tenantId()); }

    public long createBase(long ownerId, String name, String description, String visibility) {
        validateBase(name, description, visibility);
        return repository.createBase(tenantId(), ownerId, name.trim(), description, visibility);
    }

    public void updateBase(long id, String name, String description, String visibility, boolean enabled) {
        requireBase(id);
        validateBase(name, description, visibility);
        repository.updateBase(tenantId(), id, name.trim(), description, visibility, enabled);
    }

    private static void validateBase(String name, String description, String visibility) {
        if (name == null || name.isBlank() || name.length() > 120 ||
                (description != null && description.length() > 500) ||
                !("all".equals(visibility) || "restricted".equals(visibility))) {
            throw error("知识库名称或可见范围无效");
        }
    }

    private void requireBase(long id) {
        if (!repository.baseExists(tenantId(), id)) throw error("知识库不存在");
    }

    public List<Map<String, Object>> grants(long baseId) {
        requireBase(baseId);
        return repository.grants(tenantId(), baseId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void replaceGrants(long baseId, List<Long> userIds, List<Long> deptIds, List<Long> roleIds) {
        requireBase(baseId);
        if (userIds == null || deptIds == null || roleIds == null || userIds.size() > 100 || deptIds.size() > 100 ||
                roleIds.size() > 100 || userIds.stream().anyMatch(Objects::isNull) ||
                deptIds.stream().anyMatch(Objects::isNull) || roleIds.stream().anyMatch(Objects::isNull)) {
            throw error("授权对象无效或超过 100 个");
        }
        List<Long> uniqueUsers = userIds.stream().distinct().toList();
        List<Long> uniqueDepts = deptIds.stream().distinct().toList();
        List<Long> uniqueRoles = roleIds.stream().distinct().toList();
        List<AdminUserDO> selected = uniqueUsers.isEmpty() ? List.of() : users.getUserList(uniqueUsers);
        if (selected.stream().anyMatch(u -> !Objects.equals(u.getTenantId(), tenantId())) ||
                selected.size() != uniqueUsers.size() ||
                uniqueDepts.stream().anyMatch(id -> {
                    var dept = departments.getDept(id);
                    return dept == null || !Objects.equals(dept.getTenantId(), tenantId());
                }) || uniqueRoles.stream().anyMatch(id -> {
                    var role = roles.getRole(id);
                    return role == null || !Objects.equals(role.getTenantId(), tenantId());
                })) throw error("授权员工、部门或角色不属于当前租户");
        repository.replaceGrants(tenantId(), baseId, uniqueUsers, uniqueDepts, uniqueRoles);
    }

    public List<Map<String, Object>> documents(long baseId) {
        requireBase(baseId);
        return repository.documents(tenantId(), baseId);
    }

    @Transactional(rollbackFor = Exception.class)
    public long upload(long baseId, Long documentId, MultipartFile file) throws IOException {
        requireBase(baseId);
        if (documentId != null && !repository.documentExists(tenantId(), baseId, documentId)) throw error("文档不存在");
        if (file == null || file.isEmpty() || file.getSize() > 2_000_000) throw error("文档不能为空或超过 2 MB");
        String name = file.getOriginalFilename();
        if (name == null || name.length() > 255) throw error("文件名无效");
        String content = parse(name, file.getBytes()).trim();
        if (content.isBlank() || content.length() > 160_000) throw error("文档无文本或解析内容超过 16 万字");
        List<String> chunks = split(content);
        var config = requireEmbeddingConfig();
        List<double[]> vectors = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i += 16) {
            vectors.addAll(model.embedBatch(config.baseUrl(), config.modelName(), chunks.subList(i, Math.min(i + 16, chunks.size()))));
        }
        int dimension = vectors.get(0).length;
        if (vectors.stream().anyMatch(v -> v.length != dimension)) throw error("向量模型返回维度不一致");
        int version = documentId == null ? 1 : repository.nextVersion(tenantId(), baseId, documentId);
        long id = repository.saveDocument(tenantId(), baseId, documentId, name, content, version);
        for (int i = 0; i < chunks.size(); i++) {
            repository.addChunk(tenantId(), baseId, id, version, i, chunks.get(i), json.writeValueAsString(vectors.get(i)));
        }
        return id;
    }

    private static String parse(String name, byte[] bytes) throws IOException {
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".txt") || lower.endsWith(".md")) return new String(bytes, StandardCharsets.UTF_8);
        if (lower.endsWith(".pdf")) {
            try (var pdf = Loader.loadPDF(bytes)) { return new PDFTextStripper().getText(pdf); }
        }
        if (lower.endsWith(".docx")) {
            try (var doc = new XWPFDocument(new ByteArrayInputStream(bytes));
                 var extractor = new XWPFWordExtractor(doc)) {
                return extractor.getText();
            }
        }
        throw error("仅支持 TXT、Markdown、PDF、DOCX");
    }

    private static List<String> split(String content) {
        List<String> chunks = new ArrayList<>();
        for (int offset = 0; offset < content.length(); offset += 700) {
            chunks.add(content.substring(offset, Math.min(offset + 800, content.length())));
            if (chunks.size() > 230) throw error("文档分段过多");
        }
        return chunks;
    }

    public void setDocumentEnabled(long baseId, long documentId, boolean enabled) {
        requireDocument(baseId, documentId);
        repository.setDocumentEnabled(tenantId(), baseId, documentId, enabled);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(long baseId, long documentId) {
        requireDocument(baseId, documentId);
        repository.deleteDocument(tenantId(), baseId, documentId);
    }

    private void requireDocument(long baseId, long documentId) {
        if (!repository.documentExists(tenantId(), baseId, documentId)) throw error("文档不存在");
    }

    public record Evidence(long documentId, String fileName, int version, String content, double score) {}

    public List<Evidence> search(long userId, String question) {
        AdminUserDO user = users.getUser(userId);
        if (user == null || !Objects.equals(user.getTenantId(), tenantId())) return List.of();
        var config = repository.embeddingConfig(tenantId());
        if (config == null || config.baseUrl() == null || config.modelName() == null) return List.of();
        List<Map<String, Object>> candidates = repository.accessibleChunks(tenantId(), userId, user.getDeptId());
        if (candidates.isEmpty()) return List.of();
        double[] query = model.embed(config.baseUrl(), config.modelName(), question);
        List<Evidence> found = new ArrayList<>();
        for (var row : candidates) {
            try {
                double[] vector = json.readValue(row.get("embedding").toString(), double[].class);
                double score = cosine(query, vector);
                if (score >= 0.35) found.add(new Evidence(((Number) row.get("document_id")).longValue(),
                        (String) row.get("file_name"), ((Number) row.get("version")).intValue(),
                        (String) row.get("content"), score));
            } catch (JsonProcessingException ex) { throw error("知识索引损坏，请重新上传文档"); }
        }
        return found.stream().sorted(Comparator.comparingDouble(Evidence::score).reversed()).limit(3).toList();
    }

    public boolean stillAccessible(long userId, List<Evidence> evidence) {
        var versions = accessibleDocumentVersions(userId);
        return evidence.stream().allMatch(item -> Objects.equals(versions.get(item.documentId()), item.version()));
    }

    public Map<Long, Integer> accessibleDocumentVersions(long userId) {
        AdminUserDO user = users.getUser(userId);
        if (user == null || !Objects.equals(user.getTenantId(), tenantId())) return Map.of();
        return repository.accessibleDocumentVersions(tenantId(), userId, user.getDeptId());
    }

    private static double cosine(double[] a, double[] b) {
        if (a.length != b.length) return -1;
        double dot = 0, left = 0, right = 0;
        for (int i = 0; i < a.length; i++) { dot += a[i] * b[i]; left += a[i] * a[i]; right += b[i] * b[i]; }
        return left == 0 || right == 0 ? -1 : dot / Math.sqrt(left * right);
    }
}
