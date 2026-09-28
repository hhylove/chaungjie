-- 企微部门与系统部门的稳定映射；执行一次即可。应用部门/账号需管理员在同步页确认。
ALTER TABLE system_wecom_dept
  ADD COLUMN system_dept_id BIGINT NULL COMMENT '关联的系统部门 ID' AFTER wecom_dept_id,
  ADD UNIQUE KEY uk_tenant_system_dept (tenant_id, system_dept_id);

SET @wecom_menu_id := (SELECT id FROM system_menu WHERE parent_id = 1 AND path = 'wecom-sync' AND deleted = 0 ORDER BY id LIMIT 1);
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show)
SELECT '企微应用到系统', 'system:wecom-sync:apply', 3, 4, @wecom_menu_id, '', '', '', '', 0, b'1', b'1', b'1'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'system:wecom-sync:apply' AND deleted = 0);
