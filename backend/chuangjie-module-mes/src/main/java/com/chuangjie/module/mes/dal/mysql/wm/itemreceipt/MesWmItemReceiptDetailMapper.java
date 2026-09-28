package com.chuangjie.module.mes.dal.mysql.wm.itemreceipt;

import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.module.mes.dal.dataobject.wm.itemreceipt.MesWmItemReceiptDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 采购入库明细 Mapper
 */
@Mapper
public interface MesWmItemReceiptDetailMapper extends BaseMapperX<MesWmItemReceiptDetailDO> {

    default List<MesWmItemReceiptDetailDO> selectListByReceiptId(Long receiptId) {
        return selectList(MesWmItemReceiptDetailDO::getReceiptId, receiptId);
    }

    default List<MesWmItemReceiptDetailDO> selectListByLineId(Long lineId) {
        return selectList(MesWmItemReceiptDetailDO::getLineId, lineId);
    }

    default void deleteByReceiptId(Long receiptId) {
        delete(MesWmItemReceiptDetailDO::getReceiptId, receiptId);
    }

    default void deleteByLineId(Long lineId) {
        delete(MesWmItemReceiptDetailDO::getLineId, lineId);
    }

}
