package com.chuangjie.module.hradmin.dal.mysql.employee;

import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.module.hradmin.dal.dataobject.employee.EmployeeSalaryDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeSalaryMapper extends BaseMapperX<EmployeeSalaryDO> {
    default EmployeeSalaryDO selectByEmployeeId(Long employeeId) {
        return selectOne(EmployeeSalaryDO::getEmployeeId, employeeId);
    }
}
