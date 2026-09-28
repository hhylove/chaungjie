package com.chuangjie.module.crm.dal.mysql.customer;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.framework.mybatis.core.util.MyBatisUtils;
import com.chuangjie.module.crm.controller.admin.customer.vo.limitconfig.CrmCustomerLimitConfigPageReqVO;
import com.chuangjie.module.crm.dal.dataobject.customer.CrmCustomerLimitConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 客户限制配置 Mapper
 *
 * @author Wanwan
 */
@Mapper
public interface CrmCustomerLimitConfigMapper extends BaseMapperX<CrmCustomerLimitConfigDO> {

    default PageResult<CrmCustomerLimitConfigDO> selectPage(CrmCustomerLimitConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CrmCustomerLimitConfigDO>()
                .eqIfPresent(CrmCustomerLimitConfigDO::getType, reqVO.getType())
                .orderByDesc(CrmCustomerLimitConfigDO::getId));
    }

    default List<CrmCustomerLimitConfigDO> selectListByTypeAndUserIdAndDeptId(
            Integer type, Long userId, Long deptId) {
        LambdaQueryWrapperX<CrmCustomerLimitConfigDO> query = new LambdaQueryWrapperX<CrmCustomerLimitConfigDO>()
                .eq(CrmCustomerLimitConfigDO::getType, type);
        query.and(w -> {
            w.apply(MyBatisUtils.findInSet("user_ids"), userId);
            if (deptId != null) {
                w.or().apply(MyBatisUtils.findInSet("dept_ids"), deptId);
            }
        });
        return selectList(query);
    }

}
