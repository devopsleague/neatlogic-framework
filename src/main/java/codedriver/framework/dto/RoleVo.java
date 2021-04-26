package codedriver.framework.dto;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;
import com.alibaba.fastjson.annotation.JSONField;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class RoleVo extends BasePageVo implements Serializable {

	private static final long serialVersionUID = -8007028390813552667L;

	public static final String USER_DEFAULT_ROLE = "R_SYSTEM_USER";
	@JSONField(serialize=false)
	private transient String keyword;
	private Long id;
	@EntityField(name = "角色uuid",
			type = ApiParamType.STRING)
	private String uuid;
	@EntityField(name = "角色名称",
			type = ApiParamType.STRING)
	private String name;
	@EntityField(name = "角色描述",
			type = ApiParamType.STRING)
	private String description;
	private int userCount;
	private String authGroup;
	private String auth;
	private List<String> userUuidList;
	private List<RoleAuthVo> roleAuthList;

	/**
	 * 此字段专供前端使用，用于渲染头像时区分对象类型，取值范围[user,team,role]
	 */
	@EntityField(name = "前端初始化类型，取值范围[user,team,role]", type = ApiParamType.STRING)
	private final String initType = GroupSearch.ROLE.getValue();

	public RoleVo() {
	}

    public RoleVo(String uuid) {
    	this.uuid = uuid;
    }

    public List<RoleAuthVo> getRoleAuthList() {
		return roleAuthList;
	}

	public List<String> getUserUuidList() {
		return userUuidList;
	}

	public void setUserUuidList(List<String> userUuidList) {
		this.userUuidList = userUuidList;
	}

	public void setRoleAuthList(List<RoleAuthVo> roleAuthList) {
		this.roleAuthList = roleAuthList;
	}

	public String getAuthGroup() {
		return authGroup;
	}

	public void setAuthGroup(String authGroup) {
		this.authGroup = authGroup;
	}

	public String getAuth() {
		return auth;
	}

	public void setAuth(String auth) {
		this.auth = auth;
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

	public synchronized String getUuid() {
		if (StringUtils.isBlank(uuid)) {
			uuid = UUID.randomUUID().toString().replace("-", "");
		}
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getUserCount() {
		return userCount;
	}

	public void setUserCount(int userCount) {
		this.userCount = userCount;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public String getInitType() {
		return initType;
	}
}
