package com.chuangjie.module.mes.dal.mysql.wm.returnissue;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.mes.controller.admin.wm.returnissue.vo.line.MesWmReturnIssueLinePageReqVO;
import com.chuangjie.module.mes.dal.dataobject.wm.returnissue.MesWmReturnIssueLineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 生产退料单行 Mapper
 */
@Mapper
public interface MesWmReturnIssueLineMapper extends BaseMapperX<MesWmReturnIssueLineDO> {

    default PageResult<MesWmReturnIssueLineDO> selectPage(MesWmReturnIssueLinePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesWmReturnIssueLineDO>()
                .eqIfPresent(MesWmReturnIssueLineDO::getIssueId, reqVO.getIssueId())
                .orderByDesc(MesWmReturnIssueLineDO::getId));
    }

    default List<MesWmReturnIssueLineDO> selectListByIssueId(Long issueId) {
        return selectList(MesWmReturnIssueLineDO::getIssueId, issueId);
    }

    default void deleteByIssueId(Long issueId) {
        delete(MesWmReturnIssueLineDO::getIssueId, issueId);
    }

}
