package com.chuangjie.module.system.dal.mysql.wecom;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.module.system.dal.dataobject.wecom.WecomSyncRunDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WecomSyncRunMapper extends BaseMapperX<WecomSyncRunDO> {
    default WecomSyncRunDO selectLatest() {
        return selectOne(new LambdaQueryWrapper<WecomSyncRunDO>()
                .orderByDesc(WecomSyncRunDO::getId).last("LIMIT 1"));
    }
}
