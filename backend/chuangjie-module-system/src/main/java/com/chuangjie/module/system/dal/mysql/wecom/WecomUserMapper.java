package com.chuangjie.module.system.dal.mysql.wecom;

import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.module.system.dal.dataobject.wecom.WecomUserDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WecomUserMapper extends BaseMapperX<WecomUserDO> {
    default WecomUserDO selectByWecomUserId(String id) {
        return selectOne(WecomUserDO::getWecomUserId, id);
    }
    default WecomUserDO selectBySystemUserId(Long id) {
        return selectOne(WecomUserDO::getSystemUserId, id);
    }
}

