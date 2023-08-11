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

package neatlogic.module.framework.groupsearch.handler;

import neatlogic.framework.common.constvalue.GroupSearch;
import neatlogic.framework.dao.mapper.RoleMapper;
import neatlogic.framework.dto.RoleVo;
import neatlogic.framework.restful.groupsearch.core.GroupSearchOptionVo;
import neatlogic.framework.restful.groupsearch.core.GroupSearchVo;
import neatlogic.framework.restful.groupsearch.core.IGroupSearchHandler;
import neatlogic.framework.service.RoleService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class RoleGroupHandler implements IGroupSearchHandler {
    @Autowired
    private RoleMapper roleMapper;

    @Resource
    private RoleService roleService;

    @Override
    public String getName() {
        return GroupSearch.ROLE.getValue();
    }

    @Override
    public String getLabel() {
        return GroupSearch.ROLE.getText();
    }

    @Override
    public String getHeader() {
        return getName() + "#";
    }


    @Override
    public List<GroupSearchOptionVo> search(GroupSearchVo groupSearchVo) {
        //总显示选项个数
        Integer total = groupSearchVo.getTotal();
        if (total == null) {
            total = 18;
        }
        List<RoleVo> roleList;
        RoleVo roleVo = new RoleVo();
        roleVo.setNeedPage(true);
        roleVo.setPageSize(total);
        roleVo.setCurrentPage(1);
        roleVo.setKeyword(groupSearchVo.getKeyword());
        //如果存在rangeList 则需要过滤option
        List<String> rangeList = groupSearchVo.getRangeList();
        if (CollectionUtils.isNotEmpty(rangeList)) {
            List<String> roleUuidList = new ArrayList<>();
            rangeList.forEach(r -> {
                if (r.startsWith(GroupSearch.ROLE.getValuePlugin())) {
                    roleUuidList.add(GroupSearch.removePrefix(r));
                }
            });
            if (CollectionUtils.isEmpty(roleUuidList)) {//如果rangList不为空 但roleUuidList为空则 无需返回角色
                roleUuidList.add("role#no_role");
            }
            roleVo.setRoleUuidList(roleUuidList);
        }
        roleList = roleMapper.searchRole(roleVo);
        roleService.setRoleTeamCountAndRoleUserCount(roleList);
        return convertGroupSearchOption(roleList);
    }

    @Override
    public List<GroupSearchOptionVo> reload(GroupSearchVo groupSearchVo) {
        List<RoleVo> roleList = new ArrayList<RoleVo>();
        List<String> roleUuidList = new ArrayList<String>();
        for (String value : groupSearchVo.getValueList()) {
            if (value.startsWith(getHeader())) {
                roleUuidList.add(value.replace(getHeader(), StringUtils.EMPTY));
            }
        }
        if (!roleUuidList.isEmpty()) {
            roleList = roleMapper.getRoleByUuidList(roleUuidList);
        }
        return convertGroupSearchOption(roleList);
    }

    private List<GroupSearchOptionVo> convertGroupSearchOption(List<RoleVo> roleList) {
        List<GroupSearchOptionVo> dataList = new ArrayList<>();
        for (RoleVo role : roleList) {
            GroupSearchOptionVo groupSearchOptionVo = new GroupSearchOptionVo();
            groupSearchOptionVo.setValue(getHeader() + role.getUuid());
            groupSearchOptionVo.setText(role.getName());
            dataList.add(groupSearchOptionVo);
        }
        return dataList;
    }

    @Override
    public int getSort() {
        return 4;
    }

    @Override
    public Boolean isLimit() {
        return true;
    }
}
