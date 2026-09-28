package com.chuangjie.module.oa.dal.mysql.supply;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.oa.dal.dataobject.supply.*;
import com.chuangjie.module.oa.controller.admin.supply.vo.apply.*;
import com.chuangjie.module.oa.controller.admin.supply.vo.issue.*;
import com.chuangjie.module.oa.controller.admin.supply.vo.item.*;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;


/**
 * OA 用品领用申请 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface OaSupplyApplyMapper extends BaseMapperX<OaSupplyApplyDO> {

    default PageResult<OaSupplyApplyDO> selectPage(OaSupplyApplyPageReqVO reqVO, Long userId) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OaSupplyApplyDO>()
                .eq(OaSupplyApplyDO::getCreator, userId.toString())
                .likeIfPresent(OaSupplyApplyDO::getNo, reqVO.getNo())
                .eqIfPresent(OaSupplyApplyDO::getStatus, reqVO.getStatus())
                .eqIfPresent(OaSupplyApplyDO::getDeptId, reqVO.getDeptId())
                .betweenIfPresent(OaSupplyApplyDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(OaSupplyApplyDO::getId));
    }

    default OaSupplyApplyDO selectByNo(String no) {
        return selectOne(OaSupplyApplyDO::getNo, no);
    }

    default int updateStatusAndClearProcessInstanceId(Long id, Integer oldStatus, Integer status) {
        return update(new LambdaUpdateWrapper<OaSupplyApplyDO>().eq(OaSupplyApplyDO::getId, id)
                .eq(OaSupplyApplyDO::getStatus, oldStatus)
                .set(OaSupplyApplyDO::getStatus, status).set(OaSupplyApplyDO::getProcessInstanceId, null));
    }

}
