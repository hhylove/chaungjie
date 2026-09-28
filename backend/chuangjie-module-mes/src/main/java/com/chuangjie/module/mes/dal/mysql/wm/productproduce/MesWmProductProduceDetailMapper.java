package com.chuangjie.module.mes.dal.mysql.wm.productproduce;

import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.module.mes.dal.dataobject.wm.productproduce.MesWmProductProduceDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 生产入库明细 Mapper
 *
 * @author hhy
 */
@Mapper
public interface MesWmProductProduceDetailMapper extends BaseMapperX<MesWmProductProduceDetailDO> {

    default List<MesWmProductProduceDetailDO> selectListByLineId(Long lineId) {
        return selectList(MesWmProductProduceDetailDO::getLineId, lineId);
    }

}
