package com.chuangjie.module.hradmin.dal.dataobject.recruitment;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.chuangjie.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hradmin_candidate")
public class CandidateDO extends TenantBaseDO {
    @TableId private Long id;
    private Long requestId;
    private String name;
    private String stage;
    private String latestNote;
    private Long employeeId;
}
