package codedriver.framework.exception.solution;

import codedriver.framework.exception.core.ApiRuntimeException;

public class SolutionRepeatException extends ApiRuntimeException {

	private static final long serialVersionUID = 7578574406227399198L;

	public SolutionRepeatException(String name) {
		super("解决方案：" + name + "已存在");
	}


}
