package com.chuangjie.module.oa.dal.mysql.officialdoc;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.oa.controller.admin.officialdoc.vo.template.OaOfficialDocTemplatePageReqVO;
import com.chuangjie.module.oa.dal.dataobject.officialdoc.OaOfficialDocTemplateDO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * OA 套红模板 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface OaOfficialDocTemplateMapper extends BaseMapperX<OaOfficialDocTemplateDO> {

    default PageResult<OaOfficialDocTemplateDO> selectPage(OaOfficialDocTemplatePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OaOfficialDocTemplateDO>()
                .likeIfPresent(OaOfficialDocTemplateDO::getName, reqVO.getName())
                .eqIfPresent(OaOfficialDocTemplateDO::getStatus, reqVO.getStatus()).orderByAsc(OaOfficialDocTemplateDO::getSort).orderByDesc(OaOfficialDocTemplateDO::getId));
    }

    default List<OaOfficialDocTemplateDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<OaOfficialDocTemplateDO>()
                .eqIfPresent(OaOfficialDocTemplateDO::getStatus, status).orderByAsc(OaOfficialDocTemplateDO::getSort).orderByDesc(OaOfficialDocTemplateDO::getId));
    }

}
