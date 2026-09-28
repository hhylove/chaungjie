package com.chuangjie.module.mes.dal.mysql.dv.machinery;

import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.mes.controller.admin.dv.machinery.vo.type.MesDvMachineryTypeListReqVO;
import com.chuangjie.module.mes.dal.dataobject.dv.machinery.MesDvMachineryTypeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 设备类型 Mapper
 *
 * @author hhy
 */
@Mapper
public interface MesDvMachineryTypeMapper extends BaseMapperX<MesDvMachineryTypeDO> {

    default List<MesDvMachineryTypeDO> selectList(MesDvMachineryTypeListReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<MesDvMachineryTypeDO>()
                .likeIfPresent(MesDvMachineryTypeDO::getName, reqVO.getName())
                .eqIfPresent(MesDvMachineryTypeDO::getStatus, reqVO.getStatus())
                .orderByAsc(MesDvMachineryTypeDO::getSort)
                .orderByDesc(MesDvMachineryTypeDO::getId));
    }

    default List<MesDvMachineryTypeDO> selectList() {
        return selectList(new LambdaQueryWrapperX<MesDvMachineryTypeDO>()
                .orderByAsc(MesDvMachineryTypeDO::getSort)
                .orderByDesc(MesDvMachineryTypeDO::getId));
    }

    default MesDvMachineryTypeDO selectByParentIdAndName(Long parentId, String name) {
        return selectOne(new LambdaQueryWrapperX<MesDvMachineryTypeDO>()
                .eq(MesDvMachineryTypeDO::getParentId, parentId)
                .eq(MesDvMachineryTypeDO::getName, name));
    }

    default MesDvMachineryTypeDO selectByCode(String code) {
        return selectOne(new LambdaQueryWrapperX<MesDvMachineryTypeDO>()
                .eq(MesDvMachineryTypeDO::getCode, code));
    }

    default Long selectCountByParentId(Long parentId) {
        return selectCount(MesDvMachineryTypeDO::getParentId, parentId);
    }

}
