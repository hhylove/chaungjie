package com.chuangjie.module.system.dal.dataobject.wecom;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.chuangjie.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 企业微信成员镜像。systemUserId 仅在管理员核对后填写。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_wecom_user")
public class WecomUserDO extends TenantBaseDO {
    @TableId
    private Long id;
    private String wecomUserId;
    private String name;
    private String departmentIds;
    private String mobile;
    private String email;
    private Integer sex;
    private Long systemUserId;
    private LocalDateTime lastSeenAt;
}
