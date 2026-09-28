package com.chuangjie.module.mes.dal.mysql.dv.maintenrecord;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.mes.controller.admin.dv.maintenrecord.vo.MesDvMaintenRecordPageReqVO;
import com.chuangjie.module.mes.dal.dataobject.dv.maintenrecord.MesDvMaintenRecordDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 设备保养记录 Mapper
 *
 * @author hhy
 */
@Mapper
public interface MesDvMaintenRecordMapper extends BaseMapperX<MesDvMaintenRecordDO> {

    default PageResult<MesDvMaintenRecordDO> selectPage(MesDvMaintenRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesDvMaintenRecordDO>()
                .eqIfPresent(MesDvMaintenRecordDO::getPlanId, reqVO.getPlanId())
                .eqIfPresent(MesDvMaintenRecordDO::getMachineryId, reqVO.getMachineryId())
                .eqIfPresent(MesDvMaintenRecordDO::getUserId, reqVO.getUserId())
                .betweenIfPresent(MesDvMaintenRecordDO::getMaintenTime, reqVO.getMaintenTime())
                .orderByDesc(MesDvMaintenRecordDO::getId));
    }

    default Long selectCountByMachineryId(Long machineryId) {
        return selectCount(MesDvMaintenRecordDO::getMachineryId, machineryId);
    }

}
