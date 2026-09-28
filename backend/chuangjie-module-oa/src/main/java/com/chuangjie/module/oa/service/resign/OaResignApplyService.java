package com.chuangjie.module.oa.service.resign;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.module.oa.controller.admin.resign.vo.OaResignApplyPageReqVO;
import com.chuangjie.module.oa.controller.admin.resign.vo.OaResignApplySaveReqVO;
import com.chuangjie.module.oa.controller.admin.resign.vo.OaResignApplySubmitReqVO;
import com.chuangjie.module.oa.dal.dataobject.resign.OaResignApplyDO;

/**
 * 离职申请 Service 接口
 *
 * @author 芋道源码
 */
public interface OaResignApplyService {

    /**
     * 创建离职申请草稿
     *
     * @param saveReqVO 申请内容
     * @return 申请编号
     */
    Long createResignApply(OaResignApplySaveReqVO saveReqVO);

    /**
     * 更新本人离职申请草稿
     *
     * @param saveReqVO 申请内容
     * @param userId 当前用户编号
     */
    void updateResignApply(OaResignApplySaveReqVO saveReqVO, Long userId);

    /**
     * 提交本人离职申请草稿并发起审批
     *
     * @param submitReqVO 申请编号和自选审批人
     * @param userId 当前用户编号
     */
    void submitResignApply(OaResignApplySubmitReqVO submitReqVO, Long userId);

    /**
     * 获得离职申请
     *
     * @param id 申请编号
     * @return 离职申请
     */
    OaResignApplyDO getResignApply(Long id);

    /**
     * 获得本人离职申请分页
     *
     * @param userId 申请人编号
     * @param pageReqVO 分页条件
     * @return 离职申请分页
     */
    PageResult<OaResignApplyDO> getResignApplyPage(Long userId, OaResignApplyPageReqVO pageReqVO);

    /**
     * 更新离职申请审批状态
     *
     * @param id 申请编号
     * @param status 审批状态
     */
    void updateResignApplyStatus(Long id, Integer status);

}
