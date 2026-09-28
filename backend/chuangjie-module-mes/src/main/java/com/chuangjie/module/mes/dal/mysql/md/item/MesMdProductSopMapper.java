package com.chuangjie.module.mes.dal.mysql.md.item;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.mes.controller.admin.md.item.vo.sop.MesMdProductSopPageReqVO;
import com.chuangjie.module.mes.dal.dataobject.md.item.MesMdProductSopDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 产品SOP Mapper
 *
 * @author hhy
 */
@Mapper
public interface MesMdProductSopMapper extends BaseMapperX<MesMdProductSopDO> {

    default PageResult<MesMdProductSopDO> selectPage(MesMdProductSopPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesMdProductSopDO>()
                .eq(MesMdProductSopDO::getItemId, reqVO.getItemId())
                .likeIfPresent(MesMdProductSopDO::getTitle, reqVO.getTitle())
                .orderByAsc(MesMdProductSopDO::getSort));
    }

    default List<MesMdProductSopDO> selectByItemId(Long itemId) {
        return selectList(new LambdaQueryWrapperX<MesMdProductSopDO>()
                .eq(MesMdProductSopDO::getItemId, itemId)
                .orderByAsc(MesMdProductSopDO::getSort));
    }

    default Long selectCountByItemIdAndSort(Long itemId, Integer sort, Long excludeId) {
        return selectCount(new LambdaQueryWrapperX<MesMdProductSopDO>()
                .eq(MesMdProductSopDO::getItemId, itemId)
                .eq(MesMdProductSopDO::getSort, sort)
                .neIfPresent(MesMdProductSopDO::getId, excludeId));
    }

    default void deleteByItemId(Long itemId) {
        delete(MesMdProductSopDO::getItemId, itemId);
    }

}
