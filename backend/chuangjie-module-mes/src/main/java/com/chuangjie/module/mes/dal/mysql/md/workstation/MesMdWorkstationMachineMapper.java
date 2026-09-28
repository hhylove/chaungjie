package com.chuangjie.module.mes.dal.mysql.md.workstation;

import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 设备资源 Mapper
 *
 * @author hhy
 */
@Mapper
public interface MesMdWorkstationMachineMapper extends BaseMapperX<MesMdWorkstationMachineDO> {

    default List<MesMdWorkstationMachineDO> selectListByWorkstationId(Long workstationId) {
        return selectList(MesMdWorkstationMachineDO::getWorkstationId, workstationId);
    }

    default MesMdWorkstationMachineDO selectByMachineryId(Long machineryId) {
        return selectOne(MesMdWorkstationMachineDO::getMachineryId, machineryId);
    }

    default void deleteByWorkstationId(Long workstationId) {
        delete(MesMdWorkstationMachineDO::getWorkstationId, workstationId);
    }

}
