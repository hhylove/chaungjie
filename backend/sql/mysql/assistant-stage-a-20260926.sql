-- A 阶段迁移。先备份数据库，再执行本文件；所有助理数据按 tenant_id 隔离。
CREATE TABLE IF NOT EXISTS assistant_tenant_config (
  tenant_id BIGINT NOT NULL PRIMARY KEY,
  enabled TINYINT(1) NOT NULL DEFAULT 0,
  model_base_url VARCHAR(255) NULL,
  model_name VARCHAR(128) NULL,
  retention_days INT NOT NULL DEFAULT 30,
  requests_per_minute INT NOT NULL DEFAULT 10,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='助理租户配置';

CREATE TABLE IF NOT EXISTS assistant_user_grant (
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 0,
  operator_user_id BIGINT NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (tenant_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='助理员工开通';

CREATE TABLE IF NOT EXISTS assistant_conversation (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_owner_time (tenant_id, user_id, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='助理会话';

CREATE TABLE IF NOT EXISTS assistant_message (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  conversation_id BIGINT NOT NULL,
  speaker VARCHAR(16) NOT NULL,
  content TEXT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_conversation (tenant_id, conversation_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='助理会话消息';

CREATE TABLE IF NOT EXISTS assistant_audit (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  action VARCHAR(40) NOT NULL,
  outcome VARCHAR(32) NOT NULL,
  model_name VARCHAR(128) NULL,
  latency_ms BIGINT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant_time (tenant_id, create_time),
  KEY idx_user_time (tenant_id, user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='助理基础审计，不保存问答正文';

CREATE TABLE IF NOT EXISTS assistant_rate_bucket (
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  minute_bucket BIGINT NOT NULL,
  request_count INT NOT NULL DEFAULT 0,
  PRIMARY KEY (tenant_id, user_id, minute_bucket)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='助理每分钟请求计数';

-- 动态菜单：管理员总览、配置、人员、审计。使用现有租户角色菜单体系。
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show)
SELECT '智能助理管理', '', 1, 50, 0, '/assistant', 'ep:chat-dot-round', NULL, NULL, 0, b'1', b'0', b'1'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE path='/assistant' AND parent_id=0 AND deleted=b'0');
SET @assistant_menu_id = (SELECT id FROM system_menu WHERE path='/assistant' AND parent_id=0 AND deleted=b'0' ORDER BY id LIMIT 1);
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show)
SELECT '总览', 'assistant:admin:read', 2, 1, @assistant_menu_id, 'overview', 'ep:data-analysis', 'assistant/overview/index', 'AssistantOverview', 0, b'1', b'0', b'1'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id=@assistant_menu_id AND path='overview' AND deleted=b'0');
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show)
SELECT '全局配置', 'assistant:admin:manage', 2, 2, @assistant_menu_id, 'settings', 'ep:setting', 'assistant/settings/index', 'AssistantSettings', 0, b'1', b'0', b'1'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id=@assistant_menu_id AND path='settings' AND deleted=b'0');
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show)
SELECT '使用人员', 'assistant:admin:manage', 2, 3, @assistant_menu_id, 'users', 'ep:user', 'assistant/users/index', 'AssistantUsers', 0, b'1', b'0', b'1'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id=@assistant_menu_id AND path='users' AND deleted=b'0');
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show)
SELECT '对话审计', 'assistant:admin:read', 2, 4, @assistant_menu_id, 'audit', 'ep:document', 'assistant/audit/index', 'AssistantAudit', 0, b'1', b'0', b'1'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id=@assistant_menu_id AND path='audit' AND deleted=b'0');

-- 仅授权当前已有的租户管理员和平台超级管理员；新租户的角色需按租户创建流程授予本菜单。
INSERT INTO system_role_menu (role_id, menu_id, creator, updater, tenant_id)
SELECT r.id, m.id, 'assistant-migration', 'assistant-migration', r.tenant_id
FROM system_role r JOIN system_menu m ON (m.id=@assistant_menu_id OR m.parent_id=@assistant_menu_id)
WHERE r.code IN ('tenant_admin','super_admin') AND r.deleted=b'0' AND m.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.id AND rm.menu_id=m.id AND rm.tenant_id=r.tenant_id AND rm.deleted=b'0');
