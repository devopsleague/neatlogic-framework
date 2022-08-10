/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.framework.restful.core;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONReader;

import codedriver.framework.restful.dto.ApiVo;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface IJsonStreamApiComponent {

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    String getId();

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    String getName();

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    String getConfig();

    // true时返回格式不再包裹固定格式
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    default boolean isRaw() {
        return false;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    int needAudit();

    Object doService(ApiVo interfaceVo, JSONObject paramObj, JSONReader jsonReader) throws Exception;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    JSONObject help();

    /**
     * 是否支持匿名访问
     *
     * @return
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    default boolean supportAnonymousAccess() {
        return false;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    default boolean disableReturnCircularReferenceDetect() {
        return false;
    }
}
