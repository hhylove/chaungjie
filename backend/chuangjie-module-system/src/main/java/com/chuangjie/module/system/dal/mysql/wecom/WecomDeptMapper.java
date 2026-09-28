package com.chuangjie.module.system.dal.mysql.wecom;

import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.module.system.dal.dataobject.wecom.WecomDeptDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WecomDeptMapper extends BaseMapperX<WecomDeptDO> {
    default WecomDeptDO selectByWecomDeptId(Long id) {
        return selectOne(WecomDeptDO::getWecomDeptId, id);
    }
    default WecomDeptDO selectBySystemDeptId(Long id) {
        return selectOne(WecomDeptDO::getSystemDeptId, id);
    }
}
