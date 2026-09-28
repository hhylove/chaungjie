package com.chuangjie.module.pms.dal.mysql.pm.project;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.pms.controller.admin.pm.project.vo.template.PmsProjectTemplatePageReqVO;
import com.chuangjie.module.pms.dal.dataobject.pm.project.PmsProjectTemplateDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PmsProjectTemplateMapper extends BaseMapperX<PmsProjectTemplateDO> {

    default PageResult<PmsProjectTemplateDO> selectPage(PmsProjectTemplatePageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<PmsProjectTemplateDO>()
                .likeIfPresent(PmsProjectTemplateDO::getName, pageReqVO.getName())
                .eqIfPresent(PmsProjectTemplateDO::getProjectType, pageReqVO.getProjectType())
                .eqIfPresent(PmsProjectTemplateDO::getStatus, pageReqVO.getStatus())
                .orderByAsc(PmsProjectTemplateDO::getSort)
                .orderByDesc(PmsProjectTemplateDO::getId));
    }

    default PmsProjectTemplateDO selectByNameAndProjectType(String name, Integer projectType) {
        return selectOne(new LambdaQueryWrapperX<PmsProjectTemplateDO>()
                .eq(PmsProjectTemplateDO::getName, name)
                .eq(PmsProjectTemplateDO::getProjectType, projectType));
    }

}
