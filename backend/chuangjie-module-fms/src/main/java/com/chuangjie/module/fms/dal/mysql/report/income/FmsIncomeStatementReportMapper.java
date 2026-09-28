package com.chuangjie.module.fms.dal.mysql.report.income;

import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.fms.dal.dataobject.report.income.FmsIncomeStatementReportDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 利润表数据 Mapper
 *
 * @author hhy
 */
@Mapper
public interface FmsIncomeStatementReportMapper extends BaseMapperX<FmsIncomeStatementReportDO> {

    default List<FmsIncomeStatementReportDO> selectListByPeriod(Long accountSetId,
                                                                Integer fromPeriod, Integer toPeriod, Integer type) {
        return selectList(new LambdaQueryWrapperX<FmsIncomeStatementReportDO>()
                .eq(FmsIncomeStatementReportDO::getAccountSetId, accountSetId)
                .eq(FmsIncomeStatementReportDO::getFromPeriod, fromPeriod)
                .eq(FmsIncomeStatementReportDO::getToPeriod, toPeriod)
                .eq(FmsIncomeStatementReportDO::getType, type)
                .orderByAsc(FmsIncomeStatementReportDO::getSort)
                .orderByAsc(FmsIncomeStatementReportDO::getId));
    }

}
