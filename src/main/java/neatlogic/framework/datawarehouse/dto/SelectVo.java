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

package neatlogic.framework.datawarehouse.dto;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SelectVo {
    public static int RSEULT_TYPE_LIST = 0;
    public static int RESULT_TYPE_MAP = 1;
    private String id;
    private Integer datasource;
    private ResultMapVo resultMap;
    private boolean lazyLoad = false;
    private String sql;
    private String template;
    private int queryTimeout = 30;
    private int resultType = 0;
    private boolean needPage = false;
    private int pageSize = 10;
    private Map<String, Object> paramMap;
    private List<Object> paramList;
    private List<Map<String, Object>> resultList;

    public Integer getDatasource() {
        return datasource;
    }

    public void setDatasource(Integer datasource) {
        this.datasource = datasource;
    }

    public int getQueryTimeout() {
        return queryTimeout;
    }

    public void setQueryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    public int getResultType() {
        return resultType;
    }

    public void setResultType(int resultType) {
        this.resultType = resultType;
    }

    private void resultMapRecursion(Map<String, ResultMapVo> returnMap, String mapName, ResultMapVo resultMap) {
        if (resultMap != null) {
            returnMap.put(mapName, resultMap);
            Map<String, ResultMapVo> tmpMap = resultMap.getResultMap();
            if (tmpMap != null) {
                Iterator<Map.Entry<String, ResultMapVo>> iter = tmpMap.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, ResultMapVo> entry = iter.next();
                    resultMapRecursion(returnMap, entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public Map<String, ResultMapVo> getAllResultMap() {
        if (this.resultMap != null) {
            Map<String, ResultMapVo> returnMap = new HashMap<String, ResultMapVo>();
            resultMapRecursion(returnMap, "ROOT", this.resultMap);
            return returnMap;
        }
        return null;
    }

    public List<Map<String, Object>> getResultList() {
        return resultList;
    }

    public void setResultList(List<Map<String, Object>> resultList) {
        this.resultList = resultList;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isLazyLoad() {
        return lazyLoad;
    }

    public void setLazyLoad(boolean lazyLoad) {
        this.lazyLoad = lazyLoad;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Map<String, Object> getParamMap() {
        return paramMap;
    }

    public void setParamMap(Map<String, Object> paramMap) {
        this.paramMap = paramMap;
    }

    public ResultMapVo getResultMap() {
        return resultMap;
    }

    public void setResultMap(ResultMapVo resultMap) {
        this.resultMap = resultMap;
    }

    public List<Object> getParamList() {
        return paramList;
    }

    public void setParamList(List<Object> paramList) {
        this.paramList = paramList;
    }

    public boolean isNeedPage() {
        return needPage;
    }

    public void setNeedPage(boolean needPage) {
        this.needPage = needPage;
    }

    public void setNeedPage(String _needPage) {
        try {
            this.needPage = Boolean.parseBoolean(_needPage);
        } catch (Exception ex) {
            this.needPage = false;
        }
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public void setPageSize(String _pageSize) {
        try {
            this.pageSize = Integer.parseInt(_pageSize);
        } catch (Exception ex) {
            this.pageSize = 10;
        }
    }

}
