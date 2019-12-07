package codedriver.framework.exception.core;

public class ApiRuntimeException extends RuntimeException {
	private static final long serialVersionUID = 9206337410118158624L;
	private String errorCode;

	public ApiRuntimeException() {
		super();
	}

	public ApiRuntimeException(String message) {
		super(message);
	}

	public ApiRuntimeException(IApiExceptionMessage exception) {
		super(exception.getError());
		this.errorCode = exception.getErrorCode();
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

}
