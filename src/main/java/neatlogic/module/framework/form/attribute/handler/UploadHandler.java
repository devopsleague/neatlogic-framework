/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

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

package neatlogic.module.framework.form.attribute.handler;

import neatlogic.framework.common.constvalue.ParamType;
import neatlogic.framework.form.attribute.core.FormHandlerBase;
import neatlogic.framework.form.constvalue.FormConditionModel;
import neatlogic.framework.form.constvalue.FormHandler;
import neatlogic.framework.form.dto.AttributeDataVo;
import neatlogic.framework.form.dto.FormAttributeVo;
import neatlogic.framework.form.exception.AttributeValidException;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UploadHandler extends FormHandlerBase {

    @Override
    public String getHandler() {
        return FormHandler.FORMUPLOAD.getHandler();
    }

    @Override
    public String getHandlerName() {
        return FormHandler.FORMUPLOAD.getHandlerName();
    }

    @Override
    public String getHandlerType(FormConditionModel model) {
        return "formupload";
    }

    @Override
    public String getIcon() {
        return "tsfont-upload";
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
    public boolean isAudit() {
        return true;
    }

    @Override
    public boolean isConditionable() {
        return false;
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
    public boolean isForTemplate() {
        return false;
    }

    @Override
    public boolean isProcessTaskBatchSubmissionTemplateParam() {
        return false;
    }

    @Override
    public String getModule() {
        return "framework";
    }

    @Override
    public JSONObject valid(AttributeDataVo attributeDataVo, JSONObject configObj) throws AttributeValidException {
        return null;
    }

    @Override
    public Object valueConversionText(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONArray dataArray = (JSONArray) attributeDataVo.getDataObj();
        if (CollectionUtils.isNotEmpty(dataArray)) {
            List<String> nameList = new ArrayList<>();
            for (int i = 0; i < dataArray.size(); i++) {
                JSONObject dataObj = dataArray.getJSONObject(i);
                if (MapUtils.isNotEmpty(dataObj)) {
                    String name = dataObj.getString("name");
                    if (StringUtils.isNotBlank(name)) {
                        nameList.add(name);
                    }
                }
            }
            return nameList;
        }
        return null;
    }

    @Override
    public Object dataTransformationForEmail(AttributeDataVo attributeDataVo, JSONObject configObj) {
        return getMyDetailedData(attributeDataVo, configObj);
    }

    @Override
    public Object textConversionValue(Object text, JSONObject config) {
        return null;
    }

    @Override
    public int getSort() {
        return 18;
    }

    @Override
    public void makeupFormAttribute(FormAttributeVo formAttributeVo) {

    }

    //表单组件配置信息
//{
//	"handler": "formupload",
//	"label": "附件上传_1",
//	"type": "form",
//	"uuid": "090ae87ae1c24950b1ea039d45ab0a85",
//	"config": {
//		"isRequired": false,
//		"defaultValueList": [],
//		"isTemplate": false,
//		"ruleList": [],
//		"width": "100%",
//		"uploadType": "one",
//		"validList": [],
//		"quoteUuid": "",
//		"defaultValueType": "self",
//		"placeholder": "选择上传的附件",
//		"authorityConfig": [
//			"common#alluser"
//		],
//		"value": null
//	}
//}
//保存数据
//[{"name":"asd.jpg","id":623050252673024}]
//返回数据结构
//{
//    "value": [{"name":"asd.jpg","id":623050252673024}],
//    "text": ["asd.jpg"]
//}
    @Override
    protected JSONObject getMyDetailedData(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject resultObj = new JSONObject();
        JSONArray dataArray = (JSONArray) attributeDataVo.getDataObj();
        if (CollectionUtils.isNotEmpty(dataArray)) {
            resultObj.put("value", dataArray);
            List<String> nameList = new ArrayList<>();
            for (int i = 0; i < dataArray.size(); i++) {
                JSONObject dataObj = dataArray.getJSONObject(i);
                if (MapUtils.isNotEmpty(dataObj)) {
                    String name = dataObj.getString("name");
                    if (StringUtils.isNotBlank(name)) {
                        nameList.add(name);
                    }
                }
            }
            resultObj.put("text", nameList);
            return resultObj;
        }
        return resultObj;
    }

    @Override
    public Object dataTransformationForExcel(AttributeDataVo attributeDataVo, JSONObject configObj) {
        return null;
    }
}
