package com.chuangjie.module.bpm.controller.admin.task.vo.instance;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.DateTimeFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Schema(description = "管理后台 - 流程实例导出 Response VO")
@Data
public class BpmProcessInstanceExportRespVO {

    @ExcelProperty("审批编号")
    private String id;

    @ExcelProperty("流程名称")
    private String name;

    @ExcelProperty("申请人")
    private String startUserNickname;

    @ExcelProperty("部门")
    private String startUserDeptName;

    @ExcelProperty("审批状态")
    private String statusName;

    @ExcelProperty("提交时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @ExcelProperty("完成时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private Date endTime;

}
