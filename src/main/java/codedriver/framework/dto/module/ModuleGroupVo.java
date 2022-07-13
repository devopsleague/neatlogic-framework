/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.framework.dto.module;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class ModuleGroupVo {

    @EntityField(name = "模块分组", type = ApiParamType.STRING)
    private String group;
    @EntityField(name = "模块分组名称", type = ApiParamType.STRING)
    private String groupName;
    @EntityField(name = "模块分组排序", type = ApiParamType.INTEGER)
    private Integer groupSort;
    @EntityField(name = "模块分组描述", type = ApiParamType.STRING)
    private String groupDescription;
    @EntityField(name = "模块列表", type = ApiParamType.JSONOBJECT)
    private List<ModuleVo> moduleList;

    @EntityField(name = "模块版本", type = ApiParamType.STRING)
    private String version;

    public ModuleGroupVo() {

    }

    public ModuleGroupVo(String _group) {
        group = _group;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<ModuleVo> getModuleList() {
        return moduleList;
    }

    public void setModuleList(List<ModuleVo> moduleList) {
        this.moduleList = moduleList;
    }

    public String getGroupDescription() {
        return groupDescription;
    }

    public void setGroupDescription(String groupDescription) {
        this.groupDescription = groupDescription;
    }

    public Integer getGroupSort() {
        return groupSort;
    }

    public void setGroupSort(Integer groupSort) {
        this.groupSort = groupSort;
    }

    public List<String> getModuleIdList() {
        List<String> moduleIdList = new ArrayList<String>();
        if (CollectionUtils.isNotEmpty(this.moduleList)) {
            for (ModuleVo moduleVo : this.moduleList) {
                moduleIdList.add(moduleVo.getId());
            }
        }
        return moduleIdList;
    }

}