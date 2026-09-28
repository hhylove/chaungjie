package com.chuangjie.module.hradmin.enums;

import com.chuangjie.framework.common.exception.ErrorCode;

/** 人事行政一期错误码。 */
public interface ErrorCodeConstants {
    ErrorCode EMPLOYEE_NOT_EXISTS = new ErrorCode(1_055_100_001, "人员档案不存在");
    ErrorCode EMPLOYEE_NO_EXISTS = new ErrorCode(1_055_100_002, "工号已存在");
    ErrorCode EMPLOYEE_USER_EXISTS = new ErrorCode(1_055_100_003, "该系统账号已有人员档案");
    ErrorCode EMPLOYEE_USER_NOT_EXISTS = new ErrorCode(1_055_100_004, "系统账号不存在或不属于当前租户");
    ErrorCode EMPLOYEE_STATUS_INVALID = new ErrorCode(1_055_100_005, "任职状态无效");
    ErrorCode EMPLOYEE_USER_LOCKED = new ErrorCode(1_055_100_006, "已关联账号不能在档案编辑中更换，请走专门的账号核对流程");
    ErrorCode EMPLOYEE_DEPT_NOT_EXISTS = new ErrorCode(1_055_100_007, "部门不存在或不属于当前租户");
    ErrorCode EMPLOYEE_MANAGER_NOT_EXISTS = new ErrorCode(1_055_100_008, "直属负责人账号不存在或不属于当前租户");
    ErrorCode EMPLOYEE_ARRIVAL_INVALID = new ErrorCode(1_055_100_009, "仅待入职且已到约定日期的人员可确认到岗");
    ErrorCode EMPLOYEE_ACCOUNT_NOT_DISABLED = new ErrorCode(1_055_100_010, "请选择尚未启用的系统账号");
    ErrorCode EMPLOYEE_ACCOUNT_IDENTITY_MISMATCH = new ErrorCode(1_055_100_011, "账号未关联同名企微成员，请先核对身份");
    ErrorCode RECRUITMENT_STATE_INVALID = new ErrorCode(1_055_100_012, "招聘事项当前阶段不允许该操作");
    ErrorCode RECRUITMENT_QUOTA_FULL = new ErrorCode(1_055_100_013, "该用人需求的录用名额已满");
    ErrorCode EMPLOYEE_SALARY_INVALID = new ErrorCode(1_055_100_014, "约定月薪须大于零");
    ErrorCode ONBOARDING_TASK_INVALID = new ErrorCode(1_055_100_015, "入职事项前置条件或办理身份不满足");
}
