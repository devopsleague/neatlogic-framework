/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.framework.apiparam.validator;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.param.validate.core.ApiParamValidatorBase;
import org.apache.commons.lang3.StringUtils;

public class DoubleApiParam extends ApiParamValidatorBase {

    @Override
    public String getName() {
        return "双精度浮点数";
    }

    @Override
    public boolean validate(Object param, String rule) {
        if (param != null && StringUtils.isNotBlank(param.toString())) {
            try {
                Double.valueOf(param.toString());
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ApiParamType getType() {
        return ApiParamType.DOUBLE;
    }

}
