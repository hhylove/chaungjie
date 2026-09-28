package com.chuangjie.module.oa.dal.mysql.contact;

import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.oa.controller.admin.contact.vo.OaContactPageReqVO;
import com.chuangjie.module.oa.dal.dataobject.contact.OaContactDO;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * OA 外部联系人 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface OaContactMapper extends BaseMapperX<OaContactDO> {

    default PageResult<OaContactDO> selectReceivedPage(OaContactPageReqVO pageReqVO, Collection<Long> contactIds) {
        return selectPage(pageReqVO, buildPageQuery(pageReqVO).in(OaContactDO::getId, contactIds));
    }

    default LambdaQueryWrapperX<OaContactDO> buildPageQuery(OaContactPageReqVO pageReqVO) {
        LambdaQueryWrapperX<OaContactDO> query = new LambdaQueryWrapperX<>();
        query.likeRightIfPresent(OaContactDO::getPinyin, StrUtil.isBlank(pageReqVO.getAlphabet()) ? null
                        : pageReqVO.getAlphabet().toLowerCase(Locale.ROOT))
                .orderByAsc(OaContactDO::getPinyin).orderByAsc(OaContactDO::getId);
        if (StrUtil.isNotBlank(pageReqVO.getKeyword())) {
            query.and(wrapper -> wrapper.like(OaContactDO::getName, pageReqVO.getKeyword())
                    .or().like(OaContactDO::getMobile, pageReqVO.getKeyword())
                    .or().like(OaContactDO::getCompanyName, pageReqVO.getKeyword())
                    .or().like(OaContactDO::getPinyin, pageReqVO.getKeyword()));
        }
        return query;
    }

    default List<OaContactDO> selectListByCreator(String creator) {
        return selectList(OaContactDO::getCreator, creator);
    }

    default void updateForSave(OaContactDO contact) {
        update(contact, new LambdaUpdateWrapper<OaContactDO>()
                .eq(OaContactDO::getId, contact.getId())
                .set(contact.getCategoryId() == null, OaContactDO::getCategoryId, null)
                .set(contact.getSex() == null, OaContactDO::getSex, null)
                .set(contact.getMobile() == null, OaContactDO::getMobile, null)
                .set(contact.getEmail() == null, OaContactDO::getEmail, null)
                .set(contact.getAddress() == null, OaContactDO::getAddress, null)
                .set(contact.getCompanyName() == null, OaContactDO::getCompanyName, null)
                .set(contact.getCompanyPhone() == null, OaContactDO::getCompanyPhone, null)
                .set(contact.getAvatar() == null, OaContactDO::getAvatar, null)
                .set(contact.getRemark() == null, OaContactDO::getRemark, null));
    }

    default void updateCategoryIdToNullByCategoryId(Long categoryId) {
        update(new LambdaUpdateWrapper<OaContactDO>()
                .eq(OaContactDO::getCategoryId, categoryId)
                .set(OaContactDO::getCategoryId, null));
    }

}
