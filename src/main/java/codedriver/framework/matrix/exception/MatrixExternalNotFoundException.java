package codedriver.framework.matrix.exception;

import codedriver.framework.exception.core.ApiRuntimeException;

public class MatrixExternalNotFoundException extends ApiRuntimeException {

	private static final long serialVersionUID = 1708428607162097984L;

	public MatrixExternalNotFoundException(String name) {
		super("矩阵外部数据源：'" + name + "'信息不存在");
	}
}
