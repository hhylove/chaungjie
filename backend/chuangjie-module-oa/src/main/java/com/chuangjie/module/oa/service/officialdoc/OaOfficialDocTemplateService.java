package com.chuangjie.module.oa.service.officialdoc;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.module.oa.controller.admin.officialdoc.vo.template.*;
import com.chuangjie.module.oa.dal.dataobject.officialdoc.*;
import java.util.List;

/**
 * 套红模板 Service 接口
 *
 * @author 芋道源码
 */
public interface OaOfficialDocTemplateService {

    /**
     * 创建套红模板
     *
     * @param reqVO 套红模板信息
     * @return 套红模板编号
     */
    Long createOfficialDocTemplate(OaOfficialDocTemplateSaveReqVO reqVO);

    /**
     * 更新套红模板
     *
     * @param reqVO 套红模板信息
     */
    void updateOfficialDocTemplate(OaOfficialDocTemplateSaveReqVO reqVO);

    /**
     * 删除套红模板
     *
     * @param id 套红模板编号
     */
    void deleteOfficialDocTemplate(Long id);

    /**
     * 获得套红模板
     *
     * @param id 套红模板编号
     * @return 套红模板
     */
    OaOfficialDocTemplateDO getOfficialDocTemplate(Long id);

    /**
     * 校验套红模板存在且已启用
     *
     * @param id 套红模板编号
     * @return 套红模板
     */
    OaOfficialDocTemplateDO validateOfficialDocTemplate(Long id);

    /**
     * 获得套红模板分页
     *
     * @param reqVO 分页条件
     * @return 套红模板分页
     */
    PageResult<OaOfficialDocTemplateDO> getOfficialDocTemplatePage(OaOfficialDocTemplatePageReqVO reqVO);

    /**
     * 获得模板列表
     *
     * @param status 状态
     * @return 模板列表
     */
    List<OaOfficialDocTemplateDO> getOfficialDocTemplateList(Integer status);

}
