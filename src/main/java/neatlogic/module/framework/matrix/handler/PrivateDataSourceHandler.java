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

package neatlogic.module.framework.matrix.handler;

import neatlogic.framework.common.constvalue.Expression;
import neatlogic.framework.dao.mapper.RoleMapper;
import neatlogic.framework.dao.mapper.TeamMapper;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.RoleVo;
import neatlogic.framework.dto.TeamVo;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.matrix.constvalue.MatrixAttributeType;
import neatlogic.framework.matrix.constvalue.MatrixType;
import neatlogic.framework.matrix.constvalue.SearchExpression;
import neatlogic.framework.matrix.core.IMatrixPrivateDataSourceHandler;
import neatlogic.framework.matrix.core.MatrixDataSourceHandlerBase;
import neatlogic.framework.matrix.core.MatrixPrivateDataSourceHandlerFactory;
import neatlogic.framework.matrix.dto.*;
import neatlogic.framework.matrix.exception.MatrixAttributeNotFoundException;
import neatlogic.framework.util.TableResultUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PrivateDataSourceHandler extends MatrixDataSourceHandlerBase {

    @Resource
    private UserMapper userMapper;

    @Resource
    private TeamMapper teamMapper;

    @Resource
    private RoleMapper roleMapper;
    @Override
    public String getHandler() {
        return MatrixType.PRIVATE.getValue();
    }

    @Override
    protected boolean mySaveMatrix(MatrixVo matrixVo) throws Exception {
        return false;
    }

    @Override
    protected void myGetMatrix(MatrixVo matrixVo) {

    }

    @Override
    protected void myDeleteMatrix(String uuid) {

    }

    @Override
    protected void myCopyMatrix(String sourceUuid, MatrixVo matrixVo) {

    }

    @Override
    protected JSONObject myImportMatrix(MatrixVo matrixVo, MultipartFile multipartFile) throws IOException {
        return null;
    }

    @Override
    protected void mySaveAttributeList(String matrixUuid, List<MatrixAttributeVo> matrixAttributeList) {

    }

    @Override
    protected List<MatrixAttributeVo> myGetAttributeList(MatrixVo matrixVo) {
        IMatrixPrivateDataSourceHandler matrixPrivateDataSourceHandler = MatrixPrivateDataSourceHandlerFactory.getHandler(matrixVo.getUuid());
        return matrixPrivateDataSourceHandler.getAttributeList();
    }

    @Override
    protected JSONObject myExportAttribute(MatrixVo matrixVo) {
        return null;
    }

    @Override
    protected JSONObject myGetTableData(MatrixDataVo dataVo) {
        IMatrixPrivateDataSourceHandler matrixPrivateDataSourceHandler = MatrixPrivateDataSourceHandlerFactory.getHandler(dataVo.getMatrixUuid());
        List<MatrixAttributeVo> attributeVoList = matrixPrivateDataSourceHandler.getAttributeList();
        List<Map<String, String>> dataList = matrixPrivateDataSourceHandler.searchTableData(dataVo);
        List<Map<String, Object>> tbodyList = matrixTableDataValueHandle(attributeVoList, dataList);
        JSONArray theadList = getTheadList(attributeVoList);
        return TableResultUtil.getResult(theadList, tbodyList, dataVo);
    }

    @Override
    protected JSONObject myTableDataSearch(MatrixDataVo dataVo) {
        IMatrixPrivateDataSourceHandler matrixPrivateDataSourceHandler = MatrixPrivateDataSourceHandlerFactory.getHandler(dataVo.getMatrixUuid());
        List<MatrixAttributeVo> attributeVoList = matrixPrivateDataSourceHandler.getAttributeList();
        JSONArray theadList = getTheadList(dataVo.getMatrixUuid(), attributeVoList, dataVo.getColumnList());
        if (!mergeFilterListAndSourceColumnList(dataVo)) {
            return TableResultUtil.getResult(theadList, new ArrayList<>(), dataVo);
        }
        List<Map<String, String>> dataList = matrixPrivateDataSourceHandler.searchTableData(dataVo);
        List<Map<String, Object>> tbodyList = matrixTableDataValueHandle(attributeVoList, dataList);
        return TableResultUtil.getResult(theadList, tbodyList, dataVo);
    }

    @Override
    protected List<Map<String, JSONObject>> myTableColumnDataSearch(MatrixDataVo dataVo) {
        List<Map<String, JSONObject>> resultList = new ArrayList<>();
        IMatrixPrivateDataSourceHandler matrixPrivateDataSourceHandler = MatrixPrivateDataSourceHandlerFactory.getHandler(dataVo.getMatrixUuid());
        List<MatrixAttributeVo> attributeList = matrixPrivateDataSourceHandler.getAttributeList();
        if (CollectionUtils.isEmpty(attributeList)) {
            return resultList;
        }
        Map<String, MatrixAttributeVo> matrixAttributeMap = new HashMap<>();
        for (MatrixAttributeVo matrixAttributeVo : attributeList) {
            matrixAttributeMap.put(matrixAttributeVo.getUuid(), matrixAttributeVo);
        }
        List<String> columnList = dataVo.getColumnList();
        for (String column : columnList) {
            if (!matrixAttributeMap.containsKey(column)) {
                throw new MatrixAttributeNotFoundException(dataVo.getMatrixUuid(), column);
            }
        }
        JSONArray defaultValue = dataVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            for (String value : defaultValue.toJavaList(String.class)) {
                if (value.contains(SELECT_COMPOSE_JOINER)) {
                    List<MatrixColumnVo> sourceColumnList = new ArrayList<>();
                    String[] split = value.split(SELECT_COMPOSE_JOINER);
                    if (StringUtils.isNotBlank(columnList.get(0))) {
                        MatrixColumnVo matrixColumnVo = new MatrixColumnVo(columnList.get(0), split[0]);
                        matrixColumnVo.setExpression(Expression.EQUAL.getExpression());
                        sourceColumnList.add(matrixColumnVo);
                    }
                    String keywordColumn = null;
                    if (columnList.size() >= 2) {
                        keywordColumn = columnList.get(1);
                    } else {
                        keywordColumn = columnList.get(0);
                    }
                    MatrixAttributeVo matrixAttribute = matrixAttributeMap.get(keywordColumn);
                    if (matrixAttribute == null) {
                        throw new MatrixAttributeNotFoundException(dataVo.getMatrixUuid(), keywordColumn);
                    }
                    MatrixColumnVo matrixColumnVo = new MatrixColumnVo(keywordColumn, split[1]);
                    matrixColumnVo.setExpression(Expression.EQUAL.getExpression());
                    sourceColumnList.add(matrixColumnVo);
                    dataVo.setSourceColumnList(sourceColumnList);
                    List<Map<String, String>> dataMapList = matrixPrivateDataSourceHandler.searchTableData(dataVo);
                    if (CollectionUtils.isNotEmpty(dataMapList)) {
                        //对dataMapList去重
                        List<Map<String, String>> distinctList = new ArrayList<>();
                        for (Map<String, String> dataMap : dataMapList) {
                            if(distinctList.contains(dataMap)){
                                continue;
                            }
                            distinctList.add(dataMap);
                            Map<String, JSONObject> resultMap = new HashMap<>(dataMap.size());
                            for (Map.Entry<String, String> entry : dataMap.entrySet()) {
                                String attributeUuid = entry.getKey();
                                resultMap.put(attributeUuid, matrixAttributeValueHandle(matrixAttributeMap.get(attributeUuid), entry.getValue()));
                            }
                            resultList.add(resultMap);
                        }
                    }
                }
            }
        } else {
            if (!mergeFilterListAndSourceColumnList(dataVo)) {
                return resultList;
            }
            String keywordColumn = dataVo.getKeywordColumn();
            String keyword = dataVo.getKeyword();
            if (StringUtils.isNotBlank(keywordColumn) && StringUtils.isNotBlank(keyword)) {
                MatrixAttributeVo matrixAttribute = matrixAttributeMap.get(keywordColumn);
                if (matrixAttribute == null) {
                    throw new MatrixAttributeNotFoundException(dataVo.getMatrixUuid(), keywordColumn);
                }
                dataVo.setAttrType(matrixAttribute.getType());
                dataVo.setKeywordExpression(Expression.LIKE.getExpression());
            }
            //下面逻辑适用于下拉框滚动加载，也可以搜索，但是一页返回的数据量可能会小于pageSize，因为做了去重处理
            List<Map<String, String>> dataMapList = matrixPrivateDataSourceHandler.searchTableData(dataVo);
            if (CollectionUtils.isEmpty(dataMapList)) {
                return resultList;
            }
            List<Map<String, String>> distinctList = new ArrayList<>(100);
            for (Map<String, String> dataMap : dataMapList) {
                if(distinctList.contains(dataMap)){
                    continue;
                }
                distinctList.add(dataMap);
                Map<String, JSONObject> resultMap = new HashMap<>(dataMap.size());
                for (Map.Entry<String, String> entry : dataMap.entrySet()) {
                    String attributeUuid = entry.getKey();
                    resultMap.put(attributeUuid, matrixAttributeValueHandle(matrixAttributeMap.get(attributeUuid), entry.getValue()));
                }
                resultList.add(resultMap);
            }
        }
        return resultList;
    }

    @Override
    protected List<Map<String, JSONObject>> mySearchTableDataNew(MatrixDataVo dataVo) {
        List<Map<String, JSONObject>> resultList = new ArrayList<>();
        IMatrixPrivateDataSourceHandler matrixPrivateDataSourceHandler = MatrixPrivateDataSourceHandlerFactory.getHandler(dataVo.getMatrixUuid());
        List<MatrixAttributeVo> attributeList = matrixPrivateDataSourceHandler.getAttributeList();
        if (CollectionUtils.isEmpty(attributeList)) {
            return resultList;
        }
        Map<String, MatrixAttributeVo> matrixAttributeMap = attributeList.stream().collect(Collectors.toMap(e -> e.getUuid(), e -> e));
        List<Map<String, String>> dataMapList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(dataVo.getDefaultValueFilterList())) {
            for (MatrixDefaultValueFilterVo defaultValueFilterVo : dataVo.getDefaultValueFilterList()) {
                List<MatrixFilterVo> filterList = new ArrayList<>();
                MatrixKeywordFilterVo valueFieldFilter = defaultValueFilterVo.getValueFieldFilter();
                filterList.add(new MatrixFilterVo(valueFieldFilter.getUuid(), valueFieldFilter.getExpression(), Arrays.asList(valueFieldFilter.getValue())));
                MatrixKeywordFilterVo textFieldFilter = defaultValueFilterVo.getTextFieldFilter();
                if (!Objects.equals(valueFieldFilter.getUuid(), textFieldFilter.getUuid())) {
                    filterList.add(new MatrixFilterVo(textFieldFilter.getUuid(), textFieldFilter.getExpression(), Arrays.asList(textFieldFilter.getValue())));
                }
                dataVo.setFilterList(filterList);
                List<Map<String, String>> list = matrixPrivateDataSourceHandler.searchTableData(dataVo);
                if (CollectionUtils.isNotEmpty(list)) {
                    dataMapList.addAll(list);
                }
            }
        } else {
            String keywordColumn = dataVo.getKeywordColumn();
            String keyword = dataVo.getKeyword();
            if (StringUtils.isNotBlank(keywordColumn) && StringUtils.isNotBlank(keyword)) {
                List<MatrixFilterVo> filterList = dataVo.getFilterList();
                if (filterList == null) {
                    filterList = new ArrayList<>();
                }
                filterList.add(new MatrixFilterVo(keywordColumn, SearchExpression.LI.getExpression(), Arrays.asList(keyword)));
                dataVo.setFilterList(filterList);
            }
            //下面逻辑适用于下拉框滚动加载，也可以搜索，但是一页返回的数据量可能会小于pageSize，因为做了去重处理
            dataMapList = matrixPrivateDataSourceHandler.searchTableData(dataVo);
        }

        if (CollectionUtils.isEmpty(dataMapList)) {
            return resultList;
        }
        List<Map<String, String>> distinctList = new ArrayList<>(100);
        for (Map<String, String> dataMap : dataMapList) {
            if(distinctList.contains(dataMap)){
                continue;
            }
            distinctList.add(dataMap);
            Map<String, JSONObject> resultMap = new HashMap<>(dataMap.size());
            for (Map.Entry<String, String> entry : dataMap.entrySet()) {
                String attributeUuid = entry.getKey();
                resultMap.put(attributeUuid, matrixAttributeValueHandle(matrixAttributeMap.get(attributeUuid), entry.getValue()));
            }
            resultList.add(resultMap);
        }
        return resultList;
    }

    @Override
    protected JSONObject mySaveTableRowData(String matrixUuid, JSONObject rowData) {
        return null;
    }

    @Override
    protected Map<String, String> myGetTableRowData(MatrixDataVo matrixDataVo) {
        return null;
    }

    @Override
    protected void myDeleteTableRowData(String matrixUuid, List<String> uuidList) {

    }

    private List<Map<String, Object>> matrixTableDataValueHandle(List<MatrixAttributeVo> matrixAttributeList, List<Map<String, String>> valueList) {
        if (CollectionUtils.isNotEmpty(matrixAttributeList)) {
            Map<String, MatrixAttributeVo> matrixAttributeMap = new HashMap<>();
            for (MatrixAttributeVo matrixAttributeVo : matrixAttributeList) {
                matrixAttributeMap.put(matrixAttributeVo.getUuid(), matrixAttributeVo);
            }
            if (CollectionUtils.isNotEmpty(valueList)) {
                List<Map<String, Object>> resultList = new ArrayList<>(valueList.size());
                for (Map<String, String> valueMap : valueList) {
                    Map<String, Object> resultMap = new HashMap<>();
                    for (Map.Entry<String, String> entry : valueMap.entrySet()) {
                        String attributeUuid = entry.getKey();
                        resultMap.put(attributeUuid, matrixAttributeValueHandle(matrixAttributeMap.get(attributeUuid), entry.getValue()));
                    }
                    resultList.add(resultMap);
                }
                return resultList;
            }
        }
        return null;
    }

    private JSONObject matrixAttributeValueHandle(MatrixAttributeVo matrixAttribute, Object valueObj) {
        JSONObject resultObj = new JSONObject();
        String type = MatrixAttributeType.INPUT.getValue();
        if (matrixAttribute != null) {
            type = matrixAttribute.getType();
        }
        resultObj.put("type", type);
        if (valueObj == null) {
            resultObj.put("value", null);
            resultObj.put("text", null);
            return resultObj;
        }
        String value = valueObj.toString();
        resultObj.put("value", value);
        resultObj.put("text", value);
        if (MatrixAttributeType.SELECT.getValue().equals(type)) {
            if (matrixAttribute != null) {
                JSONObject config = matrixAttribute.getConfig();
                if (MapUtils.isNotEmpty(config)) {
                    JSONArray dataList = config.getJSONArray("dataList");
                    if (CollectionUtils.isNotEmpty(dataList)) {
                        for (int i = 0; i < dataList.size(); i++) {
                            JSONObject data = dataList.getJSONObject(i);
                            if (Objects.equals(value, data.getString("value"))) {
                                resultObj.put("text", data.getString("text"));
                            }
                        }
                    }
                }
            }
        } else if (MatrixAttributeType.USER.getValue().equals(type)) {
            UserVo userVo = userMapper.getUserBaseInfoByUuid(value);
            if (userVo != null) {
                resultObj.put("text", userVo.getUserName());
                resultObj.put("avatar", userVo.getAvatar());
                resultObj.put("pinyin", userVo.getPinyin());
                resultObj.put("vipLevel", userVo.getVipLevel());
            }
        } else if (MatrixAttributeType.TEAM.getValue().equals(type)) {
            TeamVo teamVo = teamMapper.getTeamByUuid(value);
            if (teamVo != null) {
                resultObj.put("text", teamVo.getName());
            }
        } else if (MatrixAttributeType.ROLE.getValue().equals(type)) {
            RoleVo roleVo = roleMapper.getRoleByUuid(value);
            if (roleVo != null) {
                resultObj.put("text", roleVo.getName());
            }
        }
        return resultObj;
    }
}
