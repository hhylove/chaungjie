-- 招聘用人申请、候选人及阶段跟进；执行前先完成 hradmin-employee 脚本。
ALTER TABLE hradmin_employee
  ADD COLUMN account_activated_at DATETIME NULL,
  ADD COLUMN account_activated_by BIGINT NULL,
  ADD COLUMN account_activation_evidence VARCHAR(1000) NULL;
CREATE TABLE IF NOT EXISTS hradmin_hiring_request (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  job VARCHAR(100) NOT NULL,
  dept_id BIGINT NOT NULL,
  count INT NOT NULL,
  reason VARCHAR(1000) NOT NULL,
  status VARCHAR(32) NOT NULL,
  review_note VARCHAR(1000) NULL,
  creator VARCHAR(64) NULL DEFAULT '', create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) NULL DEFAULT '', update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  KEY idx_hradmin_hiring_tenant (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hradmin_candidate (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  request_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL,
  stage VARCHAR(32) NOT NULL,
  latest_note VARCHAR(1000) NULL,
  employee_id BIGINT NULL,
  creator VARCHAR(64) NULL DEFAULT '', create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) NULL DEFAULT '', update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  UNIQUE KEY uk_hradmin_candidate_employee (tenant_id, employee_id),
  KEY idx_hradmin_candidate_request (tenant_id, request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hradmin_candidate_history (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  candidate_id BIGINT NOT NULL,
  stage VARCHAR(32) NOT NULL,
  note VARCHAR(1000) NOT NULL,
  actor_user_id BIGINT NOT NULL,
  creator VARCHAR(64) NULL DEFAULT '', create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) NULL DEFAULT '', update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  KEY idx_hradmin_candidate_history (tenant_id, candidate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hradmin_employee_salary (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  employee_id BIGINT NOT NULL,
  agreed_monthly_salary DECIMAL(12,2) NOT NULL,
  status VARCHAR(32) NOT NULL COMMENT '待审批；审批流程接入后更新',
  creator VARCHAR(64) NULL DEFAULT '', create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) NULL DEFAULT '', update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  UNIQUE KEY uk_hradmin_salary_employee (tenant_id, employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hradmin_onboarding_task (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  employee_id BIGINT NOT NULL,
  stage_no TINYINT NOT NULL,
  sequence_no TINYINT NOT NULL,
  stage VARCHAR(32) NOT NULL,
  task_key VARCHAR(40) NOT NULL,
  title VARCHAR(100) NOT NULL,
  owner VARCHAR(32) NOT NULL,
  due_date DATE NOT NULL,
  done_at DATETIME NULL,
  evidence VARCHAR(1000) NULL,
  actor_user_id BIGINT NULL,
  creator VARCHAR(64) NULL DEFAULT '', create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) NULL DEFAULT '', update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  UNIQUE KEY uk_hradmin_onboarding_task (tenant_id, employee_id, task_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @hradmin_menu_id = (SELECT id FROM system_menu WHERE name='人事行政管理' AND deleted=b'0' ORDER BY id LIMIT 1);
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, status, visible, keep_alive, always_show)
SELECT x.name, x.permission, 3, x.sort, @hradmin_menu_id, '', 0, b'1', b'1', b'1'
FROM (
  SELECT '招聘查询' name, 'hradmin:recruitment:query' permission, 10 sort UNION ALL
  SELECT '用人申请', 'hradmin:recruitment:submit', 11 UNION ALL
  SELECT '用人审批', 'hradmin:recruitment:review', 12 UNION ALL
  SELECT '候选人登记', 'hradmin:recruitment:candidate', 13 UNION ALL
  SELECT '面试与评价办理', 'hradmin:recruitment:manager', 14 UNION ALL
  SELECT '录用审批', 'hradmin:recruitment:approve', 15 UNION ALL
  SELECT '录用转员工', 'hradmin:recruitment:convert', 16 UNION ALL
  SELECT '薪酬档案查看', 'hradmin:salary:read', 16 UNION ALL
  SELECT '薪酬档案录入', 'hradmin:salary:write', 17 UNION ALL
  SELECT '入职事项人事办理', 'hradmin:onboarding:hr', 18 UNION ALL
  SELECT '入职事项上级办理', 'hradmin:onboarding:manager', 19 UNION ALL
  SELECT '入职事项总监办理', 'hradmin:onboarding:director', 20
) x WHERE @hradmin_menu_id IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM system_menu m WHERE m.permission=x.permission AND m.deleted=b'0'
);
