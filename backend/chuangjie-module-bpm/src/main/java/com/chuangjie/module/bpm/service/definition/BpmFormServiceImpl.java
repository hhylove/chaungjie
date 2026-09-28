package com.chuangjie.module.bpm.service.definition;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.common.util.json.JsonUtils;
import com.chuangjie.framework.common.util.object.BeanUtils;
import com.chuangjie.module.bpm.controller.admin.definition.vo.form.BpmFormPageReqVO;
import com.chuangjie.module.bpm.controller.admin.definition.vo.form.BpmFormSaveReqVO;
import com.chuangjie.module.bpm.dal.dataobject.definition.BpmFormDO;
import com.chuangjie.module.bpm.dal.mysql.definition.BpmFormMapper;
import com.chuangjie.module.bpm.enums.ErrorCodeConstants;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.*;

import static com.chuangjie.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 动态表单 Service 实现类
 *
 * @author 风里雾里
 */
@Service
@Validated
public class BpmFormServiceImpl implements BpmFormService {

    @Resource
    private BpmFormMapper formMapper;

    @Override
    public Long createForm(BpmFormSaveReqVO createReqVO) {
        this.validateFields(createReqVO.getFields());
        // 插入
        BpmFormDO form = BeanUtils.toBean(createReqVO, BpmFormDO.class);
        formMapper.insert(form);
        // 返回
        return form.getId();
    }

    @Override
    public void updateForm(BpmFormSaveReqVO updateReqVO) {
        validateFields(updateReqVO.getFields());
        // 校验存在
        validateFormExists(updateReqVO.getId());
        // 更新
        BpmFormDO updateObj = BeanUtils.toBean(updateReqVO, BpmFormDO.class);
        formMapper.updateById(updateObj);
    }

    @Override
    public void deleteForm(Long id) {
        // 校验存在
        this.validateFormExists(id);
        // 删除
        formMapper.deleteById(id);
    }

    private void validateFormExists(Long id) {
        if (formMapper.selectById(id) == null) {
            throw exception(ErrorCodeConstants.FORM_NOT_EXISTS);
        }
    }

    @Override
    public BpmFormDO getForm(Long id) {
        return formMapper.selectById(id);
    }

    @Override
    public List<BpmFormDO> getFormList() {
        return formMapper.selectList();
    }

    @Override
    public List<BpmFormDO> getFormList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return formMapper.selectByIds(ids);
    }

    @Override
    public PageResult<BpmFormDO> getFormPage(BpmFormPageReqVO pageReqVO) {
        return formMapper.selectPage(pageReqVO);
    }

    /**
     * 校验 Field，避免 field 重复
     *
     * @param fields field 数组
     */
    private void validateFields(List<String> fields) {
        if (CollUtil.isEmpty(fields)) {
            return;
        }
        Map<String, String> fieldMap = new HashMap<>(); // key 是 vModel，value 是 label
        fields.forEach(field -> validateField(JsonUtils.parseObject(field, JsonNode.class), fieldMap));
    }

    private void validateField(JsonNode fieldNode, Map<String, String> fieldMap) {
        Assert.notNull(fieldNode, "表单字段配置不能为空");
        JsonNode children = fieldNode.get("children");
        if (children != null && children.isArray()) {
            children.forEach(child -> {
                if (child.isObject()) {
                    validateField(child, fieldMap);
                }
            });
        }

        // Vue3 表单使用 field/title，兼容旧版 vModel/label。
        String field = JsonUtils.getText(fieldNode, "field");
        if (StrUtil.isBlank(field)) {
            field = JsonUtils.getText(fieldNode, "vModel");
        }
        if (StrUtil.isBlank(field)) {
            return; // 布局、说明文字等展示组件没有数据字段
        }
        String label = JsonUtils.getText(fieldNode, "title");
        if (StrUtil.isBlank(label)) {
            label = JsonUtils.getText(fieldNode, "label");
        }
        String oldLabel = fieldMap.putIfAbsent(field, StrUtil.blankToDefault(label, field));
        if (oldLabel != null) {
            throw exception(ErrorCodeConstants.FORM_FIELD_REPEAT, oldLabel,
                    StrUtil.blankToDefault(label, field), field);
        }
    }

}
