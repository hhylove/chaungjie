package com.chuangjie.module.hrm.dal.mysql.recruit.config;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.hrm.controller.admin.recruit.vo.channel.HrmRecruitChannelPageReqVO;
import com.chuangjie.module.hrm.dal.dataobject.recruit.config.HrmRecruitChannelDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HrmRecruitChannelMapper extends BaseMapperX<HrmRecruitChannelDO> {

    default PageResult<HrmRecruitChannelDO> selectPage(HrmRecruitChannelPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrmRecruitChannelDO>()
                .likeIfPresent(HrmRecruitChannelDO::getName, reqVO.getName())
                .eqIfPresent(HrmRecruitChannelDO::getStatus, reqVO.getStatus())
                .orderByAsc(HrmRecruitChannelDO::getSort)
                .orderByDesc(HrmRecruitChannelDO::getId));
    }

    default List<HrmRecruitChannelDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<HrmRecruitChannelDO>()
                .eq(HrmRecruitChannelDO::getStatus, status)
                .orderByAsc(HrmRecruitChannelDO::getSort)
                .orderByDesc(HrmRecruitChannelDO::getId));
    }

}
