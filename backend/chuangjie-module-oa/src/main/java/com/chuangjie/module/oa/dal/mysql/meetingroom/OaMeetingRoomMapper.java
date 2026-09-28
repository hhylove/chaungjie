package com.chuangjie.module.oa.dal.mysql.meetingroom;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.oa.controller.admin.meetingroom.vo.room.OaMeetingRoomPageReqVO;
import com.chuangjie.module.oa.dal.dataobject.meetingroom.OaMeetingRoomDO;
import com.chuangjie.module.oa.enums.meetingroom.OaMeetingRoomBookingScopeEnum;
import com.chuangjie.module.oa.enums.meetingroom.OaMeetingRoomStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * OA 会议室 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface OaMeetingRoomMapper extends BaseMapperX<OaMeetingRoomDO> {

    default PageResult<OaMeetingRoomDO> selectPage(OaMeetingRoomPageReqVO reqVO, Collection<Long> managerIds, Long bookingUserId) {
        LambdaQueryWrapperX<OaMeetingRoomDO> query = new LambdaQueryWrapperX<OaMeetingRoomDO>()
                .likeIfPresent(OaMeetingRoomDO::getName, reqVO.getName())
                .likeIfPresent(OaMeetingRoomDO::getLocation, reqVO.getLocation())
                .eqIfPresent(OaMeetingRoomDO::getType, reqVO.getType())
                .eqIfPresent(OaMeetingRoomDO::getStatus, reqVO.getStatus())
                .inIfPresent(OaMeetingRoomDO::getManagerUserId, managerIds);
        query.orderByAsc(OaMeetingRoomDO::getSort).orderByDesc(OaMeetingRoomDO::getId);
        if (bookingUserId != null) {
            // 可预定范围在分页前过滤，保证总数和每页记录一致
            query.eq(OaMeetingRoomDO::getAllowBooking, true)
                    .eq(OaMeetingRoomDO::getStatus, OaMeetingRoomStatusEnum.NORMAL.getStatus())
                    .and(scope -> scope.eq(OaMeetingRoomDO::getBookingScope, OaMeetingRoomBookingScopeEnum.ALL.getScope())
                            .or(specified -> specified.eq(OaMeetingRoomDO::getBookingScope, OaMeetingRoomBookingScopeEnum.SPECIFIED.getScope())
                                    .apply("JSON_CONTAINS(booking_user_ids, {0})", bookingUserId.toString())));
        }
        return selectPage(reqVO, query);
    }

    default List<OaMeetingRoomDO> selectListByName(String name) {
        return selectList(new LambdaQueryWrapperX<OaMeetingRoomDO>().likeIfPresent(OaMeetingRoomDO::getName, name)
                .orderByAsc(OaMeetingRoomDO::getSort).orderByDesc(OaMeetingRoomDO::getId));
    }

}
