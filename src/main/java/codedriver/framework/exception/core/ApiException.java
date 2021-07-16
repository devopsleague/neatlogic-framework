/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.framework.exception.core;

import codedriver.framework.asynchronization.threadlocal.RequestContext;
import codedriver.framework.asynchronization.threadlocal.TenantContext;

public class ApiException extends Exception {
    private static final long serialVersionUID = 9163793348080280240L;
    private String errorCode;

    public ApiException() {
        super();
    }

    public ApiException(Throwable ex) {
        super(ex);
    }

    public ApiException(String message, Throwable ex) {
        super((TenantContext.get() != null ? TenantContext.get().getTenantUuid() : "-") + ":" + (RequestContext.get() != null ? RequestContext.get().getUrl() : "-") + ":::::::" + message, ex);
    }

    public ApiException(String message) {
        super((TenantContext.get() != null ? TenantContext.get().getTenantUuid() : "-") + ":" + (RequestContext.get() != null ? RequestContext.get().getUrl() : "-") + ":::::::" + message);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage(boolean clear) {
        String message = super.getMessage();
        if (message.contains(":::::::")) {
            return message.split(":::::::")[1];
        } else {
            return message;
        }
    }

}
