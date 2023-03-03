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

package neatlogic.framework.scheduler.dto;

import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.EntityField;
import neatlogic.framework.scheduler.core.SchedulerManager;
import com.alibaba.fastjson.annotation.JSONField;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class JobVo extends BasePageVo {

    @EntityField(name = "定时作业名称",
            type = ApiParamType.STRING)
    private String name;
    @EntityField(name = "状态(0:禁用，1：启用)",
            type = ApiParamType.INTEGER)
    private Integer isActive;

    @EntityField(name = "定时作业uuid",
            type = ApiParamType.STRING)
    private String uuid;
    @EntityField(name = "定时作业组件类路径",
            type = ApiParamType.STRING)
    private String handler;
    @EntityField(name = "定时作业组件名称",
            type = ApiParamType.STRING)
    private String handlerName;
    @EntityField(name = "是否保存执行记录(0:不保存，1:保存)",
            type = ApiParamType.INTEGER)
    private Integer needAudit;
    @EntityField(name = "cron表达式",
            type = ApiParamType.STRING)
    private String cron;
    @EntityField(name = "开始时间",
            type = ApiParamType.LONG)
    private Date beginTime;
    @EntityField(name = "结束时间",
            type = ApiParamType.LONG)
    private Date endTime;

    private JobStatusVo jobStatus;

    //	@EntityField(name = "定时作业属性列表",
//			type = ApiParamType.JSONARRAY)
    private List<JobPropVo> propList;

    public JobVo() {
        this.setPageSize(20);
    }

    public List<JobPropVo> getPropList() {
        if (propList == null || propList.size() == 0) {
            return null;
        }
        return propList;
    }

    public void setPropList(List<JobPropVo> propList) {
        this.propList = propList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }


    public String getUuid() {
        if (StringUtils.isBlank(uuid)) {
            uuid = UUID.randomUUID().toString().replace("-", "");
        }
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Integer getNeedAudit() {
        return needAudit;
    }

    public void setNeedAudit(Integer needAudit) {
        this.needAudit = needAudit;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public Date getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(Date beginTime) {
        this.beginTime = beginTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public String getHandlerName() {
        if (handlerName != null) {
            return handlerName;
        }
        JobClassVo jobClassVo = SchedulerManager.getJobClassByClassName(handler);
        if (jobClassVo == null) {
            return null;
        }
        handlerName = jobClassVo.getName();
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    public JobStatusVo getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(JobStatusVo jobStatus) {
        this.jobStatus = jobStatus;
    }

}
