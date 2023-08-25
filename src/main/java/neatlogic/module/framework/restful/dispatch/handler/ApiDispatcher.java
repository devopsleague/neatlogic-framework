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

package neatlogic.module.framework.restful.dispatch.handler;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONReader;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import neatlogic.framework.asynchronization.threadlocal.InputFromContext;
import neatlogic.framework.asynchronization.threadlocal.RequestContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.common.config.Config;
import neatlogic.framework.common.constvalue.InputFrom;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.exception.core.NotFoundEditTargetException;
import neatlogic.framework.exception.resubmit.ResubmitException;
import neatlogic.framework.exception.type.*;
import neatlogic.framework.restful.core.IApiComponent;
import neatlogic.framework.restful.core.IBinaryStreamApiComponent;
import neatlogic.framework.restful.core.IJsonStreamApiComponent;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentFactory;
import neatlogic.framework.restful.counter.ApiAccessCountUpdateThread;
import neatlogic.framework.restful.dao.mapper.ApiMapper;
import neatlogic.framework.restful.dto.ApiHandlerVo;
import neatlogic.framework.restful.dto.ApiVo;
import neatlogic.framework.restful.enums.ApiType;
import neatlogic.framework.restful.ratelimiter.RateLimiterTokenBucket;
import neatlogic.framework.util.$;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.framework.util.mongodb.IJsonSerializer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.HandlerMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Set;


@Controller
@RequestMapping("/api/")
public class ApiDispatcher {
    static Logger logger = LoggerFactory.getLogger(ApiDispatcher.class);

    @Resource
    private ApiMapper apiMapper;

