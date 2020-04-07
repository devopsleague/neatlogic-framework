package codedriver.framework.dto;

import codedriver.framework.apiparam.core.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;

public class UserProfileVo implements Cloneable{
	@EntityField(name = "个性化所属用户",
			type = ApiParamType.STRING)
	private String userId;
	@EntityField(name = "个性化所属模块id",
			type = ApiParamType.STRING)
	private String moduleId;
	@EntityField(name = "个性化所属模块名",
			type = ApiParamType.STRING)
	private String moduleName;
	@EntityField(name = "个性化配置,userProfileOperateList:用户操作列表; userProfileOperateList.value:具体用户操作名; "
			+ "userProfileOperateList.text:具体用户操作显示名; userProfileOperateList.check:具体用户操作是否选中，1选中，0未选中;"
			+ "text:用户操作显示名;value:用户操作名;checked:用户操作是否选中，1选中，0未选中",
			type = ApiParamType.STRING)
	private String config;
	
	public UserProfileVo() {
		
	}
	
	public UserProfileVo(String userId, String moduleId, String config) {
		this.userId = userId;
		this.moduleId = moduleId;
		this.config = config;
	}
	
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getModuleId() {
		return moduleId;
	}
	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}
	public String getConfig() {
		return config;
	}
	public void setConfig(String config) {
		this.config = config;
	}

	public String getModuleName() {
		return moduleName;
	}

	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}
	
	public Object clone() throws CloneNotSupportedException{
	    return super.clone();
	}

}
