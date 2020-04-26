package codedriver.framework.integration.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;

import codedriver.framework.apiparam.core.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.integration.core.IIntegrationHandler;
import codedriver.framework.integration.core.IntegrationHandlerFactory;
import codedriver.framework.restful.annotation.EntityField;

public class IntegrationVo extends BasePageVo {
	@EntityField(name = "uuid", type = ApiParamType.STRING)
	private String uuid;
	@EntityField(name = "名称", type = ApiParamType.STRING)
	private String name;
	@EntityField(name = "目标地址", type = ApiParamType.STRING)
	private String url;
	@EntityField(name = "组件", type = ApiParamType.STRING)
	private String handler;
	@EntityField(name = "组件名称", type = ApiParamType.STRING)
	private String handlerName;
	@EntityField(name = "请求方法", type = ApiParamType.STRING)
	private String method;
	@EntityField(name = "输入参数模板", type = ApiParamType.JSONARRAY)
	private List<PatternVo> inputPatternList = new ArrayList<>();
	@EntityField(name = "输出参数模板", type = ApiParamType.JSONARRAY)
	private List<PatternVo> outputPatternList = new ArrayList<>();
	@EntityField(name = "配置", type = ApiParamType.JSONOBJECT)
	private String config;
	@JSONField(serialize = false)
	private transient JSONObject configObj;
	// 请求参数
	@JSONField(serialize = false)
	private transient JSONObject paramObj;
	@EntityField(name = "创建人", type = ApiParamType.STRING)
	private String fcu;
	@EntityField(name = "创建时间", type = ApiParamType.INTEGER)
	private Date fcd;
	@EntityField(name = "修改人", type = ApiParamType.STRING)
	private String lcu;
	@EntityField(name = "修改时间", type = ApiParamType.INTEGER)
	private Date lcd;
	@JSONField(serialize = false)
	private transient String keyword;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUuid() {
		if (StringUtils.isBlank(uuid)) {
			uuid = UUID.randomUUID().toString().replace("-", "");
		}
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getHandler() {
		return handler;
	}

	public void setHandler(String handler) {
		this.handler = handler;
	}

	public String getHandlerName() {
		if (StringUtils.isBlank(handlerName) && StringUtils.isNotBlank(handler)) {
			IIntegrationHandler<?> integrationHandler = IntegrationHandlerFactory.getHandler(handler);
			if (integrationHandler != null) {
				handlerName = integrationHandler.getName();
			}
		}
		return handlerName;
	}

	public void setHandlerName(String handlerName) {
		this.handlerName = handlerName;
	}

	@JSONField(serialize = false)
	public String getConfigStr() {
		return config;
	}

	public void setConfig(String config) {
		this.config = config;
	}

	public JSONObject getConfig() {
		if (configObj == null && StringUtils.isNotBlank(config)) {
			try {
				configObj = JSONObject.parseObject(config);
			} catch (Exception ex) {

			}
		}
		return configObj;
	}

	public JSONObject getParamObj() {
		return paramObj;
	}

	public void setParamObj(JSONObject paramObj) {
		this.paramObj = paramObj;
	}

	public String getFcu() {
		return fcu;
	}

	public void setFcu(String fcu) {
		this.fcu = fcu;
	}

	public Date getFcd() {
		return fcd;
	}

	public void setFcd(Date fcd) {
		this.fcd = fcd;
	}

	public String getLcu() {
		return lcu;
	}

	public void setLcu(String lcu) {
		this.lcu = lcu;
	}

	public Date getLcd() {
		return lcd;
	}

	public void setLcd(Date lcd) {
		this.lcd = lcd;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public List<PatternVo> getInputPatternList() {
		return inputPatternList;
	}

	public void setInputPatternList(List<PatternVo> inputPatternList) {
		this.inputPatternList = inputPatternList;
	}

	public List<PatternVo> getOutputPatternList() {
		return outputPatternList;
	}

	public void setOutputPatternList(List<PatternVo> outputPatternList) {
		this.outputPatternList = outputPatternList;
	}

}
