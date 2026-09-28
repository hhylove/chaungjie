package com.chuangjie.module.oa.dal.mysql.contact;

import cn.hutool.core.util.StrUtil;
import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.query.MPJLambdaWrapperX;
import com.chuangjie.module.oa.controller.admin.contact.vo.OaContactPageReqVO;
import com.chuangjie.module.oa.dal.dataobject.contact.OaContactDO;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.oa.dal.dataobject.contact.OaContactShareDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * OA 联系人关联 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface OaContactShareMapper extends BaseMapperX<OaContactShareDO> {

    default PageResult<OaContactShareDO> selectSharedPage(OaContactPageReqVO reqVO, Long userId,
                                                          Collection<Long> contactIds) {
        MPJLambdaWrapperX<OaContactShareDO> query = new MPJLambdaWrapperX<>();
        query.selectAll(OaContactShareDO.class)
                .innerJoin(OaContactDO.class, OaContactDO::getId, OaContactShareDO::getContactId)
                .eq(OaContactDO::getDeleted, false)
                .eq(OaContactShareDO::getCreator, userId.toString())
                .ne(OaContactShareDO::getUserId, userId)
                .in(OaContactShareDO::getContactId, contactIds)
                .eq(reqVO.getHandleStatus() != null, OaContactShareDO::getHandleStatus, reqVO.getHandleStatus())
                .orderByDesc(OaContactShareDO::getCreateTime).orderByDesc(OaContactShareDO::getId);
        if (StrUtil.isNotBlank(reqVO.getAlphabet())) {
            query.likeRight(OaContactDO::getPinyin, reqVO.getAlphabet().toLowerCase(Locale.ROOT));
        }
        if (StrUtil.isNotBlank(reqVO.getKeyword())) {
            query.and(w -> w.like(OaContactDO::getName, reqVO.getKeyword())
                    .or().like(OaContactDO::getMobile, reqVO.getKeyword())
                    .or().like(OaContactDO::getCompanyName, reqVO.getKeyword())
                    .or().like(OaContactDO::getPinyin, reqVO.getKeyword()));
        }
        return selectJoinPage(reqVO, OaContactShareDO.class, query);
    }

    default List<OaContactShareDO> selectListByUserId(Long userId, Boolean handleStatus, Long categoryId) {
        return selectList(new LambdaQueryWrapperX<OaContactShareDO>()
                .eq(OaContactShareDO::getUserId, userId)
                .eqIfPresent(OaContactShareDO::getHandleStatus, handleStatus)
                .eqIfPresent(OaContactShareDO::getCategoryId, categoryId)
                .orderByAsc(OaContactShareDO::getHandleStatus)
                .orderByDesc(OaContactShareDO::getCreateTime)
                .orderByDesc(OaContactShareDO::getId));
    }

    default List<OaContactShareDO> selectListByContactId(Long contactId) {
        return selectList(OaContactShareDO::getContactId, contactId);
    }

    default List<OaContactShareDO> selectListByContactIds(Collection<Long> contactIds) {
        return selectList(new LambdaQueryWrapperX<OaContactShareDO>()
                .in(OaContactShareDO::getContactId, contactIds)
                .orderByDesc(OaContactShareDO::getCreateTime)
                .orderByDesc(OaContactShareDO::getId));
    }

    default OaContactShareDO selectByContactIdAndUserId(Long contactId, Long userId) {
        return selectOne(OaContactShareDO::getContactId, contactId, OaContactShareDO::getUserId, userId);
    }

    default void deleteByContactIdAndUserId(Long contactId, Long userId) {
        delete(new LambdaQueryWrapperX<OaContactShareDO>()
                .eq(OaContactShareDO::getContactId, contactId)
                .eq(OaContactShareDO::getUserId, userId));
    }

    default void updateHandleStatusAndCategoryIdById(OaContactShareDO updateObj) {
        update(updateObj, new LambdaUpdateWrapper<OaContactShareDO>()
                .eq(OaContactShareDO::getId, updateObj.getId())
                .set(updateObj.getCategoryId() == null, OaContactShareDO::getCategoryId, null));
    }

    default void updateCategoryIdToNullByCategoryId(Long categoryId) {
        update(new LambdaUpdateWrapper<OaContactShareDO>()
                .eq(OaContactShareDO::getCategoryId, categoryId)
                .set(OaContactShareDO::getCategoryId, null));
    }

}
