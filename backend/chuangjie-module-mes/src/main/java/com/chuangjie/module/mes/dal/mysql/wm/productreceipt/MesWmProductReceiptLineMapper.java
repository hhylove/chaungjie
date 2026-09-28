package com.chuangjie.module.mes.dal.mysql.wm.productreceipt;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.mes.controller.admin.wm.productreceipt.vo.line.MesWmProductReceiptLinePageReqVO;
import com.chuangjie.module.mes.dal.dataobject.wm.productreceipt.MesWmProductReceiptLineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 产品收货单行 Mapper
 */
@Mapper
public interface MesWmProductReceiptLineMapper extends BaseMapperX<MesWmProductReceiptLineDO> {

    default PageResult<MesWmProductReceiptLineDO> selectPage(MesWmProductReceiptLinePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesWmProductReceiptLineDO>()
                .eqIfPresent(MesWmProductReceiptLineDO::getReceiptId, reqVO.getReceiptId())
                .orderByDesc(MesWmProductReceiptLineDO::getId));
    }

    default List<MesWmProductReceiptLineDO> selectListByRecptId(Long receiptId) {
        return selectList(MesWmProductReceiptLineDO::getReceiptId, receiptId);
    }

    default void deleteByRecptId(Long receiptId) {
        delete(MesWmProductReceiptLineDO::getReceiptId, receiptId);
    }

}
