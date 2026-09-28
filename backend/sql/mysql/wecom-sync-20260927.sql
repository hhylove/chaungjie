-- 企业微信通讯录同步：只保存企微镜像数据，账号开通和权限由管理员决定。
CREATE TABLE IF NOT EXISTS system_wecom_dept (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  wecom_dept_id BIGINT NOT NULL,
  parent_wecom_dept_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  sort INT NOT NULL DEFAULT 0,
  last_seen_at DATETIME NOT NULL,
  creator VARCHAR(64) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_wecom_dept (tenant_id, wecom_dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企微部门镜像';

CREATE TABLE IF NOT EXISTS system_wecom_user (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  wecom_user_id VARCHAR(128) NOT NULL,
  name VARCHAR(128) NOT NULL,
  department_ids JSON NOT NULL,
  system_user_id BIGINT NULL,
  last_seen_at DATETIME NOT NULL,
  creator VARCHAR(64) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_wecom_user (tenant_id, wecom_user_id),
  UNIQUE KEY uk_tenant_system_user (tenant_id, system_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企微成员镜像；系统账号需管理员核对关联';

CREATE TABLE IF NOT EXISTS system_wecom_sync_run (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  department_count INT NOT NULL,
  user_count INT NOT NULL,
  creator VARCHAR(64) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_tenant_time (tenant_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企微通讯录成功同步记录';

-- 菜单权限。执行后由管理员在角色/租户套餐中授予，不自动扩大现有角色权限。
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show)
SELECT '企业微信同步', '', 2, 70, 1, 'wecom-sync', 'ep:connection', 'system/wecomSync/index', 'SystemWecomSync', 0, b'1', b'1', b'1'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id = 1 AND path = 'wecom-sync' AND deleted = 0);
SET @wecom_menu_id := (SELECT id FROM system_menu WHERE parent_id = 1 AND path = 'wecom-sync' AND deleted = 0 ORDER BY id LIMIT 1);
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show)
SELECT '企微同步查询', 'system:wecom-sync:query', 3, 1, @wecom_menu_id, '', '', '', '', 0, b'1', b'1', b'1'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'system:wecom-sync:query' AND deleted = 0);
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show)
SELECT '企微同步执行', 'system:wecom-sync:execute', 3, 2, @wecom_menu_id, '', '', '', '', 0, b'1', b'1', b'1'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'system:wecom-sync:execute' AND deleted = 0);
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show)
SELECT '企微账号关联', 'system:wecom-sync:link', 3, 3, @wecom_menu_id, '', '', '', '', 0, b'1', b'1', b'1'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'system:wecom-sync:link' AND deleted = 0);


