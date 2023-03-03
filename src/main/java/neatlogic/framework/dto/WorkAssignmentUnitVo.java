package neatlogic.framework.dto;

import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.GroupSearch;
import neatlogic.framework.restful.annotation.EntityField;

/**
 * @Title: WorkUnitVo
 * @Package neatlogic.framework.dto
 * @Description: 工作分配单元
 * @Author: linbq
 * @Date: 2021/1/14 18:37
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 **/
public class WorkAssignmentUnitVo {
    @EntityField(name = "类型", type = ApiParamType.STRING)
    private String initType;
    @EntityField(name = "uuid", type = ApiParamType.STRING)
    private String uuid;
    @EntityField(name = "名称", type = ApiParamType.STRING)
    private String name;
    @EntityField(name = "VIP级别(0,1,2,3,4,5)", type = ApiParamType.ENUM)
    private Integer vipLevel;
    @EntityField(name = "头像", type = ApiParamType.STRING)
    private String avatar;
    @EntityField(name = "拼音", type = ApiParamType.STRING)
    private String pinyin;

    public WorkAssignmentUnitVo(UserVo userVo){
        this.initType = GroupSearch.USER.getValue();
        this.uuid = userVo.getUuid();
        this.name = userVo.getUserName();
        this.vipLevel = userVo.getVipLevel();
        this.avatar = userVo.getAvatar();
        this.pinyin = userVo.getPinyin();
    }
    public WorkAssignmentUnitVo(TeamVo teamVo){
        this.initType = GroupSearch.TEAM.getValue();
        this.uuid = teamVo.getUuid();
        this.name = teamVo.getName();
    }
    public WorkAssignmentUnitVo(RoleVo roleVo){
        this.initType = GroupSearch.ROLE.getValue();
        this.uuid = roleVo.getUuid();
        this.name = roleVo.getName();
    }
    public String getInitType() {
        return initType;
    }

    public void setInitType(String initType) {
        this.initType = initType;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getVipLevel() {
        return vipLevel;
    }

    public void setVipLevel(Integer vipLevel) {
        this.vipLevel = vipLevel;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getPinyin() {
        return pinyin;
    }

    public void setPinyin(String pinyin) {
        this.pinyin = pinyin;
    }
}
