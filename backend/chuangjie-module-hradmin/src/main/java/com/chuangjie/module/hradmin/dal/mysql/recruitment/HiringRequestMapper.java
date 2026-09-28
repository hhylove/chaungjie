package com.chuangjie.module.hradmin.dal.mysql.recruitment;

import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.module.hradmin.dal.dataobject.recruitment.HiringRequestDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HiringRequestMapper extends BaseMapperX<HiringRequestDO> {
    @Select("SELECT * FROM hradmin_hiring_request WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0 FOR UPDATE")
    HiringRequestDO lockById(@Param("id") Long id, @Param("tenantId") Long tenantId);
    @Update("UPDATE hradmin_hiring_request SET status = #{status}, review_note = #{note} WHERE id = #{id} AND tenant_id = #{tenantId} AND status = '待审批' AND deleted = 0")
    int reviewIfPending(@Param("id") Long id, @Param("tenantId") Long tenantId,
                        @Param("status") String status, @Param("note") String note);
}
