package com.chuangjie.module.mes.dal.mysql.cal.holiday;

import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.mes.dal.dataobject.cal.holiday.MesCalHolidayDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MES 假期设置 Mapper
 *
 * @author hhy
 */
@Mapper
public interface MesCalHolidayMapper extends BaseMapperX<MesCalHolidayDO> {

    default MesCalHolidayDO selectByDay(LocalDateTime day) {
        return selectOne(MesCalHolidayDO::getDay, day);
    }

    default List<MesCalHolidayDO> selectList(LocalDateTime startDay, LocalDateTime endDay) {
        return selectList(new LambdaQueryWrapperX<MesCalHolidayDO>()
                .betweenIfPresent(MesCalHolidayDO::getDay, startDay, endDay)
                .orderByAsc(MesCalHolidayDO::getDay));
    }

}
