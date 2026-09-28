package com.chuangjie.module.mes.dal.mysql.wm.transfer;

import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.mes.dal.dataobject.wm.transfer.MesWmTransferLineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 转移单行 Mapper
 */
@Mapper
public interface MesWmTransferLineMapper extends BaseMapperX<MesWmTransferLineDO> {

    default List<MesWmTransferLineDO> selectListByTransferId(Long transferId) {
        return selectList(new LambdaQueryWrapperX<MesWmTransferLineDO>()
                .eq(MesWmTransferLineDO::getTransferId, transferId)
                .orderByAsc(MesWmTransferLineDO::getId));
    }

    default void deleteByTransferId(Long transferId) {
        delete(MesWmTransferLineDO::getTransferId, transferId);
    }

}
