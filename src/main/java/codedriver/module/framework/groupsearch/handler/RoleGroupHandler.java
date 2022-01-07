/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.framework.groupsearch.handler;

import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.dao.mapper.RoleMapper;
import codedriver.framework.dto.RoleVo;
import codedriver.framework.restful.groupsearch.core.IGroupSearchHandler;
import codedriver.framework.service.RoleService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
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
    public String getHeader() {
        return getName() + "#";
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> search(JSONObject jsonObj) {
        //总显示选项个数
        Integer total = jsonObj.getInteger("total");
        if (total == null) {
            total = 18;
        }
        List<RoleVo> roleList = new ArrayList<RoleVo>();
        RoleVo roleVo = new RoleVo();
        roleVo.setNeedPage(true);
        roleVo.setPageSize(total);
        roleVo.setCurrentPage(1);
        roleVo.setKeyword(jsonObj.getString("keyword"));
        //如果存在rangeList 则需要过滤option
        List<Object> rangeList = jsonObj.getJSONArray("rangeList");
        if (CollectionUtils.isNotEmpty(rangeList)) {
            List<String> roleUuidList = new ArrayList<>();
            rangeList.forEach(r -> {
                if (r.toString().startsWith(GroupSearch.ROLE.getValuePlugin())) {
                    roleUuidList.add(GroupSearch.removePrefix(r.toString()));
                }
            });
            if (CollectionUtils.isEmpty(roleUuidList)) {//如果rangList不为空 但roleUuidList为空则 无需返回角色
                roleUuidList.add("role#no_role");
            }
            roleVo.setRoleUuidList(roleUuidList);
        }
        roleList = roleMapper.searchRole(roleVo);
        roleService.getRoleTeamAndRoleUser(roleList);
        return (List<T>) roleList;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> reload(JSONObject jsonObj) {
        List<RoleVo> roleList = new ArrayList<RoleVo>();
        List<String> roleUuidList = new ArrayList<String>();
        for (Object value : jsonObj.getJSONArray("valueList")) {
            if (value.toString().startsWith(getHeader())) {
                roleUuidList.add(value.toString().replace(getHeader(), ""));
            }
        }
        if (roleUuidList.size() > 0) {
            roleList = roleMapper.getRoleByUuidList(roleUuidList);
        }
        return (List<T>) roleList;
    }

    @Override
    public <T> JSONObject repack(List<T> roleList) {
        JSONObject roleObj = new JSONObject();
        roleObj.put("value", "role");
        roleObj.put("text", "角色");
        JSONArray roleArray = new JSONArray();
        for (T role : roleList) {
            JSONObject roleTmp = new JSONObject();
            roleTmp.put("value", getHeader() + ((RoleVo) role).getUuid());
            roleTmp.put("text", ((RoleVo) role).getName());
            roleArray.add(roleTmp);
        }
        roleObj.put("sort", getSort());
        roleObj.put("dataList", roleArray);
        return roleObj;
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
