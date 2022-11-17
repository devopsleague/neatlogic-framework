/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.framework.form.attribute.handler;

import codedriver.framework.common.constvalue.ParamType;
import codedriver.framework.form.attribute.core.FormHandlerBase;
import codedriver.framework.form.attribute.core.INewAttribute;
import codedriver.framework.form.constvalue.FormConditionModel;
import codedriver.framework.form.constvalue.FormHandler;
import codedriver.framework.form.dto.AttributeDataVo;
import codedriver.framework.form.exception.AttributeValidException;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CkEditorHandler extends FormHandlerBase implements INewAttribute {

    @Override
    public String getHandler() {
        return FormHandler.FORMCKEDITOR.getHandler();
    }

    @Override
    public String getHandlerType(FormConditionModel model) {
        return "input";//富文本不管是哪种模式下过滤都是input
    }

    @Override
    public JSONObject valid(AttributeDataVo attributeDataVo, JSONObject configObj) throws AttributeValidException {
        return null;
    }

    @Override
    public Object valueConversionText(AttributeDataVo attributeDataVo, JSONObject configObj) {
        return attributeDataVo.getDataObj();
    }

    @Override
    public Object dataTransformationForEmail(AttributeDataVo attributeDataVo, JSONObject configObj) {
        return attributeDataVo.getDataObj();
    }

    @Override
    public Object textConversionValue(List<String> values, JSONObject config) {
        if (CollectionUtils.isNotEmpty(values)) {
            return values.get(0);
        }
        return null;
    }

    @Override
    public ParamType getParamType() {
        return ParamType.STRING;
    }

    @Override
    public boolean isConditionable() {
        return true;
    }

    @Override
    public boolean isAudit() {
        return true;
    }

    @Override
    public Boolean isUseFormConfig() {
        return false;
    }

    //表单组件配置信息
//{
//	"handler": "formeditor",
//	"label": "富文本框_3",
//	"type": "form",
//	"uuid": "9b6e8d7342e44127b224af410fc1b5aa",
//	"config": {
//		"isRequired": false,
//		"editorMaxlength": "",
//		"defaultValueList": "文本c",
//		"ruleList": [],
//		"width": "100%",
//		"validList": [],
//		"quoteUuid": "",
//		"defaultValueType": "self",
//		"placeholder": "请输入",
//		"authorityConfig": [
//			"common#alluser"
//		]
//	}
//}
    //保存数据结构
//    "文本c"
    //返回数据结构
//{
//	"value": "文本c"
//}
    @Override
    protected JSONObject getMyDetailedData(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject resultObj = new JSONObject();
        resultObj.put("value", attributeDataVo.getDataObj());
        return resultObj;
    }

    @Override
    public Object dataTransformationForExcel(AttributeDataVo attributeDataVo, JSONObject configObj) {
        return attributeDataVo.getDataObj();
    }
}
