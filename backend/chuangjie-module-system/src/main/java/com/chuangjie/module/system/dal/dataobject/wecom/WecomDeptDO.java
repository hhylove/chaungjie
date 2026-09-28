package com.chuangjie.module.system.dal.dataobject.wecom;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.chuangjie.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 企业微信部门镜像。企微部门 ID 只在对应企业内唯一。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_wecom_dept")
public class WecomDeptDO extends TenantBaseDO {
    @TableId
    private Long id;
    private Long wecomDeptId;
    private Long systemDeptId;
    private Long parentWecomDeptId;
    private String name;
    private String leaderUserIds;
    private Integer sort;
    private LocalDateTime lastSeenAt;
}
