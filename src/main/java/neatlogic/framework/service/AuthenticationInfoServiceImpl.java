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

package neatlogic.framework.service;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.RequestContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.common.RootComponent;
import neatlogic.framework.dao.mapper.RoleMapper;
import neatlogic.framework.dao.mapper.TeamMapper;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.dto.RoleVo;
import neatlogic.framework.dto.TeamVo;
import neatlogic.framework.util.FreemarkerUtil;
import neatlogic.framework.util.RunScriptUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author linbq
 * @since 2021/8/2 20:22
 **/
@RootComponent
public class AuthenticationInfoServiceImpl implements AuthenticationInfoService {
    private TeamMapper teamMapper;
    private RoleMapper roleMapper;
    static Logger logger = LoggerFactory.getLogger(AuthenticationInfoServiceImpl.class);

    @Resource
    public void setTeamMapper(TeamMapper _teamMapper) {
        teamMapper = _teamMapper;
    }

    @Resource
    public void setRoleMapper(RoleMapper _roleMapper) {
        roleMapper = _roleMapper;
    }

    /**
     * 查询用户鉴权时，需要用到到userUuid、teamUuidList、roleUuidList，其中roleUuidList包含用户所在分组的拥护角色列表。
     *
     * @param userUuid 用户uuid
     */
    @Override
    public AuthenticationInfoVo getAuthenticationInfo(String userUuid) {
        return getAuthenticationInfo(userUuid, true);
    }

    @Override
    public AuthenticationInfoVo getAuthenticationInfo(String userUuid, Boolean isRuleRole) {
        List<String> teamUuidList = teamMapper.getTeamUuidListByUserUuid(userUuid);
        List<String> roleUuidList = roleMapper.getRoleUuidListByUserUuid(userUuid);
        Set<String> roleUuidSet = new HashSet<>(roleUuidList);
        getTeamUuidListAndRoleUuidList(teamUuidList, roleUuidSet);
        if (isRuleRole && CollectionUtils.isNotEmpty(roleUuidSet)) {
            roleUuidList = removeInValidRoleUuidList(new ArrayList<>(roleUuidSet));
        } else {
            roleUuidList = new ArrayList<>(roleUuidSet);
        }
        return new AuthenticationInfoVo(userUuid, teamUuidList, roleUuidList);
    }

    /**
     * 补充teamUuidList roleUuidSet
     *
     * @param teamUuidList 组uuid列表
     * @param roleUuidSet  角色列表
     */
    private void getTeamUuidListAndRoleUuidList(List<String> teamUuidList, Set<String> roleUuidSet) {
        if (CollectionUtils.isNotEmpty(teamUuidList)) {
            List<String> teamRoleUuidList = roleMapper.getRoleUuidListByTeamUuidList(teamUuidList);
            roleUuidSet.addAll(teamRoleUuidList);
            Set<String> upwardUuidSet = new HashSet<>();
            List<TeamVo> teamList = teamMapper.getTeamByUuidList(teamUuidList);
            for (TeamVo teamVo : teamList) {
                String upwardUuidPath = teamVo.getUpwardUuidPath();
                if (StringUtils.isNotBlank(upwardUuidPath)) {
                    String[] upwardUuidArray = upwardUuidPath.split(",");
                    for (String upwardUuid : upwardUuidArray) {
                        if (!upwardUuid.equals(teamVo.getUuid())) {
                            upwardUuidSet.add(upwardUuid);
                        }
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(upwardUuidSet)) {
                teamRoleUuidList = roleMapper.getRoleUuidListByTeamUuidListAndCheckedChildren(new ArrayList<>(upwardUuidSet), 1);
                roleUuidSet.addAll(teamRoleUuidList);
            }
        }
    }

    /**
     * 查询用户鉴权时，需要用到到userUuidList、teamUuidList、roleUuidList，其中roleUuidList包含用户所在分组的拥护角色列表。
     *
     * @param userUuidList 用户uuid列表
     */
    @Override
    public AuthenticationInfoVo getAuthenticationInfo(List<String> userUuidList) {
        Set<String> teamUuidSet = new HashSet<>();
        Set<String> roleUuidSet = new HashSet<>();
        for (String userUuid : userUuidList) {
            List<String> userTeamUuidList = teamMapper.getTeamUuidListByUserUuid(userUuid);
            teamUuidSet.addAll(userTeamUuidList);
            List<String> userRoleUuidList = roleMapper.getRoleUuidListByUserUuid(userUuid);
            roleUuidSet.addAll(userRoleUuidList);
            getTeamUuidListAndRoleUuidList(userTeamUuidList, roleUuidSet);
        }
        List<String> roleUuidList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(roleUuidSet)) {
            roleUuidList = removeInValidRoleUuidList(new ArrayList<>(roleUuidSet));
        }
        return new AuthenticationInfoVo(userUuidList, new ArrayList<>(teamUuidSet), roleUuidList);
    }

    /**
     * 去掉不满足规则的角色
     * @param roleUuidList 角色
     */
    private List<String> removeInValidRoleUuidList(List<String> roleUuidList) {
        JSONObject headers = new JSONObject();
        List<String> validRoleUuidList = new ArrayList<>();
        if (UserContext.get() != null) {
            headers = UserContext.get().getJwtVo().getHeaders();
        } else {
            if (RequestContext.get() != null && RequestContext.get().getRequest() != null) {
                Enumeration<String> envNames = RequestContext.get().getRequest().getHeaderNames();
                while (envNames != null && envNames.hasMoreElements()) {
                    String key = envNames.nextElement();
                    String value = RequestContext.get().getRequest().getHeader(key);
                    headers.put(key, value);
                }
            }
        }
        List<RoleVo> roleVos = roleMapper.getRoleByUuidList(roleUuidList);
        for (RoleVo ro : roleVos) {
            String rule = ro.getRule();
            if (StringUtils.isNotBlank(rule)) {
                try {
                    rule = FreemarkerUtil.transform(headers, rule);
                    if (RunScriptUtil.runScript(rule)) {
                        validRoleUuidList.add(ro.getUuid());
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            } else {
                validRoleUuidList.add(ro.getUuid());
            }
        }
        return validRoleUuidList;
    }

}
