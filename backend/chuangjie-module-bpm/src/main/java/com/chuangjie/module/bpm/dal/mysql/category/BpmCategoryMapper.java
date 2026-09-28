package com.chuangjie.module.bpm.dal.mysql.category;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.bpm.controller.admin.definition.vo.category.BpmCategoryPageReqVO;
import com.chuangjie.module.bpm.dal.dataobject.definition.BpmCategoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * BPM 流程分类 Mapper
 *
 * @author hhy
 */
@Mapper
public interface BpmCategoryMapper extends BaseMapperX<BpmCategoryDO> {

    default PageResult<BpmCategoryDO> selectPage(BpmCategoryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<BpmCategoryDO>()
                .likeIfPresent(BpmCategoryDO::getName, reqVO.getName())
                .likeIfPresent(BpmCategoryDO::getCode, reqVO.getCode())
                .eqIfPresent(BpmCategoryDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(BpmCategoryDO::getCreateTime, reqVO.getCreateTime())
                .orderByAsc(BpmCategoryDO::getSort));
    }

    default BpmCategoryDO selectByName(String name) {
        return selectOne(BpmCategoryDO::getName, name);
    }

    default BpmCategoryDO selectByCode(String code) {
        return selectOne(BpmCategoryDO::getCode, code);
    }

    default List<BpmCategoryDO> selectListByCode(Collection<String> codes) {
        return selectList(BpmCategoryDO::getCode, codes);
    }

    default List<BpmCategoryDO> selectListByStatus(Integer status) {
        return selectList(BpmCategoryDO::getStatus, status);
    }

}