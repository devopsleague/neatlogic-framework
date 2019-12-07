package codedriver.framework.restful.core;

import java.lang.reflect.Method;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONReader;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.util.IpUtil;
import codedriver.framework.restful.dao.mapper.ApiMapper;
import codedriver.framework.restful.dto.ApiVo;

public abstract class JsonStreamApiComponentBase extends ApiHelpBase implements JsonStreamApiComponent, MyJsonStreamApiComponent {
	private static Logger logger = LoggerFactory.getLogger(JsonStreamApiComponentBase.class);

	@Autowired
	private ApiMapper restMapper;

	@Override
	public final Object doService(ApiVo interfaceVo, JSONObject paramObj, JSONReader jsonReader) throws Exception {
		String error = "";
		Object result = null;
		long startTime = System.currentTimeMillis();
		String requestIp = IpUtil.getIpAddr(UserContext.get().getRequest());
		Boolean status = false;
		// audit.setParam(jsonObj.toString(4));
		try {
			try {
				Object proxy = AopContext.currentProxy();
				Method method = proxy.getClass().getMethod("myDoService", JSONObject.class, JSONReader.class);
				result = method.invoke(proxy, paramObj, jsonReader);
			} catch (IllegalStateException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException ex) {
				result = myDoService(paramObj, jsonReader);
			} catch (Exception ex) {
				throw ex;
			}
			status = true;
		} catch (Exception e) {
			error = e.getMessage() == null ? ExceptionUtils.getStackTrace(e) : e.getMessage();
			throw e;
		} finally {

		}
		return result;
	}

	public final String getId() {
		return this.getClass().getName();
	}

	@Override
	public final JSONObject help() {
		return getApiComponentHelp(JSONObject.class, JSONReader.class);
	}

}