    /*
      给fastJson加载自定义序列化配置，序列化json时返回正确格式
     */
    static {
        Reflections reflections = new Reflections("neatlogic");
        Set<Class<? extends IJsonSerializer>> enumClasses = reflections.getSubTypesOf(IJsonSerializer.class);
        for (Class<? extends IJsonSerializer> c : enumClasses) {
            try {
                SerializeConfig serializeConfig = SerializeConfig.getGlobalInstance();
                IJsonSerializer serializer = c.newInstance();
                serializeConfig.put(serializer.getType(), serializer);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }


    private void doIt(HttpServletRequest request, HttpServletResponse response, String token, ApiType apiType, JSONObject paramObj, JSONObject returnObj, String action) throws Exception {
        InputFromContext.init(InputFrom.PAGE);
        RequestContext.init(request, token, response);
        ApiVo interfaceVo = PrivateApiComponentFactory.getApiByToken(token);
        if (paramObj == null) {
            paramObj = new JSONObject();
        }
        if (interfaceVo == null) {
            interfaceVo = apiMapper.getApiByToken(token);
            if (interfaceVo == null || !interfaceVo.getIsActive().equals(1)) {
                throw new ApiNotFoundException(token);
            }
        } else if (interfaceVo.getPathVariableObj() != null) {
            // 融合路径参数
            paramObj.putAll(interfaceVo.getPathVariableObj());
        }

        // 判断是否master模块接口，如果是不允许访问
        ApiHandlerVo apiHandlerVo = PrivateApiComponentFactory.getApiHandlerByHandler(interfaceVo.getHandler());
        if (apiHandlerVo != null) {
            if (apiHandlerVo.getModuleId().equals("master")) {
                throw new PermissionDeniedException();
            }
        } else {
            throw new ComponentNotFoundException(interfaceVo.getHandler());
        }
        Double qps = interfaceVo.getQps();
        ApiVo apiVo = apiMapper.getApiByToken(token);
        if (apiVo != null) {
            qps = apiVo.getQps();
        }
        RequestContext.get().setApiRate(qps);
        //从令牌桶拿到令牌才能继续访问，否则直接返回，提示“系统繁忙，请稍后重试”
        if (!RateLimiterTokenBucket.tryAcquire()) {
            response.setStatus(429);
//            returnObj.put("Message", "系统繁忙，请稍后重试");
            JSONObject returnV = new JSONObject();
            returnV.put("rejectSource", RequestContext.get().getRejectSource().getValue());
            returnV.put("apiRate", RequestContext.get().getApiRate());
            returnV.put("tenantRate", RequestContext.get().getTenantRate());
            returnObj.put("Return", returnV);
            returnObj.put("Status", "ERROR");
            return;
        }
        //如果只是接口校验入参
        String validField = request.getHeader("neatlogic-validfield");
        if (StringUtils.isNotBlank(validField)) {
            IApiComponent restComponent = PrivateApiComponentFactory.getInstance(interfaceVo.getHandler());
            FieldValidResultVo validResultVo = restComponent.doValid(interfaceVo, paramObj, validField);
            if (StringUtils.isNotBlank(validResultVo.getMsg())) {
                response.setStatus(530);
                returnObj.put("Message", validResultVo.getMsg());
                if (validResultVo.getParam() != null) {
                    returnObj.put("Param", validResultVo.getParam());
                }
            }
            returnObj.put("Status", validResultVo.getStatus());
        } else {
            if (apiType.equals(ApiType.OBJECT)) {
                IApiComponent restComponent = PrivateApiComponentFactory.getInstance(interfaceVo.getHandler());
                if (restComponent != null) {
                    if (action.equals("doservice")) {
                        /* 统计接口访问次数 */
                        ApiAccessCountUpdateThread.putToken(token);
                        Long starttime = System.currentTimeMillis();
                        Object returnV = restComponent.doService(interfaceVo, paramObj, response);
                        Long endtime = System.currentTimeMillis();
                        if (!restComponent.isRaw()) {
                            returnObj.put("TimeCost", endtime - starttime);
                            returnObj.put("Return", returnV);
                            returnObj.put("Status", "OK");
                            if (restComponent.disableReturnCircularReferenceDetect()) {
                                returnObj.put("_disableDetect", true);
                            }
                        } else {
                            if (restComponent.disableReturnCircularReferenceDetect()) {
                                returnObj.putAll(JSONObject.parseObject(JSONObject.toJSONString(returnV, SerializerFeature.DisableCircularReferenceDetect)));
                            } else {
                                returnObj.putAll(JSONObject.parseObject(JSONObject.toJSONString(returnV)));
                            }
                        }
                    } else {
                        returnObj.putAll(restComponent.help());
                    }
                } else {
                    throw new ComponentNotFoundException("接口组件:" + interfaceVo.getHandler() + "不存在");
                }
            } else if (apiType.equals(ApiType.STREAM)) {
                IJsonStreamApiComponent restComponent = PrivateApiComponentFactory.getStreamInstance(interfaceVo.getHandler());
                if (restComponent != null) {
                    if (action.equals("doservice")) {
                        /* 统计接口访问次数 */
                        ApiAccessCountUpdateThread.putToken(token);
                        Long starttime = System.currentTimeMillis();
                        Object returnV = restComponent.doService(interfaceVo, paramObj, new JSONReader(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8)));
                        Long endtime = System.currentTimeMillis();
                        if (!restComponent.isRaw()) {
                            returnObj.put("TimeCost", endtime - starttime);
                            returnObj.put("Return", returnV);
                            returnObj.put("Status", "OK");
                            if (restComponent.disableReturnCircularReferenceDetect()) {
                                returnObj.put("_disableDetect", true);
                            }
                        } else {
                            returnObj.putAll(JSONObject.parseObject(JSONObject.toJSONString(returnV)));
                        }
                    } else {
                        returnObj.putAll(restComponent.help());
                    }
                } else {
                    throw new ComponentNotFoundException("接口组件:" + interfaceVo.getHandler() + "不存在");
                }
            } else if (apiType.equals(ApiType.BINARY)) {
                IBinaryStreamApiComponent restComponent = PrivateApiComponentFactory.getBinaryInstance(interfaceVo.getHandler());
                if (restComponent != null) {
                    if (action.equals("doservice")) {
                        /* 统计接口访问次数 */
                        ApiAccessCountUpdateThread.putToken(token);
                        Long starttime = System.currentTimeMillis();
                        Object returnV = restComponent.doService(interfaceVo, paramObj, request, response);
                        Long endtime = System.currentTimeMillis();
                        if (!restComponent.isRaw()) {
                            returnObj.put("TimeCost", endtime - starttime);
                            returnObj.put("Return", returnV);
                            returnObj.put("Status", "OK");
                            if (restComponent.disableReturnCircularReferenceDetect()) {
                                returnObj.put("_disableDetect", true);
                            }
                        } else {
                            returnObj.putAll(JSONObject.parseObject(JSONObject.toJSONString(returnV)));
                        }
                    } else {
                        returnObj.putAll(restComponent.help());
                    }
                } else {
                    throw new ComponentNotFoundException("接口组件:" + interfaceVo.getHandler() + "不存在");
                }
            }
        }
    }

    @RequestMapping(value = "/rest/**", method = RequestMethod.GET)
    public void dispatcherForGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String token = new AntPathMatcher().extractPathWithinPattern(pattern, request.getServletPath());
        JSONObject paramObj = new JSONObject();
        Enumeration<String> paraNames = request.getParameterNames();
        while (paraNames.hasMoreElements()) {
            String p = paraNames.nextElement();
            String[] vs = request.getParameterValues(p);
            if (vs.length > 1) {
                paramObj.put(p, vs);
            } else {
                paramObj.put(p, request.getParameter(p));
            }
        }
        JSONObject returnObj = new JSONObject();
        try {
            doIt(request, response, token, ApiType.OBJECT, paramObj, returnObj, "doservice");
        } catch (ApiRuntimeException ex) {
            response.setStatus(520);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
            if (ex.getParam() != null) {
                returnObj.put("Param", ex.getParam());
            }
        } catch (NotFoundEditTargetException ex) {
            response.setStatus(526);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
        } catch (PermissionDeniedException ex) {
            response.setStatus(523);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", $.t(ex.getMessage(), ex.getValues()));
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(500);
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ExceptionUtils.getStackFrames(ex));
        }
        if (!response.isCommitted()) {
            response.setContentType(Config.RESPONSE_TYPE_JSON);
            if (returnObj.containsKey("_disableDetect")) {
                returnObj.remove("_disableDetect");
                response.getWriter().print(returnObj.toString(SerializerFeature.DisableCircularReferenceDetect));
            } else {
                response.getWriter().print(returnObj);
            }
        }
    }

    @RequestMapping(value = "/rest/**", method = RequestMethod.POST)
    public void dispatcherForPost(@RequestBody String jsonStr, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String token = new AntPathMatcher().extractPathWithinPattern(pattern, request.getServletPath());
        JSONObject returnObj = new JSONObject();
        try {
            JSONObject paramObj;
            if (StringUtils.isNotBlank(jsonStr)) {
                try {
                    paramObj = JSONObject.parseObject(jsonStr);
                } catch (Exception e) {
                    throw new ParamJSONIrregularException();
                }
            } else {
                paramObj = new JSONObject();
            }

            Enumeration<String> paraNames = request.getParameterNames();
            while (paraNames.hasMoreElements()) {
                String p = paraNames.nextElement();
                String[] vs = request.getParameterValues(p);
                if (vs.length > 1) {
                    paramObj.put(p, vs);
                } else {
                    paramObj.put(p, request.getParameter(p));
                }
            }

            doIt(request, response, token, ApiType.OBJECT, paramObj, returnObj, "doservice");
        } catch (ResubmitException ex) {
            response.setStatus(524);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
        } catch (LicenseInvalidException | LicenseExpiredException ex) {
            response.setStatus(550);
            logger.error(ex.getMessage());
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
        } catch (ApiRuntimeException ex) {
            response.setStatus(520);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
            if (ex.getParam() != null) {
                returnObj.put("Param", ex.getParam());
            }
        } catch (NotFoundEditTargetException ex) {
            response.setStatus(526);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
        } catch (PermissionDeniedException ex) {
            response.setStatus(523);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", $.t(ex.getMessage(), ex.getValues()));
        } catch (Exception ex) {
            response.setStatus(500);
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ExceptionUtils.getStackTrace(ex));
            logger.error(ex.getMessage(), ex);
        }
        if (!response.isCommitted()) {
            response.setContentType(Config.RESPONSE_TYPE_JSON);
            if (returnObj.containsKey("_disableDetect")) {
                returnObj.remove("_disableDetect");
                response.getWriter().print(returnObj.toString(SerializerFeature.DisableCircularReferenceDetect));
            } else {
                response.getWriter().print(returnObj.toJSONString());
            }
        }

    }

    @RequestMapping(value = "/stream/**", method = RequestMethod.POST)
    public void dispatcherForPostStream(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String token = new AntPathMatcher().extractPathWithinPattern(pattern, request.getServletPath());
        JSONObject paramObj = new JSONObject();
        Enumeration<String> paraNames = request.getParameterNames();
        while (paraNames.hasMoreElements()) {
            String p = paraNames.nextElement();
            String[] vs = request.getParameterValues(p);
            if (vs.length > 1) {
                paramObj.put(p, vs);
            } else {
                paramObj.put(p, request.getParameter(p));
            }
        }
        JSONObject returnObj = new JSONObject();
        try {
            doIt(request, response, token, ApiType.STREAM, paramObj, returnObj, "doservice");
        } catch (ResubmitException ex) {
            response.setStatus(524);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
        } catch (LicenseInvalidException | LicenseExpiredException ex) {
            response.setStatus(550);
            logger.error(ex.getMessage());
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
        } catch (ApiRuntimeException ex) {
            response.setStatus(520);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
            if (ex.getParam() != null) {
                returnObj.put("Param", ex.getParam());
            }
        } catch (PermissionDeniedException ex) {
            response.setStatus(523);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(500);
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ExceptionUtils.getStackFrames(ex));
        }
        if (!response.isCommitted()) {
            response.setContentType(Config.RESPONSE_TYPE_JSON);
            if (returnObj.containsKey("_disableDetect")) {
                returnObj.remove("_disableDetect");
                response.getWriter().print(returnObj.toString(SerializerFeature.DisableCircularReferenceDetect));
            } else {
                response.getWriter().print(returnObj.toJSONString());
            }
        }
    }

    @RequestMapping(value = "/binary/**", method = RequestMethod.GET)
    public void dispatcherForPostBinary(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String token = new AntPathMatcher().extractPathWithinPattern(pattern, request.getServletPath());
        JSONObject paramObj = new JSONObject();

        Enumeration<String> paraNames = request.getParameterNames();
        while (paraNames.hasMoreElements()) {
            String p = paraNames.nextElement();
            String[] vs = request.getParameterValues(p);
            if (vs.length > 1) {
                paramObj.put(p, vs);
            } else {
                paramObj.put(p, request.getParameter(p));
            }
        }
        JSONObject returnObj = new JSONObject();
        try {
            doIt(request, response, token, ApiType.BINARY, paramObj, returnObj, "doservice");
        } catch (ResubmitException ex) {
            response.setStatus(524);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
        } catch (LicenseInvalidException | LicenseExpiredException ex) {
            response.setStatus(550);
            logger.error(ex.getMessage());
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
        } catch (ApiRuntimeException ex) {
            response.setStatus(520);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
            if (ex.getParam() != null) {
                returnObj.put("Param", ex.getParam());
            }
        } catch (PermissionDeniedException ex) {
            response.setStatus(523);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(500);
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ExceptionUtils.getStackFrames(ex));
        }
        if (!response.isCommitted()) {
            if (UserContext.get() != null) {
                HttpRequestUtil.resetResponse(response);
            }
            response.setContentType(Config.RESPONSE_TYPE_JSON);
            if (returnObj.containsKey("_disableDetect")) {
                returnObj.remove("_disableDetect");
                response.getWriter().print(returnObj.toString(SerializerFeature.DisableCircularReferenceDetect));
            } else {
                response.getWriter().print(returnObj.toJSONString());
            }
        }
    }

    @RequestMapping(value = "/binary/**", method = RequestMethod.POST, consumes = "application/json")
    public void dispatcherForPostBinaryJson(@RequestBody String jsonStr, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String token = new AntPathMatcher().extractPathWithinPattern(pattern, request.getServletPath());
        JSONObject paramObj;
        if (StringUtils.isNotBlank(jsonStr)) {
            try {
                paramObj = JSONObject.parseObject(jsonStr);
            } catch (Exception e) {
                throw new ParamJSONIrregularException();
            }
        } else {
            paramObj = new JSONObject();
        }

        Enumeration<String> paraNames = request.getParameterNames();
        while (paraNames.hasMoreElements()) {
            String p = paraNames.nextElement();
            String[] vs = request.getParameterValues(p);
            if (vs.length > 1) {
                paramObj.put(p, vs);
            } else {
                paramObj.put(p, request.getParameter(p));
            }
        }
        JSONObject returnObj = new JSONObject();
        try {
            doIt(request, response, token, ApiType.BINARY, paramObj, returnObj, "doservice");
        } catch (ResubmitException ex) {
            response.setStatus(524);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
        } catch (LicenseInvalidException | LicenseExpiredException ex) {
            response.setStatus(550);
            logger.error(ex.getMessage());
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
        } catch (ApiRuntimeException ex) {
            response.setStatus(520);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
            if (ex.getParam() != null) {
                returnObj.put("Param", ex.getParam());
            }
        } catch (PermissionDeniedException ex) {
            response.setStatus(523);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(500);
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ExceptionUtils.getStackFrames(ex));
        }
        if (!response.isCommitted()) {
            if (UserContext.get() != null) {
                HttpRequestUtil.resetResponse(response);
            }
            response.setContentType(Config.RESPONSE_TYPE_JSON);
            if (returnObj.containsKey("_disableDetect")) {
                returnObj.remove("_disableDetect");
                response.getWriter().print(returnObj.toString(SerializerFeature.DisableCircularReferenceDetect));
            } else {
                response.getWriter().print(returnObj.toJSONString());
            }
        }
    }

    @RequestMapping(value = "/binary/**", method = RequestMethod.POST, consumes = "multipart/form-data")
    public void dispatcherForPostBinaryMultipart(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String token = new AntPathMatcher().extractPathWithinPattern(pattern, request.getServletPath());
        JSONObject paramObj = new JSONObject();

        Enumeration<String> paraNames = request.getParameterNames();
        while (paraNames.hasMoreElements()) {
            String p = paraNames.nextElement();
            String[] vs = request.getParameterValues(p);
            if (vs.length > 1) {
                paramObj.put(p, vs);
            } else {
                paramObj.put(p, request.getParameter(p));
            }
        }
        JSONObject returnObj = new JSONObject();
        try {
            doIt(request, response, token, ApiType.BINARY, paramObj, returnObj, "doservice");
        } catch (ResubmitException ex) {
            response.setStatus(524);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
        } catch (LicenseInvalidException | LicenseExpiredException ex) {
            response.setStatus(550);
            logger.error(ex.getMessage());
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
        } catch (ApiRuntimeException ex) {
            response.setStatus(520);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
            if (ex.getParam() != null) {
                returnObj.put("Param", ex.getParam());
            }
        } catch (PermissionDeniedException ex) {
            response.setStatus(523);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(500);
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ExceptionUtils.getStackFrames(ex));
        }
        if (!response.isCommitted()) {
            response.setContentType(Config.RESPONSE_TYPE_JSON);
            response.getWriter().print(returnObj.toJSONString());
        }
    }

    @RequestMapping(value = "/help/rest/**", method = RequestMethod.GET)
    public void resthelp(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String token = new AntPathMatcher().extractPathWithinPattern(pattern, request.getServletPath());
        JSONObject returnObj = new JSONObject();
        try {
            doIt(request, response, token, ApiType.OBJECT, null, returnObj, "help");
        } catch (ApiRuntimeException ex) {
            response.setStatus(520);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
            if (ex.getParam() != null) {
                returnObj.put("Param", ex.getParam());
            }
        } catch (PermissionDeniedException ex) {
            response.setStatus(523);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(500);
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ExceptionUtils.getStackFrames(ex));
        }
        response.setContentType(Config.RESPONSE_TYPE_JSON);
        response.getWriter().print(returnObj.toJSONString());
    }

    @RequestMapping(value = "/help/stream/**", method = RequestMethod.GET)
    public void streamhelp(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String token = new AntPathMatcher().extractPathWithinPattern(pattern, request.getServletPath());
        JSONObject returnObj = new JSONObject();
        try {
            doIt(request, response, token, ApiType.STREAM, null, returnObj, "help");
        } catch (ApiRuntimeException ex) {
            response.setStatus(520);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
            if (ex.getParam() != null) {
                returnObj.put("Param", ex.getParam());
            }
        } catch (PermissionDeniedException ex) {
            response.setStatus(523);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(500);
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ExceptionUtils.getStackFrames(ex));
        }
        response.setContentType(Config.RESPONSE_TYPE_JSON);
        response.getWriter().print(returnObj.toJSONString());
    }

    @RequestMapping(value = "/help/binary/**", method = RequestMethod.GET)
    public void binaryhelp(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String token = new AntPathMatcher().extractPathWithinPattern(pattern, request.getServletPath());
        JSONObject returnObj = new JSONObject();
        try {
            doIt(request, response, token, ApiType.BINARY, null, returnObj, "help");
        } catch (ApiRuntimeException ex) {
            response.setStatus(520);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
            if (ex.getParam() != null) {
                returnObj.put("Param", ex.getParam());
            }
        } catch (PermissionDeniedException ex) {
            response.setStatus(523);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(500);
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ExceptionUtils.getStackFrames(ex));
        }
        response.setContentType(Config.RESPONSE_TYPE_JSON);
        response.getWriter().print(returnObj.toJSONString());
    }

}
