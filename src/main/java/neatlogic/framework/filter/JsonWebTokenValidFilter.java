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

package neatlogic.framework.filter;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.RequestContext;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.common.config.Config;
import neatlogic.framework.common.util.TenantUtil;
import neatlogic.framework.dao.cache.UserSessionCache;
import neatlogic.framework.dao.mapper.UserSessionMapper;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.dto.JwtVo;
import neatlogic.framework.dto.UserSessionVo;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.filter.core.ILoginAuthHandler;
import neatlogic.framework.filter.core.LoginAuthFactory;
import neatlogic.framework.login.core.ILoginPostProcessor;
import neatlogic.framework.login.core.LoginPostProcessorFactory;
import neatlogic.framework.util.$;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Date;

public class JsonWebTokenValidFilter extends OncePerRequestFilter {
    @Resource
    private UserSessionMapper userSessionMapper;

    /**
     * Default constructor.
     */
    public JsonWebTokenValidFilter() {
    }

    /**
     * @see Filter#destroy()
     */
    @Override
    public void destroy() {
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException {
        Cookie[] cookies = request.getCookies();
        String timezone = "+8:00";
        boolean isUnExpired = false;
        UserVo userVo = null;
        String authType = "default";
        ILoginAuthHandler defaultLoginAuth = LoginAuthFactory.getLoginAuth(authType);
        ILoginAuthHandler loginAuth = defaultLoginAuth;
        //获取时区
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("neatlogic_timezone".equals(cookie.getName())) {
                    timezone = (URLDecoder.decode(cookie.getValue(), "UTF-8"));
                }
            }
        }
        //初始化request上下文
        RequestContext.init(request, request.getRequestURI(), response);

