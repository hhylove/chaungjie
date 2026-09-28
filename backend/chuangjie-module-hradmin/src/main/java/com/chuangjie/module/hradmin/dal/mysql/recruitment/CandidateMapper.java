package com.chuangjie.module.hradmin.dal.mysql.recruitment;

import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.module.hradmin.dal.dataobject.recruitment.CandidateDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CandidateMapper extends BaseMapperX<CandidateDO> {
    @Update("UPDATE hradmin_candidate SET stage = #{nextStage}, latest_note = #{note} WHERE id = #{id} AND tenant_id = #{tenantId} AND stage = #{currentStage} AND employee_id IS NULL AND deleted = 0")
    int advanceIfCurrent(@Param("id") Long id, @Param("tenantId") Long tenantId,
                         @Param("currentStage") String currentStage, @Param("nextStage") String nextStage,
                         @Param("note") String note);
    @Update("UPDATE hradmin_candidate SET employee_id = #{employeeId}, stage = '已转员工' WHERE id = #{id} AND tenant_id = #{tenantId} AND stage = '待入职' AND employee_id IS NULL AND deleted = 0")
    int markConverted(@Param("id") Long id, @Param("employeeId") Long employeeId, @Param("tenantId") Long tenantId);
}
