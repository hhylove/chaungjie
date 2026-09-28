package com.chuangjie.module.oa.dal.mysql.reimbursement;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.oa.controller.admin.reimbursement.vo.OaReimbursementPageReqVO;
import com.chuangjie.module.oa.dal.dataobject.reimbursement.OaReimbursementDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * OA 费用报销 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface OaReimbursementMapper extends BaseMapperX<OaReimbursementDO> {

    default PageResult<OaReimbursementDO> selectPage(Long userId, OaReimbursementPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OaReimbursementDO>()
                .eq(OaReimbursementDO::getCreator, userId.toString())
                .likeIfPresent(OaReimbursementDO::getTitle, reqVO.getTitle())
                .eqIfPresent(OaReimbursementDO::getStatus, reqVO.getStatus())
                .orderByDesc(OaReimbursementDO::getCreateTime, OaReimbursementDO::getId));
    }

}
