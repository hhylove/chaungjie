package com.chuangjie.module.oa.service.overtime;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.module.oa.controller.admin.overtime.vo.OaOvertimeApplySaveReqVO;
import com.chuangjie.module.oa.controller.admin.overtime.vo.OaOvertimeApplySubmitReqVO;
import com.chuangjie.module.oa.controller.admin.overtime.vo.OaOvertimeApplyPageReqVO;
import com.chuangjie.module.oa.dal.dataobject.overtime.OaOvertimeApplyDO;

/**
 * 加班申请 Service 接口
 *
 * @author 芋道源码
 */
public interface OaOvertimeApplyService {

    /**
     * 创建加班申请草稿
     *
     * @param saveReqVO 申请内容
     * @return 申请编号
     */
    Long createOvertimeApply(OaOvertimeApplySaveReqVO saveReqVO);

    /**
     * 更新本人草稿
     *
     * @param saveReqVO 申请内容
     * @param userId 当前用户编号
     */
    void updateOvertimeApply(OaOvertimeApplySaveReqVO saveReqVO, Long userId);

    /**
     * 提交本人草稿并发起审批
     *
     * @param submitReqVO 申请编号和自选审批人
     * @param userId 当前用户编号
     */
    void submitOvertimeApply(OaOvertimeApplySubmitReqVO submitReqVO, Long userId);

    /**
     * 获得加班申请
     *
     * @param id 申请编号
     * @return 加班申请
     */
    OaOvertimeApplyDO getOvertimeApply(Long id);

    /**
     * 获得本人加班申请分页
     *
     * @param userId 申请人编号
     * @param pageReqVO 分页条件
     * @return 申请分页
     */
    PageResult<OaOvertimeApplyDO> getOvertimeApplyPage(Long userId, OaOvertimeApplyPageReqVO pageReqVO);

    /**
     * 更新加班申请审批状态
     *
     * @param id 申请编号
     * @param status 审批状态
     */
    void updateOvertimeApplyStatus(Long id, Integer status);

}