        //判断租户
        try {
            String tenant = request.getHeader("Tenant");
            //认证过程中可能需要从request中获取inputStream，为了后续spring也可以获取inputStream，需要做一层cached
            HttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
            if (!TenantUtil.hasTenant(tenant)) {
                returnErrorResponseJson($.t("nff.jsonwebtokenvalidfilter.dofilterinternal.lacktenant", tenant), 521, response, loginAuth.directUrl());
                return;
            }
            TenantContext.init();
            TenantContext.get().switchTenant(tenant);
            logger.warn("======= defaultLoginAuth: ");
            //先按 default 认证，不存在才根据具体 AuthType 认证用户
            userVo = defaultLoginAuth.auth(cachedRequest, response);
            if (userVo == null) {
                //获取认证插件名,优先使用请求方指定的认证
                String authTypeHeader = request.getHeader("AuthType");
                if (StringUtils.isNotBlank(authTypeHeader)) {
                    authType = authTypeHeader;
                } else {
                    authType = Config.LOGIN_AUTH_TYPE();
                }
                logger.info("AuthType: " + authType);
                if (StringUtils.isNotBlank(authType)) {
                    loginAuth = LoginAuthFactory.getLoginAuth(authType);
                    if (loginAuth != null) {
                        userVo = loginAuth.auth(cachedRequest, response);
                        if (userVo != null && StringUtils.isNotBlank(userVo.getUuid())) {
                            logger.warn("======= getUser succeed: " + userVo.getUuid());
                            isUnExpired = userExpirationValid(userVo, timezone, request, response);
                            if (isUnExpired) {
                                for (ILoginPostProcessor loginPostProcessor : LoginPostProcessorFactory.getLoginPostProcessorSet()) {
                                    loginPostProcessor.loginAfterInitialization();
                                }
                            }
                        } else {
                            returnErrorResponseJson($.t("nff.jsonwebtokenvalidfilter.dofilterinternal.authfailed", loginAuth.getType()), 523, response, loginAuth.directUrl());
                            return;
                        }
                    } else {
                        returnErrorResponseJson($.t("nff.jsonwebtokenvalidfilter.dofilterinternal.authtypeinvalid", authType), 522, response, defaultLoginAuth.directUrl());
                        return;
                    }
                } else {
                    returnErrorResponseJson($.t("nff.jsonwebtokenvalidfilter.dofilterinternal.authfailed", loginAuth.getType()), 523, response, defaultLoginAuth.directUrl());
                    return;
                }
            } else {
                logger.warn("======= getUser succeed: " + userVo.getUuid());
                isUnExpired = userExpirationValid(userVo, timezone, request, response);
            }

            //回话超时
            if (!isUnExpired) {
                returnErrorResponseJson($.t("nff.jsonwebtokenvalidfilter.dofilterinternal.unexpired"), 527, response, loginAuth.directUrl());
                return;
            } else {
                logger.warn("======= is Expired");
            }

            try {
                filterChain.doFilter(cachedRequest, response);
            } catch (Exception ex) {
                //兼容“处理response,对象toString可能会异常”的场景，过了filter，应该是520异常
                logger.error(ex.getMessage(), ex);
                returnErrorResponseJson(ex.getMessage(), 520, response, false);
            }
        } catch (Exception ex) {
            logger.error($.t("nff.jsonwebtokenvalidfilter.dofilterinternal.authfailederrorlog"), ex);
            returnErrorResponseJson(ex.getMessage(), 524, response, loginAuth != null ? loginAuth.directUrl() : defaultLoginAuth.directUrl());
        }
    }

    /**
     * 返回异常JSON
     *
     * @param message      异常信息
     * @param responseCode 异常码
     * @param response     相应
     * @param directUrl    前端跳转url
     * @throws IOException 异常
     */
    private void returnErrorResponseJson(String message, Integer responseCode, HttpServletResponse response, boolean isRemoveCookie, String directUrl) throws IOException {
        JSONObject redirectObj = new JSONObject();
        redirectObj.put("Status", "FAILED");
        redirectObj.put("Message", message);
        logger.warn("======login error:" + message);
        response.setStatus(responseCode);
        redirectObj.put("DirectUrl", directUrl);
        if (isRemoveCookie) {
            removeAuthCookie(response);
        }
        response.setContentType(Config.RESPONSE_TYPE_JSON);
        response.getWriter().print(redirectObj.toJSONString());
    }

    /**
     * 返回异常JSON
     *
     * @param message      异常信息
     * @param responseCode 异常码
     * @param response     相应
     * @param directUrl    前端跳转url
     * @throws IOException 异常
     */
    private void returnErrorResponseJson(String message, Integer responseCode, HttpServletResponse response, String directUrl) throws IOException {
        returnErrorResponseJson(message, responseCode, response, true, directUrl);
    }

    /**
     * 返回异常JSON
     *
     * @param message      异常信息
     * @param responseCode 异常码
     * @param response     相应
     * @throws IOException 异常
     */
    private void returnErrorResponseJson(String message, Integer responseCode, HttpServletResponse response, boolean isRemoveCookie) throws IOException {
        returnErrorResponseJson(message, responseCode, response, isRemoveCookie, null);
    }

    /**
     * 登录异常后端清除neatlogic_authorization cookie，防止sso循环跳转
     */
    private void removeAuthCookie(HttpServletResponse response) {
        if (TenantContext.get() != null) {
            Cookie authCookie = new Cookie("neatlogic_authorization", null);
            authCookie.setPath("/" + TenantContext.get().getTenantUuid());
            authCookie.setMaxAge(0);//表示删除
            response.addCookie(authCookie);
        }
    }

    /**
     * 校验用户登录超时
     *
     * @return 不超时返回权限信息，否则返回null
     */
    private boolean userExpirationValid(UserVo userVo, String timezone, HttpServletRequest request, HttpServletResponse response) {
        JwtVo jwt = userVo.getJwtVo();
        AuthenticationInfoVo authenticationInfo = (AuthenticationInfoVo) UserSessionCache.getItem(jwt.getTokenHash());
        if (authenticationInfo == null || authenticationInfo.getUserUuid() == null) {
            UserSessionVo userSessionVo = userSessionMapper.getUserSessionByTokenHash(jwt.getTokenHash());
            if (null != userSessionVo && (jwt.validTokenCreateTime(userSessionVo.getTokenCreateTime()))) {
                Date visitTime = userSessionVo.getSessionTime();
                Date now = new Date();
                int expire = Config.USER_EXPIRETIME();
                long expireTime = expire * 60L * 1000L + visitTime.getTime();
                if (now.getTime() < expireTime) {
                    userSessionMapper.updateUserSession(jwt.getTokenHash());
                    authenticationInfo = userSessionVo.getAuthInfo();
                    UserSessionCache.addItem(jwt.getTokenHash(), authenticationInfo);
                    UserContext.init(userVo, authenticationInfo, timezone, request, response);
                    return true;
                }
                userSessionMapper.deleteUserSessionByTokenHash(jwt.getTokenHash());
            }
        } else {
            UserContext.init(userVo, authenticationInfo, timezone, request, response);
            return true;
        }
        return false;
    }


}
