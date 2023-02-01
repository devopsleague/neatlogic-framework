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

package neatlogic.module.framework.filter.handler;

import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.exception.hmac.HeaderIrregularException;
import neatlogic.framework.exception.hmac.HeaderNotFoundException;
import neatlogic.framework.exception.user.UserNotFoundException;
import neatlogic.framework.exception.user.UserTokenNotFoundException;
import neatlogic.framework.filter.core.LoginAuthHandlerBase;
import neatlogic.framework.util.SHA256Util;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.Base64;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Service
public class HmacLoginAuthHandler extends LoginAuthHandlerBase {

    @Resource
    private UserMapper userMapper;

    @Override
    public String getType() {
        return "hmac";
    }

    /*
    请求超时时间，默认5分钟
     */
    //private final int expiretime = 60;

    /**
     * 头部说明：
     * x-access-key="用户名"
     * Authorization="Hmac 签名后的sha256散列值"
     */
    @Override
    public UserVo myAuth(HttpServletRequest request) throws IOException {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.isBlank(authorization)) {
            throw new HeaderNotFoundException("Authorization");
        }
        if (!authorization.startsWith("Hmac ")) {
            throw new HeaderIrregularException("Authorization");
        }
        authorization = authorization.substring(5);
        if (StringUtils.isBlank(authorization)) {
            throw new HeaderIrregularException("Authorization");
        }
        String user = request.getHeader("x-access-key");
        if (StringUtils.isBlank(user)) {
            throw new HeaderNotFoundException("x-access-key");
        }

        UserVo userVo = userMapper.getUserByUserId(user);

        if (userVo == null) {
            throw new UserNotFoundException(user);
        }
        String token = userMapper.getUserTokenByUserId(user);
        if (StringUtils.isBlank(token)) {
            throw new UserTokenNotFoundException(user);
        }


        InputStream input = request.getInputStream();
        //System.out.println(request.getContentType());
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isBlank(request.getContentType()) || (request.getContentType() != null && !request.getContentType().toLowerCase().startsWith("multipart/form-data"))) {
            //如果是multipart，则跳过对body内容的抽取，避免文件内容过大时导致内存溢出
            BufferedReader reader;
            if (input != null) {
                reader = new BufferedReader(new InputStreamReader(input));
                char[] charBuffer = new char[2048];
                int bytesRead = -1;
                while ((bytesRead = reader.read(charBuffer)) > 0) {
                    sb.append(charBuffer, 0, bytesRead);
                }
            }
        }

        String queryString = StringUtils.isNotBlank(request.getQueryString()) ? "?" + request.getQueryString() : StringUtils.EMPTY;
        String sign = user + "#" + request.getRequestURI() + queryString + "#" + Base64.encodeBase64StringUnChunked(sb.toString().getBytes(StandardCharsets.UTF_8));
        String result = SHA256Util.encrypt(token, sign);
        if (result.equalsIgnoreCase(authorization)) {
            return userVo;
        }
        return null;
    }


    @Override
    public String directUrl() {
        return null;
    }


}
