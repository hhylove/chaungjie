package com.chuangjie.module.mes.dal.mysql.cal.plan;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.mes.controller.admin.cal.plan.vo.shift.MesCalPlanShiftPageReqVO;
import com.chuangjie.module.mes.dal.dataobject.cal.plan.MesCalPlanShiftDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 计划班次 Mapper
 *
 * @author hhy
 */
@Mapper
public interface MesCalPlanShiftMapper extends BaseMapperX<MesCalPlanShiftDO> {

    default PageResult<MesCalPlanShiftDO> selectPage(MesCalPlanShiftPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesCalPlanShiftDO>()
                .eqIfPresent(MesCalPlanShiftDO::getPlanId, reqVO.getPlanId())
                .likeIfPresent(MesCalPlanShiftDO::getName, reqVO.getName())
                .orderByAsc(MesCalPlanShiftDO::getSort));
    }

    default List<MesCalPlanShiftDO> selectListByPlanId(Long planId) {
        return selectList(new LambdaQueryWrapperX<MesCalPlanShiftDO>()
                .eq(MesCalPlanShiftDO::getPlanId, planId)
                .orderByAsc(MesCalPlanShiftDO::getSort));
    }

    default Long selectCountByPlanId(Long planId) {
        return selectCount(MesCalPlanShiftDO::getPlanId, planId);
    }

    default void deleteByPlanId(Long planId) {
        delete(new LambdaQueryWrapperX<MesCalPlanShiftDO>()
                .eq(MesCalPlanShiftDO::getPlanId, planId));
    }

}
