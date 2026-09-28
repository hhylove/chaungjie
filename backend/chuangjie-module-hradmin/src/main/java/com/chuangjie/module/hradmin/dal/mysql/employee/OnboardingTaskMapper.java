package com.chuangjie.module.hradmin.dal.mysql.employee;

import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.module.hradmin.dal.dataobject.employee.OnboardingTaskDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import java.util.List;
import java.time.LocalDateTime;

@Mapper
public interface OnboardingTaskMapper extends BaseMapperX<OnboardingTaskDO> {
    @Update("UPDATE hradmin_onboarding_task SET done_at = #{doneAt}, evidence = #{evidence}, actor_user_id = #{actorUserId} WHERE id = #{id} AND tenant_id = #{tenantId} AND done_at IS NULL AND deleted = 0")
    int completeIfPending(@Param("id") Long id, @Param("tenantId") Long tenantId,
                          @Param("doneAt") LocalDateTime doneAt, @Param("evidence") String evidence,
                          @Param("actorUserId") Long actorUserId);
    default List<OnboardingTaskDO> byEmployee(Long employeeId) {
        return selectList(OnboardingTaskDO::getEmployeeId, employeeId).stream()
                .sorted((a,b) -> Integer.compare(a.getSequenceNo(), b.getSequenceNo())).toList();
    }
}
