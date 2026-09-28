package com.chuangjie.module.mes.dal.mysql.qc.indicatorresult;

import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.mes.dal.dataobject.qc.indicatorresult.MesQcIndicatorResultDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 检验结果明细记录 Mapper
 *
 * @author hhy
 */
@Mapper
public interface MesQcIndicatorResultDetailMapper extends BaseMapperX<MesQcIndicatorResultDetailDO> {

    default List<MesQcIndicatorResultDetailDO> selectListByResultId(Long resultId) {
        return selectList(MesQcIndicatorResultDetailDO::getResultId, resultId);
    }

    default void deleteByResultId(Long resultId) {
        delete(new LambdaQueryWrapperX<MesQcIndicatorResultDetailDO>()
                .eq(MesQcIndicatorResultDetailDO::getResultId, resultId));
    }

}
