package codedriver.framework.api.param;

import java.util.regex.Pattern;

import codedriver.framework.api.core.ApiParamBase;
import codedriver.framework.api.core.ApiParamType;

public class TelephoneApiParam extends ApiParamBase {

	@Override
	public String getAuthName() {

		return "手机号码参数认证";
	}

	@Override
	public boolean doAuth(String param) {
		Pattern pattern = Pattern.compile("^1(3|4|5|7|8)\\d{9}$");
		return pattern.matcher(param).matches();
	}

	@Override
	public ApiParamType getAuthType() {
		return ApiParamType.TELEPHONE;
	}

}
