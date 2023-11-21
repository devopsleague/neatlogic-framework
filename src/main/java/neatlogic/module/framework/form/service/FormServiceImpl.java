/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.framework.form.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.form.attribute.core.FormAttributeHandlerFactory;
import neatlogic.framework.form.attribute.core.IFormAttributeHandler;
import neatlogic.framework.form.dao.mapper.FormMapper;
import neatlogic.framework.form.dto.AttributeDataVo;
import neatlogic.framework.form.dto.FormAttributeMatrixVo;
import neatlogic.framework.form.dto.FormAttributeVo;
import neatlogic.framework.form.dto.FormVersionVo;
import neatlogic.framework.form.exception.AttributeValidException;
import neatlogic.framework.form.service.IFormCrossoverService;
import neatlogic.framework.matrix.constvalue.SearchExpression;
import neatlogic.framework.matrix.core.IMatrixDataSourceHandler;
import neatlogic.framework.matrix.core.MatrixDataSourceHandlerFactory;
import neatlogic.framework.matrix.dto.MatrixDataVo;
import neatlogic.framework.matrix.dto.MatrixDefaultValueFilterVo;
import neatlogic.framework.matrix.dto.MatrixKeywordFilterVo;
import neatlogic.framework.matrix.dto.MatrixVo;
import neatlogic.framework.matrix.exception.MatrixDataSourceHandlerNotFoundException;
import neatlogic.module.framework.dependency.handler.Integration2FormAttrDependencyHandler;
import neatlogic.module.framework.dependency.handler.MatrixAttr2FormAttrDependencyHandler;
import neatlogic.module.framework.matrix.service.MatrixService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FormServiceImpl implements FormService, IFormCrossoverService {

    @Resource
    private FormMapper formMapper;

    @Resource
    private MatrixService matrixService;

    /**
     * 保存表单属性与其他功能的引用关系
     * @param formAttributeVo
     */
    @Override
    public void saveDependency(FormAttributeVo formAttributeVo) {
        String formUuid = formAttributeVo.getFormUuid();
        String formVersionUuid = formAttributeVo.getFormVersionUuid();
        IFormAttributeHandler formAttributeHandler = FormAttributeHandlerFactory.getHandler(formAttributeVo.getHandler());
        if (formAttributeHandler == null) {
//            throw new FormAttributeHandlerNotFoundException(formAttributeVo.getHandler());
            return;
        }
        formAttributeHandler.makeupFormAttribute(formAttributeVo);
        Set<String> matrixUuidSet = formAttributeVo.getMatrixUuidSet();
        if (CollectionUtils.isNotEmpty(matrixUuidSet)) {
            for (String matrixUuid : matrixUuidSet) {
                FormAttributeMatrixVo formAttributeMatrixVo = new FormAttributeMatrixVo();
                formAttributeMatrixVo.setMatrixUuid(matrixUuid);
                formAttributeMatrixVo.setFormVersionUuid(formVersionUuid);
                formAttributeMatrixVo.setFormAttributeLabel(formAttributeVo.getLabel());
                formAttributeMatrixVo.setFormAttributeUuid(formAttributeVo.getUuid());
                formMapper.insertFormAttributeMatrix(formAttributeMatrixVo);
            }
        }

        Set<String> integrationUuidSet = formAttributeVo.getIntegrationUuidSet();
        if (CollectionUtils.isNotEmpty(integrationUuidSet)) {
            JSONObject config = new JSONObject();
            config.put("formUuid", formUuid);
            config.put("formVersionUuid", formVersionUuid);
            config.put("formAttributeUuid", formAttributeVo.getUuid());
            for (String integrationUuid : integrationUuidSet) {
                config.put("integrationUuid", integrationUuid);
                DependencyManager.insert(Integration2FormAttrDependencyHandler.class, integrationUuid, formAttributeVo.getUuid(), config);
            }
        }

        Map<String, Set<String>> matrixUuidAttributeUuidSetMap = formAttributeVo.getMatrixUuidAttributeUuidSetMap();
        if (MapUtils.isNotEmpty(matrixUuidAttributeUuidSetMap)) {
            JSONObject config = new JSONObject();
            config.put("formUuid", formUuid);
            config.put("formVersionUuid", formVersionUuid);
            for (Map.Entry<String, Set<String>> entry : matrixUuidAttributeUuidSetMap.entrySet()) {
                String matrixUuid = entry.getKey();
                config.put("matrixUuid", matrixUuid);
                Set<String> attributeUuidSet = entry.getValue();
                if (CollectionUtils.isNotEmpty(attributeUuidSet)) {
                    for (String attributeUuid : attributeUuidSet) {
                        DependencyManager.insert(MatrixAttr2FormAttrDependencyHandler.class, attributeUuid, formAttributeVo.getUuid(), config);
                    }
                }
            }
        }
    }

    @Override
    public JSONArray staticListPasswordEncrypt(JSONArray data, JSONObject config) {
        if (CollectionUtils.isEmpty(data)) {
            return data;
        }
        List<String> passwordTypeAttributeUuidList = new ArrayList<>();
        List<String> tableTypeAttributeUuidList = new ArrayList<>();
        JSONArray attributeList = config.getJSONArray("attributeList");
        if (CollectionUtils.isEmpty(attributeList)) {
            return data;
        }
        for (int i = 0; i < attributeList.size(); i++) {
            JSONObject attributeObj = attributeList.getJSONObject(i);
            if (MapUtils.isEmpty(attributeObj)) {
                continue;
            }
            String attributeUuid = attributeObj.getString("attributeUuid");
            String type = attributeObj.getString("type");
            if (Objects.equals(type, "password")) {
                passwordTypeAttributeUuidList.add(attributeUuid);
            } else if (Objects.equals(type, "table")) {
                tableTypeAttributeUuidList.add(attributeUuid);
                JSONObject attrConfig = attributeObj.getJSONObject("attrConfig");
                if (MapUtils.isEmpty(attrConfig)) {
                    continue;
                }
                JSONArray attributeArray = attrConfig.getJSONArray("attributeList");
                if (CollectionUtils.isEmpty(attributeArray)) {
                    continue;
                }
                for (int j = 0; j < attributeArray.size(); j++) {
                    JSONObject attributeObject = attributeArray.getJSONObject(j);
                    if (MapUtils.isEmpty(attributeObject)) {
                        continue;
                    }
                    String attrUuid = attributeObject.getString("attributeUuid");
                    if (Objects.equals(attributeObject.getString("type"), "password")) {
                        passwordTypeAttributeUuidList.add(attrUuid);
                    }
                }
            }
        }
//        JSONObject extendedData = data.getJSONObject("extendedData");
//        if (MapUtils.isNotEmpty(extendedData)) {
//            for (Map.Entry<String, Object> entry : extendedData.entrySet()) {
//               JSONObject rowDataObj = (JSONObject) entry.getValue();
//               for (String key : rowDataObj.keySet()) {
//                   if (passwordTypeAttributeUuidList.contains(key)) {
//                       String value = rowDataObj.getString(key);
//                       if (StringUtils.isNotBlank(value)) {
//                           rowDataObj.put(key, RC4Util.encrypt(value));
//                       }
//                   } else if (tableTypeAttributeUuidList.contains(key)) {
//                       JSONObject tableDataObj = rowDataObj.getJSONObject(key);
//                       for (Map.Entry<String, Object> tableEntry : tableDataObj.entrySet()) {
//                           JSONObject tableRowDataObj = (JSONObject) tableEntry.getValue();
//                           List<String> keyList = new ArrayList<>(tableRowDataObj.keySet());
//                           for (String tableRowKey : keyList) {
//                               if (passwordTypeAttributeUuidList.contains(tableRowKey)) {
//                                   String value = tableRowDataObj.getString(tableRowKey);
//                                   if (StringUtils.isNotBlank(value)) {
//                                       tableRowDataObj.put(tableRowKey, RC4Util.encrypt(value));
//                                   }
//                               }
//                           }
//                       }
//                   }
//               }
//            }
//        }
//        JSONObject detailData = data.getJSONObject("detailData");
//        if (MapUtils.isNotEmpty(detailData)) {
//            for (Map.Entry<String, Object> entry : detailData.entrySet()) {
//                JSONObject rowDataObj = (JSONObject) entry.getValue();
//                for (String key : rowDataObj.keySet()) {
//                    if (passwordTypeAttributeUuidList.contains(key)) {
//                        JSONObject valueObj = rowDataObj.getJSONObject(key);
//                        if (MapUtils.isNotEmpty(valueObj)) {
//                            String value = valueObj.getString("value");
//                            if (StringUtils.isNotBlank(value)) {
//                                valueObj.put("value", RC4Util.encrypt(value));
//                            }
//                            String text = valueObj.getString("text");
//                            if (StringUtils.isNotBlank(text)) {
//                                valueObj.put("text", RC4Util.encrypt(text));
//                            }
//                        }
//                    } else if (tableTypeAttributeUuidList.contains(key)) {
//                        JSONObject tableDataObj = rowDataObj.getJSONObject(key);
//                        tableDataObj = tableDataObj.getJSONObject("value");
//                        for (Map.Entry<String, Object> tableEntry : tableDataObj.entrySet()) {
//                            JSONObject tableRowDataObj = (JSONObject) tableEntry.getValue();
//                            for (String tableRowKey : tableRowDataObj.keySet()) {
//                                if (passwordTypeAttributeUuidList.contains(tableRowKey)) {
//                                    JSONObject valueObj = tableRowDataObj.getJSONObject(tableRowKey);
//                                    if (MapUtils.isNotEmpty(valueObj)) {
//                                        String value = valueObj.getString("value");
//                                        if (StringUtils.isNotBlank(value)) {
//                                            valueObj.put("value", RC4Util.encrypt(value));
//                                        }
//                                        String text = valueObj.getString("text");
//                                        if (StringUtils.isNotBlank(text)) {
//                                            valueObj.put("text", RC4Util.encrypt(text));
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
        return data;
    }

    @Override
    public List<ValueTextVo> textConversionValueForSelectHandler(Object text, JSONObject config) {
        List<ValueTextVo> valueList = new ArrayList<>();
        if (text == null) {
            return valueList;
        }
        String dataSource = config.getString("dataSource");
        if ("static".equals(dataSource)) {
            JSONArray dataArray = config.getJSONArray("dataList");
            if (CollectionUtils.isEmpty(dataArray)) {
                return null;
            }
            List<ValueTextVo> dataList = dataArray.toJavaList(ValueTextVo.class);
            Map<String, Object> valueTextMap = dataList.stream().collect(Collectors.toMap(e -> e.getText(), e -> e.getValue()));
            if (text instanceof String) {
                String textStr = (String) text;
                Object value = valueTextMap.get(textStr);
                if (value != null) {
                    valueList.add(new ValueTextVo(value, textStr));
                }
                return valueList;
            }  else if (text instanceof List) {
                List<String> textList = (List) text;
                if (CollectionUtils.isEmpty(textList)) {
                    return valueList;
                }
                for (String textStr : textList) {
                    Object value = valueTextMap.get(textStr);
                    if (value != null) {
                        valueList.add(new ValueTextVo(value, textStr));
                    }
                }
                return valueList;
            }
        } else if ("matrix".equals(dataSource)) {
            String matrixUuid = config.getString("matrixUuid");
            if (StringUtils.isBlank(matrixUuid)) {
                return valueList;
            }
            JSONObject mappingObj = config.getJSONObject("mapping");
            if (MapUtils.isEmpty(mappingObj)) {
                return valueList;
            }
            ValueTextVo mapping = mappingObj.toJavaObject(ValueTextVo.class);
            System.out.println("13");
            if (text instanceof String) {
                String textStr = (String) text;
                if (Objects.equals(mapping.getText(), mapping.getValue())) {
                    valueList.add(new ValueTextVo(textStr, textStr));
                    return valueList;
                }
                String value = getValue(matrixUuid, mapping, textStr);
                if (value != null) {
                    valueList.add(new ValueTextVo(value, textStr));
                }
                return valueList;
            }  else if (text instanceof List) {
                List<String> textList = (List) text;
                if (CollectionUtils.isEmpty(textList)) {
                    return valueList;
                }
                for (String textStr : textList) {
                    if (Objects.equals(mapping.getText(), mapping.getValue())) {
                        valueList.add(new ValueTextVo(textStr, textStr));
                    } else {
                        String value = getValue(matrixUuid, mapping, textStr);
                        if (value != null) {
                            valueList.add(new ValueTextVo(value, textStr));
                        }
                    }
                }
                return valueList;
            }
        }
        return null;
    }

    @Override
    public void formAttributeValueValid(FormVersionVo formVersionVo, JSONArray formAttributeDataList) throws AttributeValidException {
        if (formVersionVo == null) {
            return;
        }
        List<FormAttributeVo> formAttributeList = formVersionVo.getFormAttributeList();
        if (CollectionUtils.isEmpty(formAttributeList)) {
            return;
        }
        Map<String, FormAttributeVo> formAttributeMap = formAttributeList.stream().collect(Collectors.toMap(e -> e.getUuid(), e -> e));
        for (int i = 0; i < formAttributeDataList.size(); i++) {
            JSONObject formAttributeDataObj = formAttributeDataList.getJSONObject(i);
            String attributeUuid = formAttributeDataObj.getString("attributeUuid");
            FormAttributeVo formAttributeVo = formAttributeMap.get(attributeUuid);
            if (formAttributeVo == null) {
                continue;
            }
            IFormAttributeHandler formAttributeHandler = FormAttributeHandlerFactory.getHandler(formAttributeVo.getHandler());
            if (formAttributeHandler == null) {
                continue;
            }
            Object dataList = formAttributeHandler.conversionDataType(formAttributeDataObj.get("dataList"), formAttributeVo.getLabel());
            formAttributeDataObj.put("dataList", dataList);
//            AttributeDataVo attributeDataVo = new AttributeDataVo();
//            attributeDataVo.setAttributeUuid(formAttributeVo.getUuid());
//            attributeDataVo.setAttributeLabel(formAttributeVo.getLabel());
//            attributeDataVo.setDataObj(formAttributeDataObj.get("dataList"));
//            formAttributeHandler.valid(attributeDataVo, formAttributeVo.getConfigObj());
//            formAttributeDataObj.put("dataList", attributeDataVo.getDataObj());
        }
    }

    @Override
    public JSONObject getMyDetailedDataForSelectHandler(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject resultObj = new JSONObject();
        Object dataObj = attributeDataVo.getDataObj();
        if (dataObj == null) {
            return resultObj;
        }
        List<String> valueList = new ArrayList<>();
        List<String> textList = new ArrayList<>();
        boolean isMultiple = configObj.getBooleanValue("isMultiple");
        attributeDataVo.setIsMultiple(isMultiple ? 1 : 0);
        String dataSource = configObj.getString("dataSource");
        if ("static".equals(dataSource)) {
            JSONArray dataArray = configObj.getJSONArray("dataList");
            if (CollectionUtils.isEmpty(dataArray)) {
                return resultObj;
            }
            List<ValueTextVo> dataList = dataArray.toJavaList(ValueTextVo.class);
            Map<Object, String> valueTextMap = dataList.stream().collect(Collectors.toMap(e -> e.getValue(), e -> e.getText()));
            if (dataObj instanceof JSONArray) {
                JSONArray valueArray = (JSONArray) dataObj;
                if (CollectionUtils.isNotEmpty(valueArray)) {
//                    valueList = valueArray.toJavaList(String.class);
                    for (Object valueObj : valueArray) {
                        if (valueObj instanceof JSONObject) {
                            JSONObject jsonObj = (JSONObject) valueObj;
                            String value = jsonObj.getString("value");
                            if (value != null) {
                                valueList.add(value);
                            }
                            String text = jsonObj.getString("text");
                            if (text != null) {
                                textList.add(text);
                            } else {
                                if (value != null) {
                                    textList.add(value);
                                }
                            }
                        } else {
                            String text = valueTextMap.get(valueObj);
                            if (text != null) {
                                textList.add(text);
                            } else {
                                textList.add(valueObj.toString());
                            }
                        }
                    }
                }
            } else if (dataObj instanceof JSONObject) {
                JSONObject jsonObj = (JSONObject) dataObj;
                String value = jsonObj.getString("value");
                if (value != null) {
                    valueList.add(value);
                }
                String text = jsonObj.getString("text");
                if (text != null) {
                    textList.add(text);
                } else {
                    if (value != null) {
                        textList.add(value);
                    }
                }
            } else {
                String value = dataObj.toString();
                valueList.add(value);
                String text = valueTextMap.get(value);
                if (text != null) {
                    textList.add(text);
                } else {
                    textList.add(value);
                }
            }
        } else {// 其他，如动态数据源
            System.out.println("14");
            String matrixUuid = configObj.getString("matrixUuid");
            if (StringUtils.isBlank(matrixUuid)) {
                return resultObj;
            }
            JSONObject mappingObj = configObj.getJSONObject("mapping");
            if (MapUtils.isEmpty(mappingObj)) {
                return resultObj;
            }
            ValueTextVo mapping = mappingObj.toJavaObject(ValueTextVo.class);
            if (dataObj instanceof JSONArray) {
                JSONArray valueArray = (JSONArray) dataObj;
                if (CollectionUtils.isNotEmpty(valueArray)) {
                    for (int i = 0; i < valueArray.size(); i++) {
                        Object obj = valueArray.get(i);
                        if (obj instanceof JSONObject) {
                            JSONObject jsonObj = (JSONObject) obj;
                            String value = jsonObj.getString("value");
                            if (value != null) {
                                valueList.add(value);
                            }
                            String text = jsonObj.getString("text");
                            if (text != null) {
                                textList.add(text);
                            } else {
                                textList.add(value);
                            }
                        } else {
                            String value = obj.toString();
                            valueList.add(value);
                            String text = getText(matrixUuid, mapping, value);
                            if (text != null) {
                                textList.add(text);
                            } else {
                                textList.add(value);
                            }
                        }
                    }
                }
            } else if (dataObj instanceof JSONObject) {
                JSONObject jsonObj = (JSONObject) dataObj;
                String value = jsonObj.getString("value");
                if (value != null) {
                    valueList.add(value);
                }
                String text = jsonObj.getString("text");
                if (text != null) {
                    textList.add(text);
                } else {
                    textList.add(value);
                }
            } else {
                String value = dataObj.toString();
                valueList.add(value);
                String text = getText(matrixUuid, mapping, value);
                if (text != null) {
                    textList.add(text);
                } else {
                    textList.add(value);
                }
            }
        }
        resultObj.put("valueList", valueList);
        resultObj.put("textList", textList);
        return resultObj;
    }

    @Override
    public boolean isModifiedFormData(List<FormAttributeVo> formAttributeList,
                                      List<? extends AttributeDataVo> newFormAttributeDataList,
                                      List<? extends AttributeDataVo> oldFormAttributeDataList) {
        boolean isModified = false;
        Map<String, ? extends AttributeDataVo> newFormAttributeDataMap = newFormAttributeDataList.stream().collect(Collectors.toMap(e -> e.getAttributeUuid(), e -> e));
        Map<String, ? extends AttributeDataVo> oldFormAttributeDataMap = oldFormAttributeDataList.stream().collect(Collectors.toMap(e -> e.getAttributeUuid(), e -> e));
        for (FormAttributeVo formAttributeVo : formAttributeList) {
            String attributeUuid = formAttributeVo.getUuid();
            AttributeDataVo newProcessTaskFormAttributeDataVo = newFormAttributeDataMap.get(attributeUuid);
            AttributeDataVo oldProcessTaskFormAttributeDataVo = oldFormAttributeDataMap.get(attributeUuid);
            if (oldProcessTaskFormAttributeDataVo == null && newProcessTaskFormAttributeDataVo == null) {
                continue;
            }
            // 在此之前如果该属性的值，在数据库中没有对应的旧数据
            if (oldProcessTaskFormAttributeDataVo == null) {
                if (newProcessTaskFormAttributeDataVo.getDataObj() != null) {
                    // 现在要保存该属性的值不为null，则将该属性值保存到数据库中，但标记为已修改
                    isModified = true;
                }
            } else if (newProcessTaskFormAttributeDataVo == null) {
                // 如果现在接口参数中没有该属性值，则表示不修改该属性值
            } else if (!Objects.equals(oldProcessTaskFormAttributeDataVo.getDataObj(), newProcessTaskFormAttributeDataVo.getDataObj())) {
                isModified = true;
            }
        }
        return isModified;
    }

    @Override
    public Object getFormSelectAttributeValueByOriginalValue(Object originalValue) {
        if (originalValue == null) {
            return null;
        }
        if (originalValue instanceof JSONArray) {
            JSONArray valueList = new JSONArray();
            JSONArray originalValueArray = (JSONArray) originalValue;
            for (int i = 0; i < originalValueArray.size(); i++) {
                Object originalValueObject = originalValueArray.get(i);
                if (originalValueObject instanceof JSONObject) {
                    JSONObject originalValueObj = (JSONObject) originalValueObject;
                    Object value = originalValueObj.get("value");
                    if (value != null) {
                        valueList.add(value);
                    }
                } else {
                    valueList.add(originalValueObject);
                }
            }
            return valueList;
        } else if (originalValue instanceof JSONObject) {
            JSONObject originalValueObj = (JSONObject) originalValue;
            return originalValueObj.get("value");
        }
        return originalValue;
    }

    private String getValue(String matrixUuid, ValueTextVo mapping, String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        try {
            MatrixVo matrixVo = matrixService.getMatrixByUuid(matrixUuid);
            IMatrixDataSourceHandler matrixDataSourceHandler = MatrixDataSourceHandlerFactory.getHandler(matrixVo.getType());
            if (matrixDataSourceHandler == null) {
                throw new MatrixDataSourceHandlerNotFoundException(matrixVo.getType());
            }
            String valueField = (String) mapping.getValue();
            String textField = mapping.getText();
            MatrixDataVo dataVo = new MatrixDataVo();
            dataVo.setMatrixUuid(matrixUuid);
            List<String> columnList = new ArrayList<>();
            columnList.add((String) mapping.getValue());
            columnList.add(mapping.getText());
            dataVo.setColumnList(columnList);
            dataVo.setKeyword(text);
            dataVo.setKeywordColumn(textField);
            for (int i = 0; i < 10; i++) {
                List<Map<String, JSONObject>> tbodyList = matrixDataSourceHandler.searchTableDataNew(dataVo);
                for (Map<String, JSONObject> tbody : tbodyList) {
                    JSONObject textObj = tbody.get(textField);
                    if (Objects.equals(text, textObj.getString("text"))) {
                        JSONObject valueObj = tbody.get(valueField);
                        System.out.println("15");
                        return valueObj.getString("value");
                    }
                }
                if (dataVo.getCurrentPage() >= dataVo.getPageCount()) {
                    break;
                }
                dataVo.setCurrentPage(dataVo.getCurrentPage() + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getText(String matrixUuid, ValueTextVo mapping, String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        try {
            MatrixVo matrixVo = matrixService.getMatrixByUuid(matrixUuid);
            IMatrixDataSourceHandler matrixDataSourceHandler = MatrixDataSourceHandlerFactory.getHandler(matrixVo.getType());
            if (matrixDataSourceHandler == null) {
                throw new MatrixDataSourceHandlerNotFoundException(matrixVo.getType());
            }
            String valueField = (String) mapping.getValue();
            String textField = mapping.getText();
            MatrixDataVo dataVo = new MatrixDataVo();
            dataVo.setMatrixUuid(matrixUuid);
            List<String> columnList = new ArrayList<>();
            columnList.add((String) mapping.getValue());
            columnList.add(mapping.getText());
            dataVo.setColumnList(columnList);
            List<MatrixDefaultValueFilterVo> defaultValueFilterList = new ArrayList<>();
            MatrixDefaultValueFilterVo matrixDefaultValueFilterVo = new MatrixDefaultValueFilterVo(
                    new MatrixKeywordFilterVo(valueField, SearchExpression.EQ.getExpression(), value),
                    null
            );
            defaultValueFilterList.add(matrixDefaultValueFilterVo);
            dataVo.setDefaultValueFilterList(defaultValueFilterList);
            for (int i = 0; i < 10; i++) {
                List<Map<String, JSONObject>> tbodyList = matrixDataSourceHandler.searchTableDataNew(dataVo);
                for (Map<String, JSONObject> tbody : tbodyList) {
                    JSONObject valueObj = tbody.get(valueField);
                    if (Objects.equals(value, valueObj.getString("value"))) {
                        JSONObject textObj = tbody.get(textField);
                        System.out.println("15");
                        return textObj.getString("text");
                    }
                }
                if (dataVo.getCurrentPage() >= dataVo.getPageCount()) {
                    break;
                }
                dataVo.setCurrentPage(dataVo.getCurrentPage() + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
