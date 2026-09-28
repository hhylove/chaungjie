package com.chuangjie.module.bpm.service.comment;

import com.chuangjie.module.bpm.controller.admin.comment.vo.BpmCommentCreateReqVO;
import com.chuangjie.module.bpm.enums.task.BpmCommentTypeEnum;
import com.chuangjie.module.bpm.service.task.BpmTaskService;
import com.chuangjie.module.bpm.service.task.BpmProcessInstanceAccessService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.flowable.engine.TaskService;
import org.flowable.engine.task.Comment;
import org.flowable.task.api.Task;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 流程评论 Service 实现类
 *
 * @author hhy
 */
@Service
public class BpmCommentServiceImpl implements BpmCommentService {

    @Resource
    private TaskService taskService;
    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private BpmTaskService bpmTaskService;
    @Resource
    private BpmProcessInstanceAccessService processInstanceAccessService;

    @Override
    public List<Comment> getCommentListByProcessInstanceId(Long userId, String processInstanceId) {
        processInstanceAccessService.validateProcessInstanceViewPermission(userId, processInstanceId);
        return taskService.getProcessInstanceComments(processInstanceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createComment(Long userId, @Valid BpmCommentCreateReqVO reqVO) {
        Task task = bpmTaskService.validateTaskExists(reqVO.getTaskId());
        processInstanceAccessService.validateProcessInstanceViewPermission(userId, task.getProcessInstanceId());
        createComment(task.getId(), task.getProcessInstanceId(), BpmCommentTypeEnum.COMMENT, reqVO.getMessage());
    }

    @Override
    public void createComment(String taskId, String processInstanceId, BpmCommentTypeEnum type, Object... params) {
        taskService.addComment(taskId, processInstanceId, type.getType(), type.formatComment(params));
    }

}
