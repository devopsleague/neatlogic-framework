/*
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
 */

package neatlogic.framework.dependency.core;

import neatlogic.framework.dependency.dao.mapper.DependencyMapper;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.util.I18nUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 自定义表结构依赖关系处理器基类
 *
 * @author: linbq
 * @since: 2021/4/1 11:43
 **/
public abstract class CustomTableDependencyHandlerBase implements IDependencyHandler {

    private String groupName;

    protected static DependencyMapper dependencyMapper;

    @Resource
    public void setDependencyMapper(DependencyMapper _dependencyMapper) {
        dependencyMapper = _dependencyMapper;
    }

    @Override
    public String getGroupName() {
        return I18nUtils.getMessage(groupName);
    }

    @Override
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    /**
     * 表名
     *
     * @return
     */
    protected abstract String getTableName();

    /**
     * 被引用者（上游）字段
     *
     * @return
     */
    protected abstract String getFromField();

    /**
     * 引用者（下游）字段
     *
     * @return
     */
    protected abstract String getToField();

    /**
     * 引用者（下游）字段列表
     *
     * @return
     */
    protected abstract List<String> getToFieldList();

    /**
     * 插入一条引用关系数据
     *
     * @param from 被引用者（上游）值（如：服务时间窗口uuid）
     * @param to   引用者（下游）值（如：服务uuid）
     * @return
     */
    @Override
    public int insert(Object from, Object to) {
        return insert(from, to, null);
    }

    /**
     * 插入一条引用关系数据
     *
     * @param from   被引用者（上游）值（如：服务时间窗口uuid）
     * @param to     引用者（下游）值（如：服务uuid）
     * @param config 额外数据
     * @return
     */
    @Override
    public int insert(Object from, Object to, JSONObject config) {
        if (to instanceof JSONArray) {
            return dependencyMapper.insertIgnoreDependencyForCallerFieldList(getTableName(), getFromField(), getToFieldList(), from, (JSONArray) to);
        } else {
            return dependencyMapper.insertIgnoreDependencyForCallerField(getTableName(), getFromField(), getToField(), from, to);
        }
    }

    /**
     * 删除引用关系
     *
     * @param to 引用者（下游）值（如：服务uuid）
     * @return
     */
    @Override
    public int delete(Object to) {
        return dependencyMapper.deleteDependencyByCaller(getTableName(), getToField(), to);
    }

    /**
     * 删除引用关系
     *
     * @param from 被引用者（上游）值（如：服务时间窗口uuid）
     * @return 删除个数
     */
    @Override
    public int deleteByFrom(Object from) {
        return dependencyMapper.deleteDependencyByCallee(getTableName(), getFromField(), from);
    }

    /**
     * 查询引用列表数据
     *
     * @param from     被引用者（上游）值（如：服务时间窗口uuid）
     * @param startNum 开始行号
     * @param pageSize 每页条数
     * @return
     */
    @Override
    public List<DependencyInfoVo> getDependencyList(Object from, int startNum, int pageSize) {
        List<DependencyInfoVo> resultList = new ArrayList<>();
        List<Map<String, Object>> callerList = dependencyMapper.getCallerListByCallee(getTableName(), getFromField(), from, startNum, pageSize);
        for (Object caller : callerList) {
            DependencyInfoVo valueTextVo = parse(caller);
            if (valueTextVo != null) {
                resultList.add(valueTextVo);
            }
        }
        return resultList;
    }

    /**
     * 查询引用次数
     *
     * @param from 被引用者（上游）值（如：服务时间窗口uuid）
     * @return
     */
    @Override
    public int getDependencyCount(Object from) {
        return dependencyMapper.getCallerCountByCallee(getTableName(), getFromField(), from);
    }

    /**
     * 批量查询引用次数
     *
     * @param fromList
     * @return
     */
    public List<Map<Object, Integer>> getBatchDependencyCount(Object fromList) {
        return dependencyMapper.getBatchCallerCountByCallee(getTableName(), getFromField(), (List<Object>) fromList);
    }

    /**
     * 解析数据，拼装跳转url，返回引用下拉列表一个选项数据结构
     *
     * @param dependencyObj 引用关系数据
     * @return
     */
    protected abstract DependencyInfoVo parse(Object dependencyObj);
}
