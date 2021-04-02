/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.framework.dependency.core;

import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.dto.ValueTextVo;
import org.apache.commons.collections4.CollectionUtils;
import org.xlsx4j.sml.Col;

import java.util.ArrayList;
import java.util.List;

/**
 * 依赖关系管理类，基本操作：保存依赖关系数据，删除依赖关系数据，查询依赖数量，查询引用列表
 * @author: linbq
 * @since: 2021/4/1 12:03
 **/
public class DependencyManager {

    /**
     * 插入一条引用关系数据
     * @param clazz
     * @param callee
     * @param caller
     * @return
     */
    public static int insertOne(Class<? extends IDependencyHandler> clazz, Object callee, Object caller){
        IDependencyHandler dependencyHandler = DependencyHandlerFactory.getHandler(clazz.getName());
        dependencyHandler.delete(caller);
        return dependencyHandler.insert(callee, caller);
    }

    /**
     * 删除引用关系
     * @param clazz
     * @param caller
     * @return
     */
    public static int delete(Class<? extends IDependencyHandler> clazz, String caller){
        IDependencyHandler dependencyHandler = DependencyHandlerFactory.getHandler(clazz.getName());
        return dependencyHandler.delete(caller);
    }

    /**
     * 查询引用列表
     * @param calleeType
     * @param callee
     * @return
     */
    public static List<ValueTextVo> getDependencyList(ICalleeType calleeType, Object callee, BasePageVo basePageVo){
        List<ValueTextVo> resultList = new ArrayList<>();
        List<IDependencyHandler> dependencyHandlerList = DependencyHandlerFactory.getHandlerList(calleeType);
        int pageSize = basePageVo.getPageSize();
        int startNum = basePageVo.getStartNum();
        for(IDependencyHandler handler : dependencyHandlerList){
            if(pageSize == 0){
                break;
            }
            int count = handler.getCallerCount(callee);
            if(startNum > count){
                startNum -= count;
                continue;
            }
            List<ValueTextVo> callerList = handler.getCallerList(callee, startNum, pageSize);
            resultList.addAll(callerList);
            pageSize -= callerList.size();
            startNum = 0;
        }
        return resultList;
    }

    /**
     * 查询引用个数
     * @param calleeType
     * @param callee
     * @return
     */
    public static int getDependencyCount(ICalleeType calleeType, Object callee){
        int sum = 0;
        List<IDependencyHandler> dependencyHandlerList = DependencyHandlerFactory.getHandlerList(calleeType);
        if(CollectionUtils.isNotEmpty(dependencyHandlerList)){
            for(IDependencyHandler handler : dependencyHandlerList){
                sum += handler.getCallerCount(callee);
            }
        }
        return sum;
    }
}
