package com.chuangjie.module.crm.service.permission.bo;

import com.chuangjie.framework.common.validation.InEnum;
import com.chuangjie.module.crm.enums.common.CrmBizTypeEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * CRM 负责人变更记录 Create Req BO
 *
 * @author hhy
 */
@Data
public class CrmOwnerRecordCreateReqBO {

    /**
     * CRM 业务类型
     */
    @NotNull(message = "CRM 业务类型不能为空")
    @InEnum(CrmBizTypeEnum.class)
    private Integer bizType;

    /**
     * CRM 业务编号
     */
    @NotNull(message = "CRM 业务编号不能为空")
    private Long bizId;

    /**
     * 变更前负责人
     */
    private Long preOwnerUserId;

    /**
     * 变更后负责人
     */
    private Long postOwnerUserId;

}
