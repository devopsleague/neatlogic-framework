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

package neatlogic.framework.asynchronization.threadlocal;

import neatlogic.framework.common.RootConfiguration;
import neatlogic.framework.common.util.ModuleUtil;
import neatlogic.framework.dao.mapper.ModuleMapper;
import neatlogic.framework.dto.module.ModuleGroupVo;
import neatlogic.framework.dto.module.ModuleVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RootConfiguration
public class TenantContext implements Serializable {
    private static final long serialVersionUID = -5977938340288247600L;
    private static final ThreadLocal<TenantContext> instance = new ThreadLocal<>();
    private String tenantUuid;
    private Boolean useDefaultDatasource = false;
    private List<ModuleVo> activeModuleList;
    private List<ModuleGroupVo> activeModuleGroupList;
    private Map<String, ModuleVo> activeModuleMap;
    private Boolean isData = false;
    private final String dataDbName = "";

    private static ModuleMapper moduleMapper;

    @Autowired
    public void setModuleMapper(ModuleMapper _moduleMapper) {
        moduleMapper = _moduleMapper;
    }

    public static TenantContext init() {
        TenantContext context = new TenantContext();
        instance.set(context);
        return context;
    }

    public static TenantContext init(TenantContext _tenantContext) {
        TenantContext context = new TenantContext();
        if (_tenantContext != null) {
            context.setTenantUuid(_tenantContext.getTenantUuid());
            context.setActiveModuleList(_tenantContext.getActiveModuleList());
        }
        instance.set(context);
        return context;
    }

    public static TenantContext init(String _tenantUuid) {
        TenantContext context = new TenantContext(_tenantUuid);
        instance.set(context);
        return context;
    }

    private TenantContext() {

    }

    private TenantContext(String _tenantUuid) {
        this.tenantUuid = _tenantUuid;
    }

    public String getTenantUuid() {
        if (useDefaultDatasource) {
            return null;
        } else {
            return tenantUuid;
        }
    }

    public String getDataDbName() {
        return "neatlogic_" + tenantUuid + "_data";
    }

    public String getDbName() {
        return "neatlogic_" + tenantUuid;
    }

    private TenantContext setTenantUuid(String tenantUuid) {
        this.tenantUuid = tenantUuid;
        return this;
    }

    public void switchDataDatabase() {
        this.isData = true;
    }

    public void switchDefaultDatabase() {
        this.isData = false;
    }

    public Boolean isData() {
        return this.isData;
    }

    public TenantContext switchTenant(String tenantUuid) {
        if (StringUtils.isNotBlank(tenantUuid)) {
            this.tenantUuid = tenantUuid;
            // 使用master库
            this.setUseDefaultDatasource(true);
            //防止 ArrayList HashMap 对象在存入 ehcache 之前迭代序列化时，另一个线程对这个 list、map 进行了修改操作
            List<String> tenantModuleGroupList = new ArrayList<>(moduleMapper.getModuleGroupListByTenantUuid(tenantUuid));
            this.activeModuleList = ModuleUtil.getTenantActiveModuleList(tenantModuleGroupList);
            this.activeModuleGroupList = new ArrayList<>();
            for (String group : tenantModuleGroupList) {
                ModuleGroupVo groupVo = ModuleUtil.getModuleGroup(group);
                if (groupVo != null) {
                    this.activeModuleGroupList.add(groupVo);
                }
            }
            // 还原回租户库
            this.setUseDefaultDatasource(false);
            activeModuleMap = new HashMap<>();
            if (activeModuleList != null && activeModuleList.size() > 0) {
                for (ModuleVo module : activeModuleList) {
                    activeModuleMap.put(module.getId(), module);
                }
            }
        }
        return this;
    }

    public static TenantContext get() {
        return instance.get();
    }

    public void release() {
        instance.remove();
    }

    public Boolean getUseDefaultDatasource() {
        return useDefaultDatasource;
    }

    /**
     * 切换数据库
     * 注意：不能在事务场景使用改方法，否则会切库失败
     * @param useDefaultDatasource true 使用neatlogic 库 ，false 还原使用租户库
     */
    public void setUseDefaultDatasource(Boolean useDefaultDatasource) {
        this.useDefaultDatasource = useDefaultDatasource;
    }

    public List<ModuleVo> getActiveModuleList() {
        return activeModuleList;
    }

    public List<ModuleGroupVo> getActiveModuleGroupList() {
        return activeModuleGroupList;
    }

    public void setActiveModuleList(List<ModuleVo> activeModuleList) {
        this.activeModuleList = activeModuleList;
        activeModuleMap = new HashMap<>();
        if (activeModuleList != null && activeModuleList.size() > 0) {
            for (ModuleVo module : activeModuleList) {
                activeModuleMap.put(module.getId(), module);
            }
        }
    }

    public Map<String, ModuleVo> getActiveModuleMap() {
        return activeModuleMap;
    }

    public boolean containsModule(String moduleId) {
        return activeModuleMap.containsKey(moduleId);
    }
}
