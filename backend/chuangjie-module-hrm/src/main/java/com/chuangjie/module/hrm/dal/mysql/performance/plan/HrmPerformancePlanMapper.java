package com.chuangjie.module.hrm.dal.mysql.performance.plan;

import cn.hutool.core.map.MapUtil;
import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.common.util.collection.CollectionUtils;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.framework.mybatis.core.query.QueryWrapperX;
import com.chuangjie.module.hrm.controller.admin.performance.vo.plan.HrmPerformancePlanPageReqVO;
import com.chuangjie.module.hrm.dal.dataobject.performance.plan.HrmPerformancePlanDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface HrmPerformancePlanMapper extends BaseMapperX<HrmPerformancePlanDO> {

    default PageResult<HrmPerformancePlanDO> selectPage(HrmPerformancePlanPageReqVO reqVO) {
        return selectPage(reqVO, buildQueryWrapper(reqVO)
                .eqIfPresent("status", reqVO.getStatus())
                .orderByDesc("id"));
    }

    default Map<Integer, Long> selectCountMapByStatus(HrmPerformancePlanPageReqVO reqVO) {
        QueryWrapperX<HrmPerformancePlanDO> query = buildQueryWrapper(reqVO);
        query.select("status", "COUNT(id) AS count").groupBy("status");
        return CollectionUtils.convertMap(selectMaps(query),
                record -> MapUtil.getInt(record, "status"),
                record -> MapUtil.getLong(record, "count"));
    }

    default HrmPerformancePlanDO selectByName(String name) {
        return selectFirstOne(HrmPerformancePlanDO::getName, name);
    }

    default List<HrmPerformancePlanDO> selectListByPaidForMonth(String paidForMonth) {
        return selectList(new LambdaQueryWrapperX<HrmPerformancePlanDO>()
                .eq(HrmPerformancePlanDO::getPaidForMonth, paidForMonth)
                .orderByDesc(HrmPerformancePlanDO::getId));
    }

    @SuppressWarnings("UnusedReturnValue")
    default int updateStageTypeAndOperationTypeById(Long id, Integer stageType, Integer operationType) {
        return update(new LambdaUpdateWrapper<HrmPerformancePlanDO>()
                .set(HrmPerformancePlanDO::getStageType, stageType)
                .set(HrmPerformancePlanDO::getOperationType, operationType)
                .eq(HrmPerformancePlanDO::getId, id));
    }

    static QueryWrapperX<HrmPerformancePlanDO> buildQueryWrapper(HrmPerformancePlanPageReqVO reqVO) {
        return new QueryWrapperX<HrmPerformancePlanDO>()
                .likeIfPresent("name", reqVO.getName())
                .eqIfPresent("stage_type", reqVO.getStageType());
    }

}
