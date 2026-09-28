-- 催办站内信模板和流程评论字典；重复执行不会重复插入。
INSERT INTO `system_notify_template`
    (`name`, `code`, `nickname`, `content`, `type`, `params`, `status`, `remark`, `creator`, `updater`)
SELECT '审批催办', 'bpm_task_urge', '审批中心',
       '您有待审批任务：{processInstanceName} / {taskName}，发起人 {startUserNickname} 已催办。请到审批中心处理。流程编号：{processInstanceId}',
       2, '["processInstanceName","taskName","startUserNickname","processInstanceId"]', 0,
       '流程发起人手动催办当前审批人', '1', '1'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code` = 'bpm_task_urge' AND `deleted` = b'0');

INSERT INTO `system_dict_data`
    (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `updater`)
SELECT 10, '催办', '10', 'bpm_comment_type', 0, 'warning', '', '流程评论类型 - 催办', '1', '1'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'bpm_comment_type' AND `value` = '10' AND `deleted` = b'0');
