-- 人事行政一期：只建人员档案。账号、角色和企微镜像继续由 System 管理。
CREATE TABLE IF NOT EXISTS hradmin_employee (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '档案 ID',
    tenant_id BIGINT NOT NULL COMMENT '租户 ID',
    employee_no VARCHAR(32) NOT NULL COMMENT '工号',
    user_id BIGINT NULL COMMENT '关联 system_users.id，待入职可为空',
    name VARCHAR(64) NOT NULL COMMENT '档案姓名',
    dept_id BIGINT NULL COMMENT '无系统账号时的人事部门；有账号时展示 System 部门',
    position_name VARCHAR(100) NULL COMMENT '任职名称',
    manager_user_id BIGINT NULL COMMENT '直属负责人账号 ID',
    hire_date DATE NULL COMMENT '入职日期',
    employment_status TINYINT NOT NULL DEFAULT 0 COMMENT '0 待入职 1 在职 2 离职交接 3 已离职',
    remark VARCHAR(1000) NULL COMMENT '人事备注',
    creator VARCHAR(64) NULL DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) NULL DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    UNIQUE KEY uk_hradmin_employee_no (tenant_id, employee_no),
    UNIQUE KEY uk_hradmin_employee_user (tenant_id, user_id),
    KEY idx_hradmin_employee_name (tenant_id, name),
    KEY idx_hradmin_employee_dept (tenant_id, dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人员档案';

-- 侧栏只有“人事行政管理”一个入口，其余业务在页面内切换；不自动授予普通角色。
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show)
SELECT '人事行政管理', '', 2, 70, 0, '/hradmin', 'ep:avatar', 'hradmin/employee/index', 'HradminEmployee', 0, b'1', b'1', b'1'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE name = '人事行政管理' AND type = 2 AND parent_id = 0 AND deleted = b'0');
SET @employee_menu_id = (SELECT id FROM system_menu WHERE name = '人事行政管理' AND type = 2 AND parent_id = 0 AND deleted = b'0' ORDER BY id LIMIT 1);
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, status, visible, keep_alive, always_show)
SELECT '人员档案查询', 'hradmin:employee:query', 3, 0, @employee_menu_id, '', 0, b'1', b'1', b'1'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'hradmin:employee:query' AND deleted = b'0');
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, status, visible, keep_alive, always_show)
SELECT '人员档案新增', 'hradmin:employee:create', 3, 1, @employee_menu_id, '', 0, b'1', b'1', b'1'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'hradmin:employee:create' AND deleted = b'0');
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, status, visible, keep_alive, always_show)
SELECT '人员档案修改', 'hradmin:employee:update', 3, 2, @employee_menu_id, '', 0, b'1', b'1', b'1'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'hradmin:employee:update' AND deleted = b'0');
