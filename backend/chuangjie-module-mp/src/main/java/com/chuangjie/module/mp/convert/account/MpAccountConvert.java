package com.chuangjie.module.mp.convert.account;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.module.mp.controller.admin.account.vo.MpAccountCreateReqVO;
import com.chuangjie.module.mp.controller.admin.account.vo.MpAccountRespVO;
import com.chuangjie.module.mp.controller.admin.account.vo.MpAccountSimpleRespVO;
import com.chuangjie.module.mp.controller.admin.account.vo.MpAccountUpdateReqVO;
import com.chuangjie.module.mp.dal.dataobject.account.MpAccountDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface MpAccountConvert {

    MpAccountConvert INSTANCE = Mappers.getMapper(MpAccountConvert.class);

    MpAccountDO convert(MpAccountCreateReqVO bean);

    MpAccountDO convert(MpAccountUpdateReqVO bean);

    MpAccountRespVO convert(MpAccountDO bean);

    List<MpAccountRespVO> convertList(List<MpAccountDO> list);

    PageResult<MpAccountRespVO> convertPage(PageResult<MpAccountDO> page);

    List<MpAccountSimpleRespVO> convertList02(List<MpAccountDO> list);

}
