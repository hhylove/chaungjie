package com.chuangjie.module.hradmin.dal.dataobject.employee;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.chuangjie.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 员工人事档案；账号及企微镜像由 System 模块维护。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hradmin_employee")
public class EmployeeArchiveDO extends TenantBaseDO {
    @TableId
    private Long id;
    private String employeeNo;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long userId;
    private String name;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long deptId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String positionName;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long managerUserId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate hireDate;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate probationEndDate;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate contractEndDate;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String projectName;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer contractStatus;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer socialStatus;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String socialReason;
    /** 0 待入职，1 在职，2 离职交接，3 已离职；不代表账号状态。 */
    private Integer employmentStatus;
    private LocalDateTime accountActivatedAt;
    private Long accountActivatedBy;
    private String accountActivationEvidence;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;
}
