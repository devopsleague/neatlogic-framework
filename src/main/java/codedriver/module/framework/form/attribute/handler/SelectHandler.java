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
public class SelectHandler extends FormHandlerBase {

    @Override
    public String getHandler() {
        return "formselect";
    }

    @Override
    public String getHandlerType(FormConditionModel model) {
        return "select";
    }

    @Override
    public String getHandlerName() {
        return "下拉框";
    }

    @Override
    public String getIcon() {
        return "tsfont-formselect";
    }

    @Override
    public JSONObject valid(AttributeDataVo attributeDataVo, JSONObject configObj) throws AttributeValidException {
        return null;
    }

    @Override
    public Object valueConversionText(AttributeDataVo attributeDataVo, JSONObject configObj) {
        Object dataObj = attributeDataVo.getDataObj();
        if (dataObj != null) {
            boolean isMultiple = configObj.getBooleanValue("isMultiple");
            attributeDataVo.setIsMultiple(isMultiple? 1 : 0);
            String dataSource = configObj.getString("dataSource");
            if ("static".equals(dataSource)) {
                List<ValueTextVo> dataList =
                        JSON.parseArray(JSON.toJSONString(configObj.getJSONArray("dataList")), ValueTextVo.class);
                if (CollectionUtils.isNotEmpty(dataList)) {
                    Map<Object, String> valueTextMap = new HashMap<>();
                    for (ValueTextVo data : dataList) {
                        valueTextMap.put(data.getValue(), data.getText());
                    }
                    if (isMultiple) {
                        List<String> valueList = JSON.parseArray(JSON.toJSONString(dataObj), String.class);
                        if (CollectionUtils.isNotEmpty(valueList)) {
                            List<String> textList = new ArrayList<>();
                            for (String key : valueList) {
                                String text = valueTextMap.get(key);
                                if (text != null) {
                                    textList.add(text);
                                } else {
                                    textList.add(key);
                                }
                            }
                            return textList;
                        }
                        return valueList;
                    } else {
                        String text = valueTextMap.get((String) dataObj);
                        if (text != null) {
                            return text;
                        } else {
                            return dataObj;
                        }
                    }
                }
            } else {// 其他，如动态数据源
                if (isMultiple) {
                    List<String> valueList = JSON.parseArray(JSON.toJSONString(dataObj), String.class);
                    if (CollectionUtils.isNotEmpty(valueList)) {
                        List<String> textList = new ArrayList<>();
                        for (String key : valueList) {
                            if (key.contains(IFormAttributeHandler.SELECT_COMPOSE_JOINER)) {
                                textList.add(key.split(IFormAttributeHandler.SELECT_COMPOSE_JOINER)[1]);
                            } else {
                                textList.add(key);
                            }
                        }
                        return textList;
                    }
                    return valueList;
                } else {
                    String value = (String) dataObj;
                    if (value.contains(IFormAttributeHandler.SELECT_COMPOSE_JOINER)) {
                        return value.split(IFormAttributeHandler.SELECT_COMPOSE_JOINER)[1];
                    } else {
                        return dataObj;
                    }
                }
            }
        }
        return dataObj;
    }

    @Override
    public Object dataTransformationForEmail(AttributeDataVo attributeDataVo, JSONObject configObj) {
        return valueConversionText(attributeDataVo, configObj);
    }

