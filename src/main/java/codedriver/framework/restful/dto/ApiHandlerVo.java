package codedriver.framework.restful.dto;

import java.util.List;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;

public class ApiHandlerVo {
	@EntityField(name = "处理器", type = ApiParamType.STRING)
	private String handler;
	@EntityField(name = "名称", type = ApiParamType.STRING)
	private String name;
	@EntityField(name = "配置信息，json格式", type = ApiParamType.JSONOBJECT)
	private String config;
	@EntityField(name = "状态", type = ApiParamType.INTEGER)
	private Integer isActive;
	@EntityField(name = "模块ID", type = ApiParamType.STRING)
	private String moduleId;
	@EntityField(name = "是否是私有接口", type = ApiParamType.BOOLEAN)
	private boolean isPrivate;
	@EntityField(name = "处理器类型", type = ApiParamType.STRING)
	private String type;
	private List<ApiVo> interfaceList;

	public String getConfig() {
		return config;
	}

	public void setConfig(String config) {
		this.config = config;
	}

	public List<ApiVo> getInterfaceList() {
		return interfaceList;
	}

	public void setInterfaceList(List<ApiVo> interfaceList) {
		this.interfaceList = interfaceList;
	}

	public String getHandler() {
		return handler;
	}

	public void setHandler(String handler) {
		this.handler = handler;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getIsActive() {
		return isActive;
	}

	public void setIsActive(Integer isActive) {
		this.isActive = isActive;
	}

	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}

	public boolean isPrivate() {
		return isPrivate;
	}

	public void setPrivate(boolean isPrivate) {
		this.isPrivate = isPrivate;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
