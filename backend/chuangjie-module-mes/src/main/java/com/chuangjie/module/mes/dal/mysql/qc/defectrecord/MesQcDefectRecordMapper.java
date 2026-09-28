package com.chuangjie.module.mes.dal.mysql.qc.defectrecord;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.mes.controller.admin.qc.defectrecord.vo.MesQcDefectRecordPageReqVO;
import com.chuangjie.module.mes.dal.dataobject.qc.defectrecord.MesQcDefectRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 质检缺陷记录 Mapper
 *
 * @author hhy
 */
@Mapper
public interface MesQcDefectRecordMapper extends BaseMapperX<MesQcDefectRecordDO> {

    default PageResult<MesQcDefectRecordDO> selectPage(MesQcDefectRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesQcDefectRecordDO>()
                .eqIfPresent(MesQcDefectRecordDO::getQcType, reqVO.getQcType())
                .eqIfPresent(MesQcDefectRecordDO::getQcId, reqVO.getQcId())
                .eqIfPresent(MesQcDefectRecordDO::getLineId, reqVO.getLineId())
                .orderByDesc(MesQcDefectRecordDO::getId));
    }

    default List<MesQcDefectRecordDO> selectListByQcTypeAndQcId(Integer qcType, Long qcId) {
        return selectList(new LambdaQueryWrapperX<MesQcDefectRecordDO>()
                .eq(MesQcDefectRecordDO::getQcType, qcType)
                .eq(MesQcDefectRecordDO::getQcId, qcId));
    }

    default void deleteByQcTypeAndQcId(Integer qcType, Long qcId) {
        delete(new LambdaQueryWrapperX<MesQcDefectRecordDO>()
                .eq(MesQcDefectRecordDO::getQcType, qcType)
                .eq(MesQcDefectRecordDO::getQcId, qcId));
    }

}