    @Override
    public Object textConversionValue(List<String> values, JSONObject config) {
        Object result = null;
        if (CollectionUtils.isNotEmpty(values)) {
            boolean isMultiple = config.getBooleanValue("isMultiple");
            String dataSource = config.getString("dataSource");
            if ("static".equals(dataSource)) {
                List<ValueTextVo> dataList =
                        JSON.parseArray(JSON.toJSONString(config.getJSONArray("dataList")), ValueTextVo.class);
                if (CollectionUtils.isNotEmpty(dataList)) {
                    Map<String, Object> valueTextMap = new HashMap<>();
                    for (ValueTextVo data : dataList) {
                        valueTextMap.put(data.getText(), data.getValue());
                    }
                    if (isMultiple) {
                        JSONArray jsonArray = new JSONArray();
                        for (String value : values) {
                            jsonArray.add(valueTextMap.get(value));
                        }
                        result = jsonArray;
                    } else {
                        result = valueTextMap.get(values.get(0));
                    }
                }

            } else if ("matrix".equals(dataSource)) {
                String matrixUuid = config.getString("matrixUuid");
                ValueTextVo mapping = JSON.toJavaObject(config.getJSONObject("mapping"), ValueTextVo.class);
                if (StringUtils.isNotBlank(matrixUuid) && CollectionUtils.isNotEmpty(values)
                        && mapping != null) {
                    ApiVo api = PrivateApiComponentFactory.getApiByToken("matrix/column/data/search/forselect/new");
                    if (api != null) {
                        IApiComponent restComponent = PrivateApiComponentFactory.getInstance(api.getHandler());
                        if (restComponent != null) {
                            if (isMultiple) {
                                JSONArray jsonArray = new JSONArray();
                                for (String value : values) {
                                    jsonArray.add(getValue(matrixUuid, mapping, value, restComponent, api));
                                }
                                result = jsonArray;
                            } else {
                                result = getValue(matrixUuid, mapping, values.get(0), restComponent, api);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public ParamType getParamType() {
        return ParamType.ARRAY;
    }

    @Override
    public String getDataType() {
        return "list";
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
        List<String> contentList = new ArrayList<>();
        if (data.startsWith("[") && data.endsWith("]")) {
            JSONArray jsonArray = JSONArray.parseArray(data);
            for (Object obj : jsonArray) {
                if (obj != null) {
                    contentList.addAll(Arrays.asList(obj.toString().split("&=&")));
                }
            }
            return JSONObject.parseArray(jsonArray.toJSONString(), String.class);
        } else {
            contentList.addAll(Arrays.asList(data.split("&=&")));
        }
        return contentList;
    }

    @Override
    public Boolean isNeedSliceWord() {
        return false;
    }

    @Override
    protected JSONObject getMyDetailedData(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject resultObj = new JSONObject();
        Object dataObj = attributeDataVo.getDataObj();
        if (dataObj != null) {
            List<String> valueList = new ArrayList<>();
            List<String> textList = new ArrayList<>();
            boolean isMultiple = configObj.getBooleanValue("isMultiple");
            attributeDataVo.setIsMultiple(isMultiple? 1 : 0);
            String dataSource = configObj.getString("dataSource");
            if ("static".equals(dataSource)) {
                JSONArray dataArray = configObj.getJSONArray("dataList");
                if (CollectionUtils.isNotEmpty(dataArray)) {
                    Map<Object, String> valueTextMap = new HashMap<>();
                    List<ValueTextVo> dataList = dataArray.toJavaList(ValueTextVo.class);
                    for (ValueTextVo data : dataList) {
                        valueTextMap.put(data.getValue(), data.getText());
                    }
                    if (isMultiple) {
                        JSONArray valueArray = (JSONArray) dataObj;
                        if (CollectionUtils.isNotEmpty(valueArray)) {
                            valueList = valueArray.toJavaList(String.class);
                            for (String key : valueList) {
                                String text = valueTextMap.get(key);
                                if (text != null) {
                                    textList.add(text);
                                } else {
                                    textList.add(key);
                                }
                            }
                        }
                    } else {
                        valueList.add((String) dataObj);
                        String text = valueTextMap.get(dataObj);
                        if (text != null) {
                            textList.add(text);
                        } else {
                            textList.add((String) dataObj);
                        }
                    }
                }
            } else {// 其他，如动态数据源
                if (isMultiple) {
                    JSONArray valueArray = (JSONArray) dataObj;
                    if (CollectionUtils.isNotEmpty(valueArray)) {
                        valueList = valueArray.toJavaList(String.class);
                        for (String key : valueList) {
                            if (key.contains(IFormAttributeHandler.SELECT_COMPOSE_JOINER)) {
                                textList.add(key.split(IFormAttributeHandler.SELECT_COMPOSE_JOINER)[1]);
                            } else {
                                textList.add(key);
                            }
                        }
                    }
                } else {
                    String value = (String) dataObj;
                    if (value.contains(IFormAttributeHandler.SELECT_COMPOSE_JOINER)) {
                        textList.add(value.split(IFormAttributeHandler.SELECT_COMPOSE_JOINER)[1]);
                    } else {
                        textList.add((String) dataObj);
                    }
                }
            }
            resultObj.put("valueList", valueList);
        }
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
