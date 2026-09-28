package com.chuangjie.module.mes.dal.mysql.wm.outsourceissue;

import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.module.mes.dal.dataobject.wm.outsourceissue.MesWmOutsourceIssueDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 外协发料单明细 Mapper
 *
 * @author hhy
 */
@Mapper
public interface MesWmOutsourceIssueDetailMapper extends BaseMapperX<MesWmOutsourceIssueDetailDO> {

    default List<MesWmOutsourceIssueDetailDO> selectListByIssueId(Long issueId) {
        return selectList(MesWmOutsourceIssueDetailDO::getIssueId, issueId);
    }

    default List<MesWmOutsourceIssueDetailDO> selectListByLineId(Long lineId) {
        return selectList(MesWmOutsourceIssueDetailDO::getLineId, lineId);
    }

    default void deleteByIssueId(Long issueId) {
        delete(MesWmOutsourceIssueDetailDO::getIssueId, issueId);
    }

    default void deleteByLineId(Long lineId) {
        delete(MesWmOutsourceIssueDetailDO::getLineId, lineId);
    }

}
