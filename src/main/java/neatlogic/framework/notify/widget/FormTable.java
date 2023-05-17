/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.framework.notify.widget;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import neatlogic.framework.form.dto.AttributeDataVo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Function;

public class FormTable implements TemplateMethodModelEx {

    private static Map<String, Function<Object, String>> map = new HashMap<>();

    static {
        // 文本框
        map.put("formtext", dataObj -> {
            if (dataObj instanceof String) {
                return (String) dataObj;
            } else {
                return dataObj.toString();
            }
        });
        // 文本域
        map.put("formtextarea", dataObj -> {
            if (dataObj instanceof String) {
                return (String) dataObj;
            } else {
                return dataObj.toString();
            }
        });
        // 富文本框
        map.put("formckeditor", dataObj -> {
            if (dataObj instanceof String) {
                return (String) dataObj;
            } else {
                return dataObj.toString();
            }
        });
        // 数字
        map.put("formnumber", dataObj -> {
            if (dataObj instanceof String) {
                return (String) dataObj;
            } else {
                return dataObj.toString();
            }
        });
        // 密码
        map.put("formpassword", dataObj -> {
            if (dataObj instanceof String) {
                return (String) dataObj;
            } else {
                return dataObj.toString();
            }
        });
        // 时间
        map.put("formtime", dataObj -> {
            if (dataObj instanceof String) {
                return (String) dataObj;
            } else {
                return dataObj.toString();
            }
        });
        // 日期
        map.put("formdate", dataObj -> {
            if (dataObj instanceof String) {
                return (String) dataObj;
            } else {
                return dataObj.toString();
            }
        });
        // 超链接
        map.put("formlink", dataObj -> {
            if (dataObj instanceof String) {
                return (String) dataObj;
            } else {
                return dataObj.toString();
            }
        });
        // 评分
        map.put("formrate", dataObj -> {
            if (dataObj instanceof String) {
                return (String) dataObj;
            } else {
                return dataObj.toString();
            }
        });
        // 上传附件
        map.put("formupload", dataObj -> {
            if (dataObj instanceof String) {
                return (String) dataObj;
            } else {
                return dataObj.toString();
            }
        });
        // 树形下拉框
        map.put("formtreeselect", dataObj -> {
            if (dataObj instanceof String) {
                return (String) dataObj;
            } else {
                return dataObj.toString();
            }
        });
        // 连接协议
        map.put("formprotocol", dataObj -> {
            if (dataObj instanceof String) {
                return (String) dataObj;
            } else {
                return dataObj.toString();
            }
        });
        // 级联下拉框
        map.put("formcascader", dataObj -> {
            if (dataObj instanceof String) {
                return (String) dataObj;
            } else {
                return dataObj.toString();
            }
        });
        // 复选框
        map.put("formcheckbox", dataObj -> {
            if (dataObj instanceof String) {
                return (String) dataObj;
            } else {
                return dataObj.toString();
            }
        });
        // 单选框
        map.put("formradio", dataObj -> {
            if (dataObj instanceof String) {
                return (String) dataObj;
            } else {
                return dataObj.toString();
            }
        });
        // 下拉框
        map.put("formselect", dataObj -> {
            if (dataObj instanceof String) {
                return (String) dataObj;
            } else {
                return dataObj.toString();
            }
        });
        // 用户选择
        map.put("formuserselect", dataObj -> {
            if (dataObj instanceof String) {
                return (String) dataObj;
            } else {
                return dataObj.toString();
            }
        });
        // 矩阵选择
        map.put("formcube", dataObj -> {
            if (dataObj instanceof String) {
                return (String) dataObj;
            } else {
                return dataObj.toString();
            }
        });
        // 执行目标
        map.put("formresoureces", dataObj -> {
            if (dataObj instanceof String) {
                return (String) dataObj;
            } else {
                return dataObj.toString();
            }
        });
        // 配置项选择
        map.put("formcientityselector", dataObj -> {
            if (dataObj instanceof String) {
                return (String) dataObj;
            } else {
                return dataObj.toString();
            }
        });
        // 帐号
        map.put("formaccounts", dataObj -> {
            if (dataObj instanceof JSONObject) {
                JSONObject tableObj = (JSONObject) dataObj;
                return createTableHtml(tableObj);
            } else {
                return StringUtils.EMPTY;
            }
        });
        // 配置项修改
        map.put("formcientitymodify", dataObj -> {
            if (dataObj instanceof JSONObject) {
                JSONObject tableObj = (JSONObject) dataObj;
                return createTableHtml(tableObj);
            } else {
                return StringUtils.EMPTY;
            }
        });
        // 表格选择
        map.put("formtableselector", dataObj -> {
            if (dataObj instanceof JSONObject) {
                JSONObject tableObj = (JSONObject) dataObj;
                return createTableHtml(tableObj);
            } else {
                return StringUtils.EMPTY;
            }
        });
        // 表格输入
        map.put("formtableinputer", dataObj -> {
            if (dataObj instanceof JSONObject) {
                JSONObject tableObj = (JSONObject) dataObj;
                return createNestedTableHtml(tableObj);
            } else {
                return StringUtils.EMPTY;
            }
        });
    }

