/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
 */

package neatlogic.framework.dependency.core;

import neatlogic.framework.dependency.dto.DependencyInfoVo;
import com.alibaba.fastjson.JSONObject;
import org.springframework.util.ClassUtils;

import java.util.List;
import java.util.Map;

/**
 * 依赖关系处理器接口
 *
 * @author: linbq
 * @since: 2021/4/1 11:09
 **/
public interface IDependencyHandler {

    /**
     * 处理器唯一标识
     *
     * @return
     */
    default String getHandler() {
        return ClassUtils.getUserClass(this.getClass()).getSimpleName();
    }

    /**
     * 被引用者（上游）类型
     *
     * @return
     */
    IFromType getFromType();

    /**
     * 依赖关系能否解除，比如服务引用时间窗口可以解除，但工单引用时间窗口不可以解除
     *
     * @return
     */
    default boolean canBeLifted() {
        return true;
    }

    void setGroupName(String groupName);

    String getGroupName();

    /**
     * 插入一条引用关系数据
     *
     * @param from 被引用者（上游）值（如：服务时间窗口uuid）
     * @param to   引用者（下游）值（如：服务uuid）
     * @return
     */
    int insert(Object from, Object to);

    /**
     * 插入一条引用关系数据
     *
     * @param from   被引用者（上游）值（如：服务时间窗口uuid）
     * @param to     引用者（下游）值（如：服务uuid）
     * @param config 额外数据
     * @return
     */
    int insert(Object from, Object to, JSONObject config);

    /**
     * 删除引用关系
     *
     * @param to 引用者（下游）值（如：服务uuid）
     * @return
     */
    int delete(Object to);

    /**
     * 删除引用关系
     *
     * @param from  被引用者（上游）值（如：服务时间窗口uuid）
     * @return 删除个数
     */
    int deleteByFrom(Object from);

    /**
     * 查询引用列表数据
     *
     * @param from     被引用者（上游）值（如：服务时间窗口uuid）
     * @param startNum 开始行号
     * @param pageSize 数据量
     * @return
     */
    List<DependencyInfoVo> getDependencyList(Object from, int startNum, int pageSize);

    /**
     * 查询引用次数
     *
     * @param to 被引用者（上游）值（如：服务时间窗口uuid）
     * @return
     */
    int getDependencyCount(Object to);

    /**
     * 批量查询引用次数
     *
     * @param fromList
     * @return
     */
    default List<Map<Object, Integer>> getBatchDependencyCount(Object fromList) {
        return null;
    }

}
