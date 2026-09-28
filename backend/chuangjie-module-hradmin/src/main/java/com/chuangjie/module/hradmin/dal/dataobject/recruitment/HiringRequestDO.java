package com.chuangjie.module.hradmin.dal.dataobject.recruitment;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.chuangjie.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hradmin_hiring_request")
public class HiringRequestDO extends TenantBaseDO {
    @TableId private Long id;
    private String job;
    private Long deptId;
    private Integer count;
    private String reason;
    private String status;
    private String reviewNote;
}
