package com.chuangjie.module.pms.dal.dataobject.kb.library;

import com.chuangjie.framework.mybatis.core.dataobject.BaseDO;
import com.chuangjie.module.pms.enums.kb.library.PmsKnowledgeLibraryMemberLevelEnum;
import com.chuangjie.module.system.dal.dataobject.dept.DeptDO;
import com.chuangjie.module.system.dal.dataobject.user.AdminUserDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * PMS 知识库成员 DO
 *
 * @author hhy
 */
@TableName("pms_knowledge_library_member")
@KeySequence("pms_knowledge_library_member_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PmsKnowledgeLibraryMemberDO extends BaseDO {

    /**
     * 成员编号
     */
    @TableId
    private Long id;
    /**
     * 知识库编号
     *
     * 关联 {@link PmsKnowledgeLibraryDO#getId()}
     */
    private Long libraryId;
    /**
     * 后台用户编号
     *
     * 关联 {@link AdminUserDO#getId()}
     */
    private Long userId;
    /**
     * 部门编号
     *
     * 关联 {@link DeptDO#getId()}
     */
    private Long deptId;
    /**
     * 成员等级
     *
     * 枚举 {@link PmsKnowledgeLibraryMemberLevelEnum}
     */
    private Integer level;

}
