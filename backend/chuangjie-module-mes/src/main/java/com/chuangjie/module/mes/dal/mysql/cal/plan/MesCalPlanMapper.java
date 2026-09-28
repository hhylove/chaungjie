package com.chuangjie.module.mes.dal.mysql.cal.plan;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.mes.controller.admin.cal.plan.vo.MesCalPlanPageReqVO;
import com.chuangjie.module.mes.dal.dataobject.cal.plan.MesCalPlanDO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 排班计划 Mapper
 *
 * @author hhy
 */
@Mapper
public interface MesCalPlanMapper extends BaseMapperX<MesCalPlanDO> {

    default PageResult<MesCalPlanDO> selectPage(MesCalPlanPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesCalPlanDO>()
                .likeIfPresent(MesCalPlanDO::getCode, reqVO.getCode())
                .likeIfPresent(MesCalPlanDO::getName, reqVO.getName())
                .eqIfPresent(MesCalPlanDO::getShiftType, reqVO.getShiftType())
                .eqIfPresent(MesCalPlanDO::getStatus, reqVO.getStatus())
                .eqIfPresent(MesCalPlanDO::getCalendarType, reqVO.getCalendarType())
                .betweenIfPresent(MesCalPlanDO::getStartDate, reqVO.getStartDate())
                .betweenIfPresent(MesCalPlanDO::getEndDate, reqVO.getEndDate())
                .orderByDesc(MesCalPlanDO::getId));
    }

    default MesCalPlanDO selectByCode(String code) {
        return selectOne(MesCalPlanDO::getCode, code);
    }

    default void updateWithShiftFieldCleanup(MesCalPlanDO plan, boolean clearShiftMethod, boolean clearShiftCount) {
        update(plan, Wrappers.<MesCalPlanDO>lambdaUpdate()
                .eq(MesCalPlanDO::getId, plan.getId())
                .set(clearShiftMethod, MesCalPlanDO::getShiftMethod, null)
                .set(clearShiftCount, MesCalPlanDO::getShiftCount, null));
    }

}
