package codedriver.framework.exception.integration;

import codedriver.framework.exception.core.ApiRuntimeException;

public class ParamFormatInvalidException extends ApiRuntimeException {

	private static final long serialVersionUID = 1061691112349475176L;

	public ParamFormatInvalidException() {
		super("参数不符合JSON格式");
	}
}
