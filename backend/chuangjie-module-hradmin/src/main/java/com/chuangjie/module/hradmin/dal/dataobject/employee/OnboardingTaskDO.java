package com.chuangjie.module.hradmin.dal.dataobject.employee;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.chuangjie.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hradmin_onboarding_task")
public class OnboardingTaskDO extends TenantBaseDO {
    @TableId private Long id;
    private Long employeeId;
    private Integer stageNo;
    private Integer sequenceNo;
    private String stage;
    private String taskKey;
    private String title;
    private String owner;
    private LocalDate dueDate;
    private LocalDateTime doneAt;
    private String evidence;
    private Long actorUserId;
}
