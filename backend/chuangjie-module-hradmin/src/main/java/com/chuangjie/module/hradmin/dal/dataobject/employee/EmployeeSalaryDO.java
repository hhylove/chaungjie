package com.chuangjie.module.hradmin.dal.dataobject.employee;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.chuangjie.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hradmin_employee_salary")
public class EmployeeSalaryDO extends TenantBaseDO {
    @TableId private Long id;
    private Long employeeId;
    private BigDecimal agreedMonthlySalary;
    private String status;
}
