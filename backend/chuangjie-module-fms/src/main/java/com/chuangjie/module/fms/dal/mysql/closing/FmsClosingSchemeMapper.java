package com.chuangjie.module.fms.dal.mysql.closing;

import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.fms.dal.dataobject.closing.FmsClosingSchemeDO;
import com.chuangjie.module.fms.enums.closing.FmsClosingTypeEnum;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 结账方案 Mapper
 *
 * @author hhy
 */
@Mapper
public interface FmsClosingSchemeMapper extends BaseMapperX<FmsClosingSchemeDO> {

    default FmsClosingSchemeDO selectByAccountSetIdAndType(Long accountSetId, Integer type) {
        return selectOne(new LambdaQueryWrapperX<FmsClosingSchemeDO>()
                .eq(FmsClosingSchemeDO::getAccountSetId, accountSetId)
                .eq(FmsClosingSchemeDO::getType, type));
    }

    default FmsClosingSchemeDO selectByIdAndAccountSetId(Long id, Long accountSetId) {
        return selectOne(new LambdaQueryWrapperX<FmsClosingSchemeDO>()
                .eq(FmsClosingSchemeDO::getId, id)
                .eq(FmsClosingSchemeDO::getAccountSetId, accountSetId));
    }

    default List<FmsClosingSchemeDO> selectListByAccountSetId(Long accountSetId) {
        return selectList(new LambdaQueryWrapperX<FmsClosingSchemeDO>()
                .eq(FmsClosingSchemeDO::getAccountSetId, accountSetId)
                .orderByAsc(FmsClosingSchemeDO::getType)
                .orderByAsc(FmsClosingSchemeDO::getId));
    }

    default List<FmsClosingSchemeDO> selectPeriodEndListByAccountSetId(Long accountSetId) {
        return selectList(new LambdaQueryWrapperX<FmsClosingSchemeDO>()
                .eq(FmsClosingSchemeDO::getAccountSetId, accountSetId)
                .and(wrapper -> wrapper.ne(FmsClosingSchemeDO::getType,
                                FmsClosingTypeEnum.REGULAR.getType())
                        .or().eq(FmsClosingSchemeDO::getPeriodEnd, true))
                .orderByAsc(FmsClosingSchemeDO::getType)
                .orderByAsc(FmsClosingSchemeDO::getId));
    }

    default Long selectCountByAccountSetIdAndVoucherWordId(
            Long accountSetId, Long voucherWordId) {
        return selectCount(new LambdaQueryWrapperX<FmsClosingSchemeDO>()
                .eq(FmsClosingSchemeDO::getAccountSetId, accountSetId)
                .eq(FmsClosingSchemeDO::getVoucherWordId, voucherWordId));
    }

}
