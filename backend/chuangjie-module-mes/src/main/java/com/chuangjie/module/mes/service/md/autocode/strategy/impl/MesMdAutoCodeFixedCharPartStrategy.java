package com.chuangjie.module.mes.service.md.autocode.strategy.impl;

import cn.hutool.core.util.StrUtil;
import com.chuangjie.module.mes.dal.dataobject.md.autocode.MesMdAutoCodePartDO;
import com.chuangjie.module.mes.enums.md.autocode.MesMdAutoCodePartTypeEnum;
import com.chuangjie.module.mes.service.md.autocode.strategy.MesMdAutoCodeContext;
import com.chuangjie.module.mes.service.md.autocode.strategy.MesMdAutoCodePartStrategy;
import org.springframework.stereotype.Component;

/**
 * MES 编码规则 - 固定字符策略
 *
 * @author hhy
 */
@Component
public class MesMdAutoCodeFixedCharPartStrategy implements MesMdAutoCodePartStrategy {

    @Override
    public Integer getType() {
        return MesMdAutoCodePartTypeEnum.FIXED_CHAR.getType();
    }

    @Override
    public String generate(MesMdAutoCodePartDO part, MesMdAutoCodeContext context) {
        return StrUtil.emptyToDefault(part.getFixCharacter(), "");
    }

}
