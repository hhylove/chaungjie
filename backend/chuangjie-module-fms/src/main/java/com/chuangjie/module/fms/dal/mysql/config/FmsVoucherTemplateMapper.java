package com.chuangjie.module.fms.dal.mysql.config;

import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.fms.dal.dataobject.config.FmsVoucherTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 凭证模板 Mapper
 *
 * @author hhy
 */
@Mapper
public interface FmsVoucherTemplateMapper extends BaseMapperX<FmsVoucherTemplateDO> {

    default List<FmsVoucherTemplateDO> selectListByAccountSetId(Long accountSetId) {
        return selectList(new LambdaQueryWrapperX<FmsVoucherTemplateDO>()
                .eq(FmsVoucherTemplateDO::getAccountSetId, accountSetId)
                .orderByAsc(FmsVoucherTemplateDO::getCategoryId)
                .orderByAsc(FmsVoucherTemplateDO::getId));
    }

    default Long selectCountByCategoryId(Long categoryId) {
        return selectCount(FmsVoucherTemplateDO::getCategoryId, categoryId);
    }

}