    /**
     * 生成表格，表单的帐号、配置项修改、表格选择等组件需要用到表格
     * @param tableObj
     * @return
     */
    private static String createTableHtml(JSONObject tableObj) {
        JSONArray theadList = tableObj.getJSONArray("theadList");
        if (CollectionUtils.isEmpty(theadList)) {
            return StringUtils.EMPTY;
        }
        StringBuilder stringBuilder = new StringBuilder("<table border=\"1\">");
        stringBuilder.append("<tr>");
        List<String> keyList = new ArrayList<>();
        for (int i = 0; i < theadList.size(); i++) {
            JSONObject theadObj = theadList.getJSONObject(i);
            if (MapUtils.isEmpty(theadObj)) {
                continue;
            }
            String title = theadObj.getString("title");
            String key = theadObj.getString("key");
            if (StringUtils.isBlank(title) || StringUtils.isBlank(key)) {
                continue;
            }
            stringBuilder.append("<th>");
            stringBuilder.append(title);
            stringBuilder.append("</th>");
            keyList.add(key);
        }
        stringBuilder.append("</tr>");
        JSONArray tbodyList = tableObj.getJSONArray("tbodyList");
        if (CollectionUtils.isNotEmpty(tbodyList)) {
            for (int i = 0; i < tbodyList.size(); i++) {
                JSONObject tbodyObj = tbodyList.getJSONObject(i);
                if (MapUtils.isEmpty(tbodyObj)) {
                    continue;
                }
                stringBuilder.append("<tr>");
                for (String key : keyList) {
                    String value = tbodyObj.getString(key);
                    if (value == null) {
                        value = StringUtils.EMPTY;
                    }
                    stringBuilder.append("<td>");
                    stringBuilder.append(value);
                    stringBuilder.append("</td>");
                }
                stringBuilder.append("</tr>");
            }
        }
        stringBuilder.append("</table>");
        return stringBuilder.toString();
    }

    /**
     * 生成嵌套表格，表单的表格输入组件需要用到嵌套表格
     * @param tableObj
     * @return
     */
    private static String createNestedTableHtml(JSONObject tableObj) {
        JSONArray theadList = tableObj.getJSONArray("theadList");
        if (CollectionUtils.isEmpty(theadList)) {
            return StringUtils.EMPTY;
        }
        StringBuilder stringBuilder = new StringBuilder("<table border=\"1\">");
        stringBuilder.append("<tr>");
        Map<String, String> keyToHandlerMap = new HashMap<>();
        List<String> keyList = new ArrayList<>();
        for (int i = 0; i < theadList.size(); i++) {
            JSONObject theadObj = theadList.getJSONObject(i);
            if (MapUtils.isEmpty(theadObj)) {
                continue;
            }
            String title = theadObj.getString("title");
            String key = theadObj.getString("key");
            String handler = theadObj.getString("handler");
            if (StringUtils.isBlank(title) || StringUtils.isBlank(key) || StringUtils.isBlank(handler)) {
                continue;
            }
            stringBuilder.append("<th>");
            stringBuilder.append(title);
            stringBuilder.append("</th>");
            keyList.add(key);
            keyToHandlerMap.put(key, handler);
        }
        stringBuilder.append("</tr>");
        JSONArray tbodyList = tableObj.getJSONArray("tbodyList");
        if (CollectionUtils.isNotEmpty(tbodyList)) {
            for (int i = 0; i < tbodyList.size(); i++) {
                JSONObject tbodyObj = tbodyList.getJSONObject(i);
                if (MapUtils.isEmpty(tbodyObj)) {
                    continue;
                }
                stringBuilder.append("<tr>");
                for (String key : keyList) {
                    stringBuilder.append("<td>");
                    String handler = keyToHandlerMap.get(key);
                    if (Objects.equals(handler, "formtable")) {
                        JSONObject cellTableObj = tbodyObj.getJSONObject(key);
                        if (MapUtils.isNotEmpty(cellTableObj)) {
                            stringBuilder.append(createTableHtml(cellTableObj));
                        } else {
                            stringBuilder.append(StringUtils.EMPTY);
                        }
                    } else {
                        String value = tbodyObj.getString(key);
                        if (value == null) {
                            value = StringUtils.EMPTY;
                        }
                        stringBuilder.append(value);
                    }
                    stringBuilder.append("</td>");
                }
                stringBuilder.append("</tr>");
            }
        }
        stringBuilder.append("</table>");
        return stringBuilder.toString();
    }
    // 表单组件数据列表
    private List<AttributeDataVo> attributeDataList;

    public FormTable(List<AttributeDataVo> attributeDataList) {
        this.attributeDataList = attributeDataList;
    }

    @Override
    public Object exec(List arguments) throws TemplateModelException {
        if (CollectionUtils.isEmpty(attributeDataList)) {
            return StringUtils.EMPTY;
        }
        StringBuilder stringBuilder = new StringBuilder("<table border=\"1\" width=\"100%\">");
        for (AttributeDataVo attributeDataVo : attributeDataList) {
            if (attributeDataVo == null) {
                continue;
            }
            String attributeLabel = attributeDataVo.getAttributeLabel();
            String type = attributeDataVo.getType();
            Object dataObj = attributeDataVo.getDataObj();
            String result = StringUtils.EMPTY;
            if (dataObj != null) {
                Function<Object, String> function = map.get(type);
                if (function != null) {
                    result = function.apply(dataObj);
                }
            }
            stringBuilder.append("<tr>");
            stringBuilder.append("<td class=\"label\">");
            stringBuilder.append(attributeLabel);
            stringBuilder.append("</td>");
            stringBuilder.append("<td>");
            stringBuilder.append(result);
            stringBuilder.append("</td>");
            stringBuilder.append("</tr>");
        }
        stringBuilder.append("</table>");
        return stringBuilder.toString();
    }
}