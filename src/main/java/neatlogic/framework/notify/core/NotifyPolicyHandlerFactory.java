/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.framework.notify.core;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import neatlogic.framework.applicationlistener.core.ModuleInitializedListenerBase;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.bootstrap.NeatLogicWebApplicationContext;
import neatlogic.framework.common.RootComponent;
import neatlogic.framework.common.util.ModuleUtil;
import neatlogic.framework.dto.module.ModuleVo;
import neatlogic.framework.notify.dto.NotifyTreeVo;
import neatlogic.framework.notify.dto.NotifyTriggerVo;
import neatlogic.framework.util.$;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@RootComponent
public class NotifyPolicyHandlerFactory extends ModuleInitializedListenerBase {

    private static final Map<String, INotifyPolicyHandler> notifyPolicyHandlerMap = new HashMap<>();

    private static final List<NotifyTreeVo> moduleTreeVoList = new ArrayList<>();

    private static final Map<String, NotifyTreeVo> moduleTreeVoMap = new HashMap<>();

//    private static final List<NotifyTreeVo> notifyPolicyTreeVoList = new ArrayList<>();

//    private static final Map<String, NotifyTreeVo> notifyPolicyGroupTreeVoMap = new HashMap<>();

    private static final Map<String, String> handler2ModuleGroupIdMap = new HashMap<>();

    private static final Map<String, String> handler2ModuleIdMap = new HashMap<>();

    public static INotifyPolicyHandler getHandler(String handler) {
        INotifyPolicyHandler notifyPolicyHandler = notifyPolicyHandlerMap.get(handler);
        if (notifyPolicyHandler == null) {
            int index = handler.lastIndexOf(".");
            notifyPolicyHandler = notifyPolicyHandlerMap.get(handler.substring(index + 1));
        }
        return notifyPolicyHandler;
    }

    public static List<INotifyPolicyHandler> getHandlerList() {
        List<INotifyPolicyHandler> resultList = new ArrayList<>();
        for (Map.Entry<String, INotifyPolicyHandler> entry : notifyPolicyHandlerMap.entrySet()) {
            INotifyPolicyHandler notifyPolicyHandler = entry.getValue();
            if (resultList.contains(notifyPolicyHandler)) {
                continue;
            }
            resultList.add(notifyPolicyHandler);
        }
        return resultList;
    }

    public static String getModuleIdByHandler(String handler) {
        return handler2ModuleIdMap.get(handler);
    }

    public static String getModuleGroupIdByHandler(String handler) {
        return handler2ModuleGroupIdMap.get(handler);
    }

    public static List<NotifyTreeVo> getModuleTreeVoList() {
        if (CollectionUtils.isNotEmpty(moduleTreeVoList)) {
            List<NotifyTreeVo> moduleTreeListTmp = JSONArray.parseArray(JSON.toJSONString(moduleTreeVoList), NotifyTreeVo.class);
            for (NotifyTreeVo notifyTreeVo : moduleTreeListTmp) {
                notifyTreeVo.setName($.t(notifyTreeVo.getName()));
                if (CollectionUtils.isNotEmpty(notifyTreeVo.getChildren())) {
                    for (NotifyTreeVo childNotifyTreeVo : notifyTreeVo.getChildren()) {
                        childNotifyTreeVo.setName($.t(childNotifyTreeVo.getName()));
                        if (CollectionUtils.isNotEmpty(childNotifyTreeVo.getChildren())) {
                            for (NotifyTreeVo secondChildNotifyTreeVo : childNotifyTreeVo.getChildren()) {
                                secondChildNotifyTreeVo.setName($.t(secondChildNotifyTreeVo.getName()));
                            }
                        }
                    }
                }
            }
            return moduleTreeListTmp;
        }
        return moduleTreeVoList;
    }

    public static List<String> getTriggerList(String type) {
        NotifyTreeVo targetNode = getTargetNode(moduleTreeVoList, type);
        return getTriggerList(targetNode);
    }

    public static List<String> getAllActiveTriggerList() {
        List<ModuleVo> activeModuleList = TenantContext.get().getActiveModuleList();
        List<String> triggerList = new ArrayList<>();
        for (ModuleVo moduleVo : activeModuleList) {
            NotifyTreeVo targetNode = getTargetNode(moduleTreeVoList, moduleVo.getGroup());
            triggerList.addAll(getTriggerList(targetNode));
        }
        return triggerList;
    }

    /**
     * @Description: 遍历消息分类树找到用户选中的节点
     * @Author: linbq
     * @Date: 2021/2/23 10:19
     * @Params: [treeVoList, type]
     * @Returns: neatlogic.framework.notify.dto.NotifyTreeVo
     **/
    private static NotifyTreeVo getTargetNode(List<NotifyTreeVo> treeVoList, String type) {
        if (CollectionUtils.isNotEmpty(treeVoList)) {
            for (NotifyTreeVo notifyTreeVo : treeVoList) {
                if (notifyTreeVo.getUuid().equals(type)) {
                    return notifyTreeVo;
                } else {
                    NotifyTreeVo targetNode = getTargetNode(notifyTreeVo.getChildren(), type);
                    if (targetNode != null) {
                        return targetNode;
                    }
                }
            }
        }
        return null;
    }

