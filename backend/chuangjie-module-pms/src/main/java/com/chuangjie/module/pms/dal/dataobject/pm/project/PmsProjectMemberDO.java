package com.chuangjie.module.pms.dal.dataobject.pm.project;

import com.chuangjie.framework.mybatis.core.dataobject.BaseDO;
import com.chuangjie.module.pms.enums.pm.project.PmsProjectMemberLevelEnum;
import com.chuangjie.module.system.dal.dataobject.user.AdminUserDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * PMS 项目成员 DO
 *
 * @author hhy
 */
@TableName("pms_project_member")
@KeySequence("pms_project_member_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PmsProjectMemberDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 项目编号
     *
     * 关联 {@link PmsProjectDO#getId()}
     */
    private Long projectId;
    /**
     * 后台用户编号
     *
     * 关联 {@link AdminUserDO#getId()}
     */
    private Long userId;
    /**
     * 成员权限级别
     *
     * 枚举 {@link PmsProjectMemberLevelEnum}
     */
    private Integer level;

}
