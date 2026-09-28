-- 仅用于从 2026-09-27 旧的三级菜单升级；不会改动其他业务菜单。
SET @employee_menu_id = (SELECT id FROM system_menu
    WHERE type = 2 AND deleted = b'0' AND
    (permission = 'hradmin:employee:query' OR (name = '人事行政管理' AND parent_id = 0))
    ORDER BY id LIMIT 1);
SET @folder_menu_id = (SELECT parent_id FROM system_menu WHERE id = @employee_menu_id AND parent_id <> 0);
SET @center_menu_id = (SELECT parent_id FROM system_menu WHERE id = @folder_menu_id AND name = '人事行政管理' AND type = 1);

UPDATE system_menu
SET name = '人事行政管理', permission = '', type = 2, parent_id = 0, path = '/hradmin',
    icon = 'ep:avatar', component = 'hradmin/employee/index', component_name = 'HradminEmployee'
WHERE id = @employee_menu_id;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, status, visible, keep_alive, always_show)
SELECT '人员档案查询', 'hradmin:employee:query', 3, 0, @employee_menu_id, '', 0, b'1', b'1', b'1'
WHERE @employee_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'hradmin:employee:query' AND deleted = b'0');

-- 旧目录只有本功能时才关闭，避免影响后来加入的其它菜单。
UPDATE system_menu
SET deleted = b'1'
WHERE id = @folder_menu_id
  AND name = '人事行政管理' AND type = 1
  AND NOT EXISTS (SELECT 1 FROM (SELECT parent_id FROM system_menu WHERE deleted = b'0') AS child WHERE child.parent_id = @folder_menu_id);

UPDATE system_menu
SET deleted = b'1'
WHERE id = @center_menu_id
  AND name = '创业者发展中心' AND type = 1 AND path = '/development'
  AND NOT EXISTS (SELECT 1 FROM (SELECT parent_id FROM system_menu WHERE deleted = b'0') AS child WHERE child.parent_id = @center_menu_id);
