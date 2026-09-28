package com.chuangjie.module.mes.dal.mysql.wm.stocktaking.task;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.mes.controller.admin.wm.stocktaking.task.vo.result.MesWmStockTakingTaskResultPageReqVO;
import com.chuangjie.module.mes.dal.dataobject.wm.stocktaking.task.MesWmStockTakingTaskResultDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 盘点结果 Mapper
 *
 * @author hhy
 */
@Mapper
public interface MesWmStockTakingTaskResultMapper extends BaseMapperX<MesWmStockTakingTaskResultDO> {

    default PageResult<MesWmStockTakingTaskResultDO> selectPage(MesWmStockTakingTaskResultPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesWmStockTakingTaskResultDO>()
                .eqIfPresent(MesWmStockTakingTaskResultDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(MesWmStockTakingTaskResultDO::getItemId, reqVO.getItemId())
                .eqIfPresent(MesWmStockTakingTaskResultDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(MesWmStockTakingTaskResultDO::getLocationId, reqVO.getLocationId())
                .eqIfPresent(MesWmStockTakingTaskResultDO::getAreaId, reqVO.getAreaId())
                .orderByDesc(MesWmStockTakingTaskResultDO::getId));
    }

    default void deleteByTaskId(Long taskId) {
        delete(MesWmStockTakingTaskResultDO::getTaskId, taskId);
    }

}
