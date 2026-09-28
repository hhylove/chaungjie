package com.chuangjie.module.bpm.framework.flowable.core.util;

import com.chuangjie.framework.common.core.KeyValue;
import com.chuangjie.module.bpm.controller.admin.definition.vo.model.BpmModelMetaInfoVO;
import com.chuangjie.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import com.chuangjie.module.bpm.enums.definition.BpmModelFormTypeEnum;
import com.chuangjie.module.bpm.enums.definition.BpmFieldPermissionEnum;
import com.chuangjie.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FlowableUtils} 的单元测试。
 *
 * @author hhy
 */
class FlowableUtilsTest {

    @Test
    public void testFilterProcessInstanceFormVariable_removeSystemVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("reason", "事假");
        variables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS, 2);
        variables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_ID, 1L);
        variables.put("RETURN_FLAG_activity_1", true);
        variables.put("_FLOWABLE_CUSTOM", true);
        variables.put("NEED_SIMULATE_CUSTOM", true);

        FlowableUtils.filterProcessInstanceFormVariable(variables);

        assertEquals(Collections.singletonMap("reason", "事假"), variables);
    }

    @Test
    public void testFilterWritableProcessInstanceFormVariables_onlyWriteFields() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("editable", "new value");
        variables.put("readonly", "forged value");
        variables.put("hidden", "forged value");
        variables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS, 2);
        Map<String, String> permissions = new HashMap<>();
        permissions.put("editable", String.valueOf(BpmFieldPermissionEnum.WRITE.getPermission()));
        permissions.put("readonly", String.valueOf(BpmFieldPermissionEnum.READ.getPermission()));
        permissions.put("hidden", String.valueOf(BpmFieldPermissionEnum.NONE.getPermission()));

        Map<String, Object> result = FlowableUtils.filterWritableProcessInstanceFormVariables(variables, permissions);

        assertEquals(Collections.singletonMap("editable", "new value"), result);
        assertTrue(variables.containsKey("readonly"));
        assertFalse(result.containsKey(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS));
    }

    @Test
    public void testFilterWritableProcessInstanceFormVariables_legacyProcess() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("normalField", "value");
        variables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS, 2);

        Map<String, Object> result = FlowableUtils.filterWritableProcessInstanceFormVariables(
                variables, Collections.emptyMap());

        assertEquals(Collections.singletonMap("normalField", "value"), result);
    }

    @Test
    public void testGetProcessInstanceStartUserId() {
        // 准备参数
        Map<String, Object> processVariables = new HashMap<>();
        processVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_ID, "1");

        // 调用
        Long startUserId = FlowableUtils.getProcessInstanceStartUserId(processVariables);

        // 断言
        assertEquals(1L, startUserId);
    }

    @Test
    public void testGetProcessInstanceStartUserId_empty() {
        // 准备参数
        Map<String, Object> processVariables = new HashMap<>();

        // 调用 & 断言
        assertNull(FlowableUtils.getProcessInstanceStartUserId(processVariables));
        assertNull(FlowableUtils.getProcessInstanceStartUserId(null));
    }

    @Test
    public void testGetSummary_customSummary_parseDbFormFields() {
        // 准备参数：模拟 DB 中 form_fields 字段，列表里每个元素都是一个 form-create 字段 JSON。
        BpmProcessDefinitionInfoDO processDefinitionInfo = processDefinitionInfo(dbFormFields(),
                summarySetting(true, "reason", "days", "notExists", "startTime"));
        Map<String, Object> processVariables = processVariables();

        // 调用
        List<KeyValue<String, String>> summary = FlowableUtils.getSummary(processDefinitionInfo, processVariables);

        // 断言
        assertEquals(Arrays.asList(
                new KeyValue<>("请假原因", "事假"),
                new KeyValue<>("请假天数", "3"),
                new KeyValue<>("开始时间", "2026-05-31 09:00:00")),
                summary);
    }

    @Test
    public void testGetSummary_defaultSummary_parseFirstThreeFieldsByFormOrder() {
        // 准备参数：未开启自定义摘要时，默认取表单配置顺序里的前三个真实字段。
        BpmProcessDefinitionInfoDO processDefinitionInfo = processDefinitionInfo(dbFormFields(), null);
        Map<String, Object> processVariables = processVariables();

        // 调用
        List<KeyValue<String, String>> summary = FlowableUtils.getSummary(processDefinitionInfo, processVariables);

        // 断言
        assertEquals(Arrays.asList(
                new KeyValue<>("请假原因", "事假"),
                new KeyValue<>("开始时间", "2026-05-31 09:00:00"),
                new KeyValue<>("请假天数", "3")),
                summary);
    }

    @Test
    public void testGetSummary_summaryDisabled_useDefaultSummary() {
        // 准备参数：摘要设置存在但未启用时，仍走默认摘要逻辑。
        BpmProcessDefinitionInfoDO processDefinitionInfo = processDefinitionInfo(dbFormFields(),
                summarySetting(false, "remark"));
        Map<String, Object> processVariables = processVariables();

        // 调用
        List<KeyValue<String, String>> summary = FlowableUtils.getSummary(processDefinitionInfo, processVariables);

        // 断言
        assertEquals(Arrays.asList(
                new KeyValue<>("请假原因", "事假"),
                new KeyValue<>("开始时间", "2026-05-31 09:00:00"),
                new KeyValue<>("请假天数", "3")),
                summary);
    }

    @Test
    public void testGetSummary_displayComponentsOnly_returnEmpty() {
        // 准备参数：分割线、标签、文字等展示组件的 children 是字符串数组，不是表单字段对象。
        BpmProcessDefinitionInfoDO processDefinitionInfo = processDefinitionInfo(Arrays.asList(
                DIVIDER_FIELD,
                TEXT_FIELD,
                TAG_FIELD), null);

        // 调用
        List<KeyValue<String, String>> summary = FlowableUtils.getSummary(processDefinitionInfo,
                Collections.emptyMap());

        // 断言
        assertEquals(Collections.emptyList(), summary);
    }

    @Test
    public void testGetSummary_notNormalForm_returnNull() {
        // 准备参数
        BpmProcessDefinitionInfoDO processDefinitionInfo = BpmProcessDefinitionInfoDO.builder()
                .formType(BpmModelFormTypeEnum.CUSTOM.getType())
                .build();

        // 调用 & 断言
        assertNull(FlowableUtils.getSummary(null, Collections.emptyMap()));
        assertNull(FlowableUtils.getSummary(processDefinitionInfo, Collections.emptyMap()));
    }

    private static BpmProcessDefinitionInfoDO processDefinitionInfo(List<String> formFields,
                                                                    BpmModelMetaInfoVO.SummarySetting summarySetting) {
        return BpmProcessDefinitionInfoDO.builder()
                .formType(BpmModelFormTypeEnum.NORMAL.getType())
                .formFields(formFields)
                .summarySetting(summarySetting)
                .build();
    }

    private static BpmModelMetaInfoVO.SummarySetting summarySetting(Boolean enable, String... fields) {
        BpmModelMetaInfoVO.SummarySetting summarySetting = new BpmModelMetaInfoVO.SummarySetting();
        summarySetting.setEnable(enable);
        summarySetting.setSummary(Arrays.asList(fields));
        return summarySetting;
    }

    private static List<String> dbFormFields() {
        return Arrays.asList(
                DIVIDER_FIELD,
                "{\"type\":\"input\",\"field\":\"reason\",\"title\":\"请假原因\",\"value\":\"\","
                        + "\"props\":{\"type\":\"textarea\",\"placeholder\":\"请输入请假原因\"},"
                        + "\"$required\":\"请输入请假原因\",\"_fc_id\":\"id_F1\",\"_fc_drag_tag\":\"input\","
                        + "\"hidden\":false,\"display\":true}",
                TEXT_FIELD,
                "{\"type\":\"elRow\",\"title\":\"栅格布局\",\"children\":["
                        + "{\"type\":\"elCol\",\"props\":{\"span\":12},\"children\":["
                        + "{\"type\":\"DatePicker\",\"field\":\"startTime\",\"title\":\"开始时间\","
                        + "\"props\":{\"type\":\"datetime\",\"placeholder\":\"请选择开始时间\"},"
                        + "\"_fc_id\":\"id_F2\",\"_fc_drag_tag\":\"datePicker\"}]},"
                        + "\"字段说明\","
                        + "{\"type\":\"elCol\",\"props\":{\"span\":12},\"children\":["
                        + "{\"type\":\"inputNumber\",\"field\":\"days\",\"title\":\"请假天数\","
                        + "\"props\":{\"min\":0,\"precision\":1},\"_fc_id\":\"id_F3\","
                        + "\"_fc_drag_tag\":\"inputNumber\"}]}],\"_fc_id\":\"id_LAYOUT\","
                        + "\"_fc_drag_tag\":\"row\"}",
                TAG_FIELD,
                "{\"type\":\"input\",\"field\":\"remark\",\"title\":\"备注\",\"value\":\"\","
                        + "\"props\":{\"placeholder\":\"请输入备注\"},\"_fc_id\":\"id_F4\","
                        + "\"_fc_drag_tag\":\"input\",\"hidden\":false,\"display\":true}");
    }

    private static Map<String, Object> processVariables() {
        Map<String, Object> processVariables = new HashMap<>();
        processVariables.put("reason", "事假");
        processVariables.put("startTime", "2026-05-31 09:00:00");
        processVariables.put("days", 3);
        processVariables.put("remark", "下午到家");
        return processVariables;
    }

    private static final String DIVIDER_FIELD = "{\"type\":\"elDivider\",\"children\":[\"基础信息\"],"
            + "\"props\":{\"contentPosition\":\"left\"},\"_fc_id\":\"id_DIVIDER\","
            + "\"_fc_drag_tag\":\"elDivider\"}";

    private static final String TEXT_FIELD = "{\"type\":\"div\",\"children\":[\"请按实际情况填写\"],"
            + "\"props\":{\"style\":{\"color\":\"#909399\"}},\"_fc_id\":\"id_TEXT\","
            + "\"_fc_drag_tag\":\"text\"}";

    private static final String TAG_FIELD = "{\"type\":\"elTag\",\"children\":[\"重要\"],"
            + "\"props\":{\"type\":\"warning\"},\"_fc_id\":\"id_TAG\",\"_fc_drag_tag\":\"elTag\"}";

}
