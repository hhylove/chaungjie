package com.chuangjie.module.oa.dal.mysql.mail;

import com.chuangjie.framework.mybatis.core.mapper.BaseMapperX;
import com.chuangjie.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.chuangjie.module.oa.dal.dataobject.mail.OaMailFolderDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 企业邮箱文件夹 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface OaMailFolderMapper extends BaseMapperX<OaMailFolderDO> {

    default List<OaMailFolderDO> selectListByAccountId(Long accountId) {
        return selectList(new LambdaQueryWrapperX<OaMailFolderDO>()
                .eq(OaMailFolderDO::getAccountId, accountId).orderByAsc(OaMailFolderDO::getId));
    }

}
