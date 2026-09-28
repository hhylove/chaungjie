-- 部门负责人及企微成员资料。手机、邮箱、性别仅在企微应用实际返回时写入镜像。
ALTER TABLE system_wecom_dept
  ADD COLUMN leader_user_ids JSON NULL COMMENT '企微部门负责人 userid 列表' AFTER name;

ALTER TABLE system_wecom_user
  ADD COLUMN mobile VARCHAR(32) NULL COMMENT '企微成员手机' AFTER department_ids,
  ADD COLUMN email VARCHAR(100) NULL COMMENT '企微成员邮箱' AFTER mobile,
  ADD COLUMN sex TINYINT NULL COMMENT '企微成员性别：1男2女' AFTER email;
