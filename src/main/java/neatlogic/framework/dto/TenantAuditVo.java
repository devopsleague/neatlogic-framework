/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.framework.dto;

import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.common.util.ModuleUtil;
import neatlogic.framework.dto.module.ModuleGroupVo;
import neatlogic.framework.dto.module.ModuleVo;
import neatlogic.framework.restful.annotation.EntityField;
import neatlogic.framework.util.SnowflakeUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.Date;

public class TenantAuditVo extends BasePageVo {
	public enum Status {
		DOING, DONE;
	}

	@EntityField(name = "id", type = ApiParamType.LONG)
	private Long id;
	@EntityField(name = "nmmat.tenantauditgetapi.input.groupid", type = ApiParamType.LONG)
	private Long groupId;
	@EntityField(name = "common.tenantuuid", type = ApiParamType.STRING)
	private String tenantUuid;
	@EntityField(name = "term.cmdb.moduleid", type = ApiParamType.STRING)
	private String moduleId;
	@EntityField(name = "common.module.name", type = ApiParamType.STRING)
	private String moduleName;
	@EntityField(name = "common.module.group", type = ApiParamType.STRING)
	private String moduleGroup;
	@EntityField(name = "common.module.groupname", type = ApiParamType.STRING)
	private String moduleGroupName;
	@EntityField(name = "nfd.tenantauditvo.sqltype", type = ApiParamType.STRING)
	private String sqlType;
	@EntityField(name = "common.starttime", type = ApiParamType.LONG)
	private Date startTime;
	@EntityField(name = "common.endtime", type = ApiParamType.LONG)
	private Date endTime;
	@EntityField(name = "nfd.tenantauditvo.timecost", type = ApiParamType.LONG)
	private Long timeCost;
	@EntityField(name = "common.status", type = ApiParamType.STRING)
	private String status;
	@EntityField(name = "nfd.tenantauditvo.result", type = ApiParamType.STRING)
	private String result;
	@EntityField(name = "nfd.tenantauditvo.error", type = ApiParamType.STRING)
	private String error;
	@EntityField(name = "nfd.tenantauditvo.resulthash", type = ApiParamType.STRING)
	private String resultHash;
	@EntityField(name = "nfd.tenantauditvo.errorhash", type = ApiParamType.STRING)
	private String errorHash;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Long getId() {
		if (id == null) {
			id = SnowflakeUtil.uniqueLong();
		}
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public String getResultHash() {
		if (StringUtils.isBlank(resultHash) && StringUtils.isNotBlank(result)) {
			resultHash = DigestUtils.md5DigestAsHex(result.getBytes());
		}
		return resultHash;
	}

	public void setResultHash(String resultHash) {
		this.resultHash = resultHash;
	}

	public String getErrorHash() {
		if (StringUtils.isBlank(errorHash) && StringUtils.isNotBlank(error)) {
			errorHash = DigestUtils.md5DigestAsHex(error.getBytes());
		}
		return errorHash;
	}

	public void setErrorHash(String errorHash) {
		this.errorHash = errorHash;
	}

	public String getTenantUuid() {
		return tenantUuid;
	}

	public void setTenantUuid(String tenantUuid) {
		this.tenantUuid = tenantUuid;
	}

	public Long getTimeCost() {
		return timeCost;
	}

	public void setTimeCost(Long timeCost) {
		this.timeCost = timeCost;
	}

	public String getModuleGroup() {
		return moduleGroup;
	}

	public void setModuleGroup(String moduleGroup) {
		this.moduleGroup = moduleGroup;
	}

	public Long getGroupId() {
		return groupId;
	}

	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}

	public String getModuleName() {
		if (StringUtils.isNotBlank(this.moduleId)) {
			ModuleVo moduleVo = ModuleUtil.getModuleById(this.moduleId);
			if (moduleVo != null) {
				moduleName = moduleVo.getName();
			}
		}
		return moduleName;
	}

	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	public String getModuleGroupName() {
		if (StringUtils.isNotBlank(this.moduleGroup)) {
			ModuleGroupVo moduleGroupVo = ModuleUtil.getModuleGroup(this.moduleGroup);
			if (moduleGroupVo != null) {
				this.moduleGroupName = moduleGroupVo.getGroupName();
			}
		}
		return moduleGroupName;
	}

	public void setModuleGroupName(String moduleGroupName) {
		this.moduleGroupName = moduleGroupName;
	}

	public String getSqlType() {
		return sqlType;
	}

	public void setSqlType(String sqlType) {
		this.sqlType = sqlType;
	}
}
