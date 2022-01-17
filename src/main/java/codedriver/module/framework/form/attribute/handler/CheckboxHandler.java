/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.framework.form.attribute.handler;

import codedriver.framework.common.constvalue.ParamType;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.form.constvalue.FormConditionModel;
import codedriver.framework.form.dto.AttributeDataVo;
import codedriver.framework.form.dto.FormAttributeVo;
import codedriver.framework.form.exception.AttributeValidException;
import codedriver.framework.form.attribute.core.FormHandlerBase;
import codedriver.framework.form.attribute.core.IFormAttributeHandler;
import codedriver.framework.restful.core.IApiComponent;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentFactory;
import codedriver.framework.restful.dto.ApiVo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CheckboxHandler extends FormHandlerBase {

    @Override
    public String getHandler() {
        return "formcheckbox";
    }

    @Override
    public JSONObject valid(AttributeDataVo attributeDataVo, JSONObject configObj) throws AttributeValidException {
        return null;
    }

    @Override
    public String getHandlerType(FormConditionModel model) {
        if (model == FormConditionModel.CUSTOM) {
            return "select";
        }
        return "checkbox";
    }

    @Override
    public Object valueConversionText(AttributeDataVo attributeDataVo, JSONObject configObj) {
        List<String> valueList = JSON.parseArray(JSON.toJSONString(attributeDataVo.getDataObj()), String.class);
        if (CollectionUtils.isNotEmpty(valueList)) {
            return getTextOrValue(configObj, valueList, ConversionType.TOTEXT.getValue());
        } else {
            return valueList;
        }
    }

    @Override
    public Object dataTransformationForEmail(AttributeDataVo attributeDataVo, JSONObject configObj) {
        return valueConversionText(attributeDataVo, configObj);
    }

    @Override
    public Object textConversionValue(List<String> values, JSONObject config) {
        Object result = null;
        if (CollectionUtils.isNotEmpty(values)) {
            result = getTextOrValue(config, values, ConversionType.TOVALUE.getValue());
        }
        return result;
    }

    private Object getTextOrValue(JSONObject configObj, List<String> valueList, String conversionType) {
        List<String> result = new ArrayList<>();
        String dataSource = configObj.getString("dataSource");
        if ("static".equals(dataSource)) {
            Map<Object, String> valueTextMap = new HashMap<>();
            List<ValueTextVo> dataList =
                    JSON.parseArray(JSON.toJSONString(configObj.getJSONArray("dataList")), ValueTextVo.class);
            if (CollectionUtils.isNotEmpty(dataList)) {
                for (ValueTextVo data : dataList) {
                    valueTextMap.put(data.getValue(), data.getText());
                }
            }
            if (ConversionType.TOTEXT.getValue().equals(conversionType)) {
                for (String value : valueList) {
                    String text = valueTextMap.get(value);
                    if (text != null) {
                        result.add(text);
                    } else {
                        result.add(value);
                    }
                }
            } else if (ConversionType.TOVALUE.getValue().equals(conversionType)) {
                for (String value : valueList) {
                    result.add(valueTextMap.get(value));
                }
            }
        } else if ("matrix".equals(dataSource)) {// 其他，如动态数据源
            if (ConversionType.TOTEXT.getValue().equals(conversionType)) {
                for (String value : valueList) {
                    if (value.contains(IFormAttributeHandler.SELECT_COMPOSE_JOINER)) {
                        result.add(value.split(IFormAttributeHandler.SELECT_COMPOSE_JOINER)[1]);
                    } else {
                        result.add(value);
                    }
                }
            } else if (ConversionType.TOVALUE.getValue().equals(conversionType)) {
                String matrixUuid = configObj.getString("matrixUuid");
                ValueTextVo mapping = JSON.toJavaObject(configObj.getJSONObject("mapping"), ValueTextVo.class);
                if (StringUtils.isNotBlank(matrixUuid) && CollectionUtils.isNotEmpty(valueList)
                        && mapping != null) {
                    ApiVo api = PrivateApiComponentFactory.getApiByToken("matrix/column/data/search/forselect/new");
                    if (api != null) {
                        IApiComponent restComponent = PrivateApiComponentFactory.getInstance(api.getHandler());
                        if (restComponent != null) {
                            for (String value : valueList) {
                                result.add(getValue(matrixUuid, mapping, value, restComponent, api));
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public String getHandlerName() {
        return "复选框";
    }

    @Override
    public String getIcon() {
        return "ts-check-square-o";
    }

    @Override
    public ParamType getParamType() {
        return ParamType.ARRAY;
    }

    @Override
    public String getDataType() {
        return "string";
    }

    @Override
    public boolean isConditionable() {
        return true;
    }

    @Override
    public boolean isShowable() {
        return true;
    }

    @Override
    public boolean isValueable() {
        return true;
    }

    @Override
    public boolean isFilterable() {
        return true;
    }

    @Override
    public boolean isExtendable() {
        return false;
    }

    @Override
    public String getModule() {
        return "framework";
    }

    @Override
    public boolean isForTemplate() {
        return true;
    }

    @Override
    public boolean isAudit() {
        return true;
    }

    @Override
    protected List<String> myIndexFieldContentList(String data) {
        if (data.startsWith("[") && data.endsWith("]")) {
            JSONArray jsonArray = JSONArray.parseArray(data);
            return JSONObject.parseArray(jsonArray.toJSONString(), String.class);
        }
        return null;
    }

    @Override
    public Boolean isNeedSliceWord() {
        return false;
    }

    @Override
    protected JSONObject getMyDetailedData(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject resultObj = new JSONObject();
        JSONArray valueArray = (JSONArray) attributeDataVo.getDataObj();
        if (CollectionUtils.isNotEmpty(valueArray)) {
            List<String> textList = new ArrayList<>();
            List<String> valueList = valueArray.toJavaList(String.class);
            String dataSource = configObj.getString("dataSource");
            if ("static".equals(dataSource)) {
                Map<Object, String> valueTextMap = new HashMap<>();
                JSONArray dataArray = configObj.getJSONArray("dataList");
                if (CollectionUtils.isNotEmpty(dataArray)) {
                    List<ValueTextVo> dataList = dataArray.toJavaList(ValueTextVo.class);
                    if (CollectionUtils.isNotEmpty(dataList)) {
                        for (ValueTextVo data : dataList) {
                            valueTextMap.put(data.getValue(), data.getText());
                        }
                    }
                }

                for (String value : valueList) {
                    String text = valueTextMap.get(value);
                    if (text != null) {
                        textList.add(text);
                    } else {
                        textList.add(value);
                    }
                }
            } else if ("matrix".equals(dataSource)) {// 其他，如动态数据源
                for (String value : valueList) {
                    if (value.contains(IFormAttributeHandler.SELECT_COMPOSE_JOINER)) {
                        textList.add(value.split(IFormAttributeHandler.SELECT_COMPOSE_JOINER)[1]);
                    } else {
                        textList.add(value);
                    }
                }
            }
            resultObj.put("textList", textList);
        }
        resultObj.put("valueList", valueArray);
        return resultObj;
    }

    @Override
    public void makeupFormAttribute(FormAttributeVo formAttributeVo) {
        Set<String> matrixUuidSet = new HashSet<>();
        Map<String, Set<String>> matrixUuidAttributeUuidSetMap = new HashMap<>();
        JSONObject config = formAttributeVo.getConfigObj();
        String dataSource = config.getString("dataSource");
        if ("matrix".equals(dataSource)) {
            String matrixUuid = config.getString("matrixUuid");
            if (StringUtils.isNotBlank(matrixUuid)) {
                matrixUuidSet.add(matrixUuid);
                Set<String> attributeUuidSet = new HashSet<>();
                /** 字段映射 **/
                JSONObject mapping = config.getJSONObject("mapping");
                if (MapUtils.isNotEmpty(mapping)) {
                    String value = mapping.getString("value");
                    if (StringUtils.isNotBlank(value)) {
                        attributeUuidSet.add(value);
                    }
                    String text = mapping.getString("text");
                    if (StringUtils.isNotBlank(text)) {
                        attributeUuidSet.add(text);
                    }
                }
                /** 过滤条件 **/
                JSONArray filterArray = config.getJSONArray("filterList");
                if (CollectionUtils.isNotEmpty(filterArray)) {
                    for (int i = 0; i < filterArray.size(); i++) {
                        JSONObject filterObj = filterArray.getJSONObject(i);
                        if (MapUtils.isNotEmpty(filterObj)) {
                            String uuid = filterObj.getString("uuid");
                            if (StringUtils.isNotBlank(uuid)) {
                                attributeUuidSet.add(uuid);
                            }
                        }
                    }
                }
                matrixUuidAttributeUuidSetMap.put(matrixUuid, attributeUuidSet);
            }
        }
        JSONArray relMatrixUuidArray = config.getJSONArray("relMatrixUuidList");
        if (CollectionUtils.isNotEmpty(relMatrixUuidArray)) {
            List<String> relMatrixUuidList = relMatrixUuidArray.toJavaList(String.class);
            matrixUuidSet.addAll(relMatrixUuidList);
        }
        formAttributeVo.setMatrixUuidSet(matrixUuidSet);
        formAttributeVo.setMatrixUuidAttributeUuidSetMap(matrixUuidAttributeUuidSetMap);
    }
}
