package com.chuangjie.module.mes.dal.mysql.wm.packages;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.mes.controller.admin.wm.packages.vo.line.MesWmPackageLinePageReqVO;
import com.chuangjie.module.mes.dal.dataobject.wm.packages.MesWmPackageLineDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 装箱明细 Mapper
 *
 * @author hhy
 */
@Mapper
public interface MesWmPackageLineMapper extends BaseMapperX<MesWmPackageLineDO> {

    default PageResult<MesWmPackageLineDO> selectPage(MesWmPackageLinePageReqVO reqVO, java.util.Collection<Long> packageIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesWmPackageLineDO>()
                .inIfPresent(MesWmPackageLineDO::getPackageId, packageIds)
                .orderByDesc(MesWmPackageLineDO::getId));
    }

    default void deleteByPackageId(Long packageId) {
        delete(MesWmPackageLineDO::getPackageId, packageId);
    }

}
