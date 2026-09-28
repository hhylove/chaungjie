-- 已执行 hradmin-employee-20260927.sql 的环境运行一次。
-- 只补充实际可登记的员工事实；流程和签收结果将由各自业务表提供。
ALTER TABLE hradmin_employee
    ADD COLUMN probation_end_date DATE NULL COMMENT '试用期结束日' AFTER hire_date,
    ADD COLUMN contract_end_date DATE NULL COMMENT '劳动合同到期日' AFTER probation_end_date,
    ADD COLUMN project_name VARCHAR(100) NULL COMMENT '所属项目' AFTER position_name,
    ADD COLUMN contract_status TINYINT NULL COMMENT '0 待签署 1 已签署，NULL 未登记' AFTER contract_end_date,
    ADD COLUMN social_status TINYINT NULL COMMENT '0 未参保 1 已参保，NULL 未登记' AFTER contract_status,
    ADD COLUMN social_reason VARCHAR(500) NULL COMMENT '未参保原因或办理说明' AFTER social_status;
