package codedriver.framework.api.param;

import codedriver.framework.api.core.ApiParamBase;
import codedriver.framework.api.core.ApiParamType;

public class LongApiParam extends ApiParamBase {

	@Override
	public String getName() {

		return "整数";
	}

	@Override
	public boolean validate(Object param) {
		try {
			Long.valueOf(param.toString());
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	@Override
	public ApiParamType getType() {
		return ApiParamType.LONG;
	}

}
