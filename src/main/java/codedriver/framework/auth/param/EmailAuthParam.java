package codedriver.framework.auth.param;

import java.util.regex.Pattern;

public class EmailAuthParam extends AuthParamBase {

	@Override
	public String getAuthName() {

		return "邮箱参数认证";
	}

	@Override
	public boolean doAuth(String param) {
		Pattern pattern = Pattern.compile("^[A-Za-z0-9]+([_\\.][A-Za-z0-9]+)*@([A-Za-z0-9\\-]+\\.)+[A-Za-z]{2,6}$");
		return pattern.matcher(param).matches();
	}

	@Override
	public AuthParamType getAuthType() {
		return AuthParamType.EMAIL;
	}

}
