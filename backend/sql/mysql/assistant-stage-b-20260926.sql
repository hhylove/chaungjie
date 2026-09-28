-- B 阶段：独立知识库。须在 A 阶段迁移后执行。
ALTER TABLE assistant_tenant_config
  ADD COLUMN embedding_base_url VARCHAR(255) NULL,
  ADD COLUMN embedding_model VARCHAR(128) NULL;

CREATE TABLE assistant_knowledge_base (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  description VARCHAR(500) NULL,
  visibility VARCHAR(16) NOT NULL DEFAULT 'restricted',
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  owner_user_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_tenant_enabled (tenant_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='助理独立知识库';

CREATE TABLE assistant_knowledge_grant (
  tenant_id BIGINT NOT NULL,
  knowledge_base_id BIGINT NOT NULL,
  principal_type VARCHAR(16) NOT NULL,
  principal_id BIGINT NOT NULL,
  PRIMARY KEY (tenant_id, knowledge_base_id, principal_type, principal_id),
  KEY idx_principal (tenant_id, principal_type, principal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='助理知识库员工或部门授权';

CREATE TABLE assistant_knowledge_document (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  knowledge_base_id BIGINT NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  content MEDIUMTEXT NOT NULL,
  version INT NOT NULL DEFAULT 1,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  index_status VARCHAR(16) NOT NULL DEFAULT 'ready',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_base (tenant_id, knowledge_base_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='助理知识文档；仅管理员可读正文';

CREATE TABLE assistant_knowledge_chunk (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  knowledge_base_id BIGINT NOT NULL,
  document_id BIGINT NOT NULL,
  document_version INT NOT NULL,
  chunk_no INT NOT NULL,
  content TEXT NOT NULL,
  embedding JSON NOT NULL,
  KEY idx_document (tenant_id, document_id),
  KEY idx_base (tenant_id, knowledge_base_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='助理知识分段与内网向量';

CREATE TABLE assistant_message_knowledge_ref (
  tenant_id BIGINT NOT NULL,
  message_id BIGINT NOT NULL,
  document_id BIGINT NOT NULL,
  document_version INT NOT NULL,
  PRIMARY KEY (tenant_id, message_id, document_id),
  KEY idx_document (tenant_id, document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='助理答案知识来源；撤权时过滤历史答案';

-- 不覆盖已有菜单与授权；仅新增知识库管理页。
SET @assistant_menu_id = (SELECT id FROM system_menu WHERE path='/assistant' AND parent_id=0 AND deleted=b'0' ORDER BY id LIMIT 1);
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show)
SELECT '独立知识库', 'assistant:admin:manage', 2, 5, @assistant_menu_id, 'knowledge', 'ep:reading', 'assistant/knowledge/index', 'AssistantKnowledge', 0, b'1', b'0', b'1'
WHERE @assistant_menu_id IS NOT NULL AND NOT EXISTS
  (SELECT 1 FROM system_menu WHERE parent_id=@assistant_menu_id AND path='knowledge' AND deleted=b'0');
INSERT INTO system_role_menu (role_id, menu_id, creator, updater, tenant_id)
SELECT r.id, m.id, 'assistant-migration', 'assistant-migration', r.tenant_id
FROM system_role r JOIN system_menu m ON m.parent_id=@assistant_menu_id AND m.path='knowledge'
WHERE r.code IN ('tenant_admin','super_admin') AND r.deleted=b'0' AND m.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.id AND rm.menu_id=m.id AND rm.tenant_id=r.tenant_id AND rm.deleted=b'0');
