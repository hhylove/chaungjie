package com.chuangjie.module.bpm.service.task;

import cn.hutool.core.util.StrUtil;
import com.chuangjie.module.bpm.enums.task.BpmCommentTypeEnum;
import com.chuangjie.module.bpm.framework.flowable.core.util.FlowableUtils;
import com.chuangjie.module.bpm.service.comment.BpmCommentService;
import com.chuangjie.module.system.api.notify.NotifyMessageSendApi;
import com.chuangjie.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import com.chuangjie.module.system.api.user.AdminUserApi;
import com.chuangjie.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.chuangjie.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.chuangjie.module.bpm.enums.ErrorCodeConstants.*;

/** 发起人手动催办当前待办任务。 */
@Service
public class BpmProcessInstanceUrgeService {

    private static final String TEMPLATE_CODE = "bpm_task_urge";
    private static final Duration URGE_INTERVAL = Duration.ofHours(24);

    @Resource
    private BpmProcessInstanceService processInstanceService;
    @Resource
    private TaskService taskService;
    @Resource
    private NotifyMessageSendApi notifyMessageSendApi;
    @Resource
    private BpmCommentService commentService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public int urge(Long userId, String processInstanceId) {
        ProcessInstance instance = processInstanceService.getProcessInstance(processInstanceId);
        if (instance == null || !Objects.equals(instance.getTenantId(), FlowableUtils.getTenantId())) {
            throw exception(PROCESS_INSTANCE_URGE_NOT_RUNNING);
        }
        if (!Objects.equals(instance.getStartUserId(), String.valueOf(userId))) {
            throw exception(PROCESS_INSTANCE_URGE_NOT_SELF);
        }
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().list();
        if (tasks.stream().noneMatch(task -> StrUtil.isNotBlank(task.getAssignee()))) {
            throw exception(PROCESS_INSTANCE_URGE_NO_ASSIGNEE);
        }
        AdminUserRespDTO sender = adminUserApi.getUser(userId);
        int sent = 0;
        for (Task task : tasks) {
            if (StrUtil.isBlank(task.getAssignee())) {
                continue;
            }
            String key = "bpm:task:urge:" + task.getId();
            if (!Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, "1", URGE_INTERVAL))) {
                continue;
            }
            try {
                Long assigneeId = Long.valueOf(task.getAssignee());
                AdminUserRespDTO assignee = adminUserApi.getUser(assigneeId);
                if (assignee == null) {
                    stringRedisTemplate.delete(key);
                    continue;
                }
                Map<String, Object> params = new HashMap<>();
                params.put("processInstanceName", instance.getName());
                params.put("taskName", task.getName());
                params.put("startUserNickname", sender.getNickname());
                params.put("processInstanceId", processInstanceId);
                NotifySendSingleToUserReqDTO message = new NotifySendSingleToUserReqDTO();
                message.setUserId(assigneeId);
                message.setTemplateCode(TEMPLATE_CODE);
                message.setTemplateParams(params);
                notifyMessageSendApi.sendSingleMessageToAdmin(message);
                commentService.createComment(task.getId(), processInstanceId, BpmCommentTypeEnum.URGE,
                        sender.getNickname(), assignee.getNickname());
                sent++;
            } catch (RuntimeException ex) {
                stringRedisTemplate.delete(key);
                throw ex;
            }
        }
        if (sent == 0) {
            throw exception(PROCESS_INSTANCE_URGE_TOO_FREQUENT);
        }
        return sent;
    }
}
