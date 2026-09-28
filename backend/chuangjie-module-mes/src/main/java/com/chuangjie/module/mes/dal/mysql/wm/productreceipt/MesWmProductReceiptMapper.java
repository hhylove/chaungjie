package com.chuangjie.module.mes.dal.mysql.wm.productreceipt;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.mes.controller.admin.wm.productreceipt.vo.MesWmProductReceiptPageReqVO;
import com.chuangjie.module.mes.dal.dataobject.wm.productreceipt.MesWmProductReceiptDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 产品收货单 Mapper
 */
@Mapper
public interface MesWmProductReceiptMapper extends BaseMapperX<MesWmProductReceiptDO> {

    default PageResult<MesWmProductReceiptDO> selectPage(MesWmProductReceiptPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesWmProductReceiptDO>()
                .likeIfPresent(MesWmProductReceiptDO::getCode, reqVO.getCode())
                .likeIfPresent(MesWmProductReceiptDO::getName, reqVO.getName())
                .eqIfPresent(MesWmProductReceiptDO::getWorkOrderId, reqVO.getWorkOrderId())
                .eqIfPresent(MesWmProductReceiptDO::getItemId, reqVO.getItemId())
                .orderByDesc(MesWmProductReceiptDO::getId));
    }

    default MesWmProductReceiptDO selectByCode(String code) {
        return selectOne(MesWmProductReceiptDO::getCode, code);
    }

}
