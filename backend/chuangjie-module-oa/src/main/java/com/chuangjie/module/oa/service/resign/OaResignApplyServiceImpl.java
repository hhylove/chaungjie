package com.chuangjie.module.oa.service.resign;

import cn.hutool.core.util.ObjUtil;
import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.common.util.object.BeanUtils;
import com.chuangjie.module.bpm.api.task.BpmProcessInstanceApi;
import com.chuangjie.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.chuangjie.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.chuangjie.module.oa.controller.admin.resign.vo.OaResignApplyPageReqVO;
import com.chuangjie.module.oa.controller.admin.resign.vo.OaResignApplySaveReqVO;
import com.chuangjie.module.oa.controller.admin.resign.vo.OaResignApplySubmitReqVO;
import com.chuangjie.module.oa.dal.dataobject.resign.OaResignApplyDO;
import com.chuangjie.module.oa.dal.mysql.resign.OaResignApplyMapper;
import com.chuangjie.module.oa.enums.BpmModelConstants;
import com.chuangjie.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

import static com.chuangjie.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.chuangjie.module.oa.enums.ErrorCodeConstants.*;

/**
 * 离职申请 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class OaResignApplyServiceImpl implements OaResignApplyService {

    @Resource
    private OaResignApplyMapper resignApplyMapper;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createResignApply(OaResignApplySaveReqVO saveReqVO) {
        // 1. 校验交接人存在
        adminUserApi.validateUser(saveReqVO.getHandoverUserId());

        // 2. 保存草稿
        OaResignApplyDO resignApply = BeanUtils.toBean(saveReqVO, OaResignApplyDO.class)
                .setStatus(BpmProcessInstanceStatusEnum.NOT_START.getStatus());
        resignApplyMapper.insert(resignApply);
        return resignApply.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateResignApply(OaResignApplySaveReqVO saveReqVO, Long userId) {
        // 1.1 校验申请可修改
        validateResignApplyEditable(saveReqVO.getId(), userId);
        // 1.2 校验交接人存在
        adminUserApi.validateUser(saveReqVO.getHandoverUserId());

        // 2. 更新草稿
        resignApplyMapper.updateById(BeanUtils.toBean(saveReqVO, OaResignApplyDO.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitResignApply(OaResignApplySubmitReqVO submitReqVO, Long userId) {
        // 1. 校验申请可提交
        OaResignApplyDO resignApply = validateResignApplyEditable(submitReqVO.getId(), userId);

        // 2. 更新为审批中
        resignApplyMapper.updateById(new OaResignApplyDO().setId(resignApply.getId())
                .setStatus(BpmProcessInstanceStatusEnum.RUNNING.getStatus()));

        // 3.1 发起审批
        Map<String, Object> variables = new HashMap<>();
        variables.put("hasPendingReimbursement", resignApply.getHasPendingReimbursement()); // 是否存在未完成报销
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(BpmModelConstants.RESIGN_APPLY)
                        .setBusinessKey(resignApply.getId().toString()).setVariables(variables)
                        .setStartUserSelectAssignees(submitReqVO.getStartUserSelectAssignees()));
        // 3.2 绑定流程编号
        resignApplyMapper.updateById(new OaResignApplyDO().setId(resignApply.getId()).setProcessInstanceId(processInstanceId));
    }

    @Override
    public OaResignApplyDO getResignApply(Long id) {
        return resignApplyMapper.selectById(id);
    }

    @Override
    public PageResult<OaResignApplyDO> getResignApplyPage(Long userId, OaResignApplyPageReqVO pageReqVO) {
        return resignApplyMapper.selectPage(userId, pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateResignApplyStatus(Long id, Integer status) {
        // 1. 校验申请存在
        validateResignApplyExists(id);

        // 2. 更新审批状态
        resignApplyMapper.updateById(new OaResignApplyDO().setId(id).setStatus(status));
    }

    /**
     * 校验申请可修改或提交
     *
     * @param id 申请编号
     * @param userId 当前用户编号
     * @return 本人的未提交申请
     */
    private OaResignApplyDO validateResignApplyEditable(Long id, Long userId) {
        // 1. 查询申请
        OaResignApplyDO resignApply = resignApplyMapper.selectById(id);
        if (resignApply == null) {
            throw exception(APPLY_NOT_EXISTS);
        }
        // 2. 校验本人归属
        if (ObjUtil.notEqual(resignApply.getCreator(), userId.toString())) {
            throw exception(APPLY_ACCESS_DENIED);
        }
        // 3. 只有未提交草稿允许修改或提交
        if (ObjUtil.notEqual(resignApply.getStatus(), BpmProcessInstanceStatusEnum.NOT_START.getStatus())) {
            throw exception(APPLY_STATUS_INVALID);
        }
        return resignApply;
    }

    /**
     * 校验离职申请存在
     *
     * @param id 申请编号
     * @return 申请
     */
    @SuppressWarnings("UnusedReturnValue")
    private OaResignApplyDO validateResignApplyExists(Long id) {
        OaResignApplyDO resignApply = resignApplyMapper.selectById(id);
        if (resignApply == null) {
            throw exception(APPLY_NOT_EXISTS);
        }
        return resignApply;
    }

}
