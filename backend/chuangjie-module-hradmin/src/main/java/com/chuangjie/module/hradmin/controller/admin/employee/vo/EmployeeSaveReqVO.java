package com.chuangjie.module.hradmin.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.math.BigDecimal;

@Data
@Schema(description = "管理后台 - 创建或更新人员档案")
public class EmployeeSaveReqVO {
    private Long id;
    @Size(max = 32)
    private String employeeNo;
    /** 无账号的待入职人员可留空。 */
    private Long userId;
    @NotBlank(message = "姓名不能为空")
    @Size(max = 64)
    private String name;
    private Long deptId;
    @Size(max = 100)
    private String positionName;
    private Long managerUserId;
    private LocalDate hireDate;
    private LocalDate probationEndDate;
    private LocalDate contractEndDate;
    @Size(max = 100)
    private String projectName;
    /** 0 待签署，1 已签署；为空表示尚未登记。 */
    @Min(0) @Max(1)
    private Integer contractStatus;
    /** 0 未参保，1 已参保；为空表示尚未登记。 */
    @Min(0) @Max(1)
    private Integer socialStatus;
    @Size(max = 500)
    private String socialReason;
    @NotNull(message = "任职状态不能为空")
    private Integer employmentStatus;
    @Size(max = 1000)
    private String remark;
    /** 仅建档时写入受控薪酬表，不属于普通员工档案响应。 */
    private BigDecimal agreedMonthlySalary;
}
