package codedriver.framework.restful.core;

import org.springframework.util.ClassUtils;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.restful.dto.ApiVo;

public interface IApiComponent {

	/**
	 * @Author: chenqiwei
	 * @Time:Jun 19, 2020
	 * @Description: 实现类全名
	 * @param @return
	 * @return String
	 */
	public default String getClassName() {
		return ClassUtils.getUserClass(this.getClass()).getName();
	}

	/**
	 * @Author: chenqiwei
	 * @Time:Jun 19, 2020
	 * @Description: true时返回格式不再包裹固定格式，固定格式是:{Status:"OK",Return:{},Message:"error"}
	 * @param @return
	 * @return boolean
	 */
	public default boolean isRaw() {
		return false;
	}

	/**
	 * @Author: chenqiwei
	 * @Time:Jun 19, 2020
	 * @Description: 接口中文名
	 * @param @return
	 * @return String
	 */
	public String getName();

	/**
	 * @Author: chenqiwei
	 * @Time:Jun 19, 2020
	 * @Description: 额外配置
	 * @param @return
	 * @return String
	 */
	public String getConfig();

	/**
	 * @Author: chenqiwei
	 * @Time:Jun 19, 2020
	 * @Description: 是否需要审计
	 * @param @return
	 * @return int
	 */
	public int needAudit();

	/**
	 * @Author: chenqiwei
	 * @Time:Jun 19, 2020
	 * @Description: 服务主入口
	 * @param @param  apiVo
	 * @param @param  jsonObj
	 * @param @return
	 * @param @throws Exception
	 * @return Object
	 */
	public Object doService(ApiVo apiVo, JSONObject jsonObj) throws Exception;

	/**
	 * @Author: chenqiwei
	 * @Time:Jun 19, 2020
	 * @Description: 获取帮助信息
	 * @param @return
	 * @return JSONObject
	 */
	public JSONObject help();
}
