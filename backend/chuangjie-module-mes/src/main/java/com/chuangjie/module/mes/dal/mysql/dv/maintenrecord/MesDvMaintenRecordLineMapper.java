package com.chuangjie.module.mes.dal.mysql.dv.maintenrecord;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.mes.controller.admin.dv.maintenrecord.vo.line.MesDvMaintenRecordLinePageReqVO;
import com.chuangjie.module.mes.dal.dataobject.dv.maintenrecord.MesDvMaintenRecordLineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 设备保养记录明细 Mapper
 *
 * @author hhy
 */
@Mapper
public interface MesDvMaintenRecordLineMapper extends BaseMapperX<MesDvMaintenRecordLineDO> {

    default PageResult<MesDvMaintenRecordLineDO> selectPage(MesDvMaintenRecordLinePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesDvMaintenRecordLineDO>()
                .eqIfPresent(MesDvMaintenRecordLineDO::getRecordId, reqVO.getRecordId())
                .orderByDesc(MesDvMaintenRecordLineDO::getId));
    }

    default List<MesDvMaintenRecordLineDO> selectListByRecordId(Long recordId) {
        return selectList(MesDvMaintenRecordLineDO::getRecordId, recordId);
    }

    default int deleteByRecordId(Long recordId) {
        return delete(MesDvMaintenRecordLineDO::getRecordId, recordId);
    }

}
