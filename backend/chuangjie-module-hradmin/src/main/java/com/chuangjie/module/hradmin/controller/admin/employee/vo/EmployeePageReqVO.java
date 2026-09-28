package com.chuangjie.module.hradmin.controller.admin.employee.vo;

import com.chuangjie.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理后台 - 人员档案分页请求")
public class EmployeePageReqVO extends PageParam {
    private String name;
    private String employeeNo;
    private Long deptId;
    private Integer employmentStatus;
    private Boolean attentionOnly;
}
