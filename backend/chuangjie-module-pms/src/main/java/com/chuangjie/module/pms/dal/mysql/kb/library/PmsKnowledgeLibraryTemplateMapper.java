package com.chuangjie.module.pms.dal.mysql.kb.library;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.pms.controller.admin.kb.library.vo.template.PmsKnowledgeLibraryTemplatePageReqVO;
import com.chuangjie.module.pms.dal.dataobject.kb.library.PmsKnowledgeLibraryTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PmsKnowledgeLibraryTemplateMapper extends BaseMapperX<PmsKnowledgeLibraryTemplateDO> {

    default PageResult<PmsKnowledgeLibraryTemplateDO> selectPage(PmsKnowledgeLibraryTemplatePageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<PmsKnowledgeLibraryTemplateDO>()
                .likeIfPresent(PmsKnowledgeLibraryTemplateDO::getName, pageReqVO.getName())
                .eq(pageReqVO.getStatus() != null, PmsKnowledgeLibraryTemplateDO::getStatus, pageReqVO.getStatus())
                .orderByAsc(PmsKnowledgeLibraryTemplateDO::getSort)
                .orderByDesc(PmsKnowledgeLibraryTemplateDO::getId));
    }

    default List<PmsKnowledgeLibraryTemplateDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<PmsKnowledgeLibraryTemplateDO>()
                .eqIfPresent(PmsKnowledgeLibraryTemplateDO::getStatus, status)
                .orderByAsc(PmsKnowledgeLibraryTemplateDO::getSort)
                .orderByDesc(PmsKnowledgeLibraryTemplateDO::getId));
    }

    default PmsKnowledgeLibraryTemplateDO selectByName(String name) {
        return selectOne(PmsKnowledgeLibraryTemplateDO::getName, name);
    }

}
