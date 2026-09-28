package com.chuangjie.module.mes.dal.mysql.dv.subject;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.mes.controller.admin.dv.subject.vo.MesDvSubjectPageReqVO;
import com.chuangjie.module.mes.dal.dataobject.dv.subject.MesDvSubjectDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesDvSubjectMapper extends BaseMapperX<MesDvSubjectDO> {

    default PageResult<MesDvSubjectDO> selectPage(MesDvSubjectPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesDvSubjectDO>()
                .likeIfPresent(MesDvSubjectDO::getCode, reqVO.getCode())
                .likeIfPresent(MesDvSubjectDO::getName, reqVO.getName())
                .eqIfPresent(MesDvSubjectDO::getType, reqVO.getType())
                .eqIfPresent(MesDvSubjectDO::getStatus, reqVO.getStatus())
                .orderByDesc(MesDvSubjectDO::getId));
    }

    default MesDvSubjectDO selectByCode(String code) {
        return selectOne(MesDvSubjectDO::getCode, code);
    }

}
