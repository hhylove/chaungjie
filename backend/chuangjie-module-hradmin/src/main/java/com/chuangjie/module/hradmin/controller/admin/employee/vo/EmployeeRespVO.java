package com.chuangjie.module.hradmin.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "管理后台 - 人员档案")
public class EmployeeRespVO {
    private Long id;
    private String employeeNo;
    private Long userId;
    private String name;
    private Long deptId;
    private String positionName;
    private Long managerUserId;
    private LocalDate hireDate;
    private LocalDate probationEndDate;
    private LocalDate contractEndDate;
    private String projectName;
    private Integer contractStatus;
    private Integer socialStatus;
    private String socialReason;
    private Integer employmentStatus;
    private String remark;
    private LocalDateTime createTime;
    /** 以下字段来自系统账号，仅展示，不由档案修改。 */
    private String accountName;
    private Long accountDeptId;
    private Integer accountStatus;
}
