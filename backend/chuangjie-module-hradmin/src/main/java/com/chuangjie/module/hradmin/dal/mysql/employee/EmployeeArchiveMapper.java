package com.chuangjie.module.hradmin.dal.mysql.employee;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.hradmin.controller.admin.employee.vo.EmployeePageReqVO;
import com.chuangjie.module.hradmin.dal.dataobject.employee.EmployeeArchiveDO;
import org.apache.ibatis.annotations.Mapper;
import java.time.LocalDate;

@Mapper
public interface EmployeeArchiveMapper extends BaseMapperX<EmployeeArchiveDO> {
    default EmployeeArchiveDO selectByEmployeeNo(String employeeNo) {
        return selectOne(EmployeeArchiveDO::getEmployeeNo, employeeNo);
    }
    default EmployeeArchiveDO selectByUserId(Long userId) {
        return selectOne(EmployeeArchiveDO::getUserId, userId);
    }
    default PageResult<EmployeeArchiveDO> selectPage(EmployeePageReqVO reqVO) {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"));
        LambdaQueryWrapperX<EmployeeArchiveDO> query = new LambdaQueryWrapperX<EmployeeArchiveDO>()
                .likeIfPresent(EmployeeArchiveDO::getName, reqVO.getName())
                .likeIfPresent(EmployeeArchiveDO::getEmployeeNo, reqVO.getEmployeeNo())
                .eqIfPresent(EmployeeArchiveDO::getEmploymentStatus, reqVO.getEmploymentStatus())
                .eqIfPresent(EmployeeArchiveDO::getDeptId, reqVO.getDeptId());
        if (Boolean.TRUE.equals(reqVO.getAttentionOnly())) {
            query.ne(EmployeeArchiveDO::getEmploymentStatus, 3)
                    .and(group -> group.lt(EmployeeArchiveDO::getContractEndDate, today)
                            .or().eq(EmployeeArchiveDO::getSocialStatus, 0));
        }
        return selectPage(reqVO, query.orderByDesc(EmployeeArchiveDO::getId));
    }
}
