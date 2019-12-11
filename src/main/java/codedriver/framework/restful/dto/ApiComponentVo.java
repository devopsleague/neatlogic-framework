package codedriver.framework.restful.dto;

import java.util.List;

public class ApiComponentVo {
	private String id;
	private String name;
	private String config;
	private Integer isActive;
	private String moduleId;

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

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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
}
