package com.chuangjie.module.pay.convert.wallet;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.module.pay.controller.admin.wallet.vo.transaction.PayWalletTransactionRespVO;
import com.chuangjie.module.pay.controller.app.wallet.vo.transaction.AppPayWalletTransactionRespVO;
import com.chuangjie.module.pay.dal.dataobject.wallet.PayWalletTransactionDO;
import com.chuangjie.module.pay.service.wallet.bo.WalletTransactionCreateReqBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PayWalletTransactionConvert {

    PayWalletTransactionConvert INSTANCE = Mappers.getMapper(PayWalletTransactionConvert.class);

    PageResult<PayWalletTransactionRespVO> convertPage2(PageResult<PayWalletTransactionDO> page);

    PayWalletTransactionDO convert(WalletTransactionCreateReqBO bean);

}
