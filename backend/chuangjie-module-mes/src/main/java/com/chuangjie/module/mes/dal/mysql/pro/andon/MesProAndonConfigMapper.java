package com.chuangjie.module.mes.dal.mysql.pro.andon;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.mes.controller.admin.pro.andon.vo.config.MesProAndonConfigPageReqVO;
import com.chuangjie.module.mes.dal.dataobject.pro.andon.MesProAndonConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 安灯呼叫配置 Mapper
 *
 * @author hhy
 */
@Mapper
public interface MesProAndonConfigMapper extends BaseMapperX<MesProAndonConfigDO> {

    default PageResult<MesProAndonConfigDO> selectPage(MesProAndonConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProAndonConfigDO>()
                .likeIfPresent(MesProAndonConfigDO::getReason, reqVO.getReason())
                .eqIfPresent(MesProAndonConfigDO::getLevel, reqVO.getLevel())
                .orderByDesc(MesProAndonConfigDO::getId));
    }

}
