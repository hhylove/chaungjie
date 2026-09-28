package com.chuangjie.module.system.dal.dataobject.wecom;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.chuangjie.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 一次成功的企微通讯录同步。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_wecom_sync_run")
public class WecomSyncRunDO extends TenantBaseDO {
    @TableId
    private Long id;
    private Integer departmentCount;
    private Integer userCount;
}
