package codedriver.framework.auth.core;

import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

public abstract class AuthBase {
	public final String getAuthName() {
		return this.getClass().getSimpleName();
	}

	public abstract String getAuthDisplayName();

	public abstract String getAuthIntroduction();

	public abstract String getAuthGroup();

	public abstract Integer getSort();

	/**
	 * 标记 用于标识base
	 * @return true|false
	 */
	public boolean getIsDefault(){
		return false;
	}

	public List<Class<? extends AuthBase>> getIncludeAuths(){
		return (List<Class<? extends AuthBase>>) CollectionUtils.EMPTY_COLLECTION;
	}

}