    /**
     * @Description: 遍历选择节点的所有子节点，找到所有触发点集合
     * @Author: linbq
     * @Date: 2021/2/23 10:18
     * @Params: [notifyTreeVo]
     * @Returns: java.util.List<java.lang.String>
     **/
    private static List<String> getTriggerList(NotifyTreeVo notifyTreeVo) {
        List<String> resultList = new ArrayList<>();
        if (notifyTreeVo != null) {
            List<NotifyTreeVo> children = notifyTreeVo.getChildren();
            if (children == null) {
                resultList.add(notifyTreeVo.getUuid());
            } else {
                for (NotifyTreeVo child : children) {
                    resultList.addAll(getTriggerList(child));
                }
            }
        }
        return resultList;
    }

//    public static List<NotifyTreeVo> getNotifyPolicyTreeVoList() {
//        return notifyPolicyTreeVoList;
//    }

    @Override
    public void onInitialized(NeatLogicWebApplicationContext context) {
        ModuleVo moduleVo = ModuleUtil.getModuleById(context.getId());
        Map<String, INotifyPolicyHandler> map = context.getBeansOfType(INotifyPolicyHandler.class);
        for (Entry<String, INotifyPolicyHandler> entry : map.entrySet()) {
            INotifyPolicyHandler notifyPolicyHandler = entry.getValue();
            if (notifyPolicyHandler.isPublic()) {
                String moduleGroup = notifyPolicyHandler.getModuleGroup();
                if (StringUtils.isBlank(moduleGroup)) {
                    moduleGroup = moduleVo.getGroup();
                }
                String className = notifyPolicyHandler.getClassName();
                int index = className.lastIndexOf(".");
                notifyPolicyHandlerMap.put(className.substring(index + 1), notifyPolicyHandler);
                notifyPolicyHandlerMap.put(className, notifyPolicyHandler);
                handler2ModuleGroupIdMap.put(className, moduleGroup);
                handler2ModuleIdMap.put(className, moduleVo.getId());
//                notifyPolicyHandlerList.add(new NotifyPolicyHandlerVo(notifyPolicyHandler.getClassName(), notifyPolicyHandler.getName(), notifyPolicyHandler.getAuthName(), moduleVo.getId(), moduleVo.getGroup(), notifyPolicyHandler.isAllowMultiPolicy()));

//                INotifyPolicyHandlerGroup notifyPolicyHandlerGroup = notifyPolicyHandler.getGroup();
//                if (notifyPolicyHandlerGroup == null) {
//                    notifyPolicyTreeVoList.add(new NotifyTreeVo(notifyPolicyHandler.getClassName(), notifyPolicyHandler.getName()));
//                } else {
//                    NotifyTreeVo notifyPolicyGroupTreeVo = notifyPolicyGroupTreeVoMap.get(notifyPolicyHandlerGroup.getValue());
//                    if (notifyPolicyGroupTreeVo == null) {
//                        notifyPolicyGroupTreeVo = new NotifyTreeVo(notifyPolicyHandlerGroup.getValue(), notifyPolicyHandlerGroup.getText());
//                        notifyPolicyGroupTreeVoMap.put(notifyPolicyHandlerGroup.getValue(), notifyPolicyGroupTreeVo);
//                        notifyPolicyTreeVoList.add(notifyPolicyGroupTreeVo);
//                    }
//                    notifyPolicyGroupTreeVo.addChildren(new NotifyTreeVo(notifyPolicyHandler.getClassName(), notifyPolicyHandler.getName()));
//                }
            }

            NotifyTreeVo treeVo = new NotifyTreeVo(notifyPolicyHandler.getClassName(), notifyPolicyHandler.getName());
            List<NotifyTreeVo> children = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(notifyPolicyHandler.getNotifyTriggerList())) {
                for (NotifyTriggerVo notifyTriggerVo : notifyPolicyHandler.getNotifyTriggerList()) {
                    children.add(new NotifyTreeVo(notifyTriggerVo.getTrigger(), notifyTriggerVo.getTriggerName()));
                }
            }
            treeVo.setChildren(children);
            NotifyTreeVo parentTreeVo = moduleTreeVoMap.get(moduleVo.getGroup());
            if (parentTreeVo == null) {
                parentTreeVo = new NotifyTreeVo(moduleVo.getGroup(), moduleVo.getGroupName());
                moduleTreeVoMap.put(moduleVo.getGroup(), parentTreeVo);
                moduleTreeVoList.add(parentTreeVo);
            }
            parentTreeVo.addChildren(treeVo);
        }
    }

    @Override
    protected void myInit() {

    }

}
