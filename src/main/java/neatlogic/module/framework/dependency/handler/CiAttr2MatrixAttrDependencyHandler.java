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

package neatlogic.module.framework.dependency.handler;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.dependency.constvalue.FrameworkFromType;
import neatlogic.framework.dependency.core.FixedTableDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.framework.dependency.dto.DependencyVo;
import neatlogic.framework.matrix.dao.mapper.MatrixMapper;
import neatlogic.framework.matrix.dto.MatrixCiVo;
import neatlogic.framework.matrix.dto.MatrixVo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 矩阵属性引用cmdbci模型属性或关系
 * @author linbq
 * @since 2022/1/11 13:05
 **/
@Component
public class CiAttr2MatrixAttrDependencyHandler extends FixedTableDependencyHandlerBase {

    @Resource
    private MatrixMapper matrixMapper;

    @Override
    protected DependencyInfoVo parse(DependencyVo dependencyVo) {
        JSONObject config = dependencyVo.getConfig();
        if (MapUtils.isNotEmpty(config)) {
            String matrixUuid = config.getString("matrixUuid");
            if (StringUtils.isNotBlank(matrixUuid)) {
                MatrixVo matrixVo = matrixMapper.getMatrixByUuid(matrixUuid);
                if (matrixVo != null) {
                    MatrixCiVo matrixCiVo = matrixMapper.getMatrixCiByMatrixUuid(matrixUuid);
                    if (matrixCiVo != null) {
                        JSONObject matrixCiconfig = matrixCiVo.getConfig();
                        if (MapUtils.isNotEmpty(matrixCiconfig)) {
                            JSONArray showAttributeArray = matrixCiconfig.getJSONArray("showAttributeList");
                            if (CollectionUtils.isNotEmpty(showAttributeArray)) {
                                for (int i = 0; i < showAttributeArray.size(); i++) {
                                    JSONObject showAttributeObj = showAttributeArray.getJSONObject(i);
                                    if (MapUtils.isNotEmpty(showAttributeObj)) {
                                        String label = showAttributeObj.getString("label");
                                        if (label.endsWith(dependencyVo.getFrom())) {
                                            JSONObject dependencyInfoConfig = new JSONObject();
                                            dependencyInfoConfig.put("matrixUuid", matrixVo.getUuid());
                                            dependencyInfoConfig.put("matrixName", matrixVo.getName());
                                            dependencyInfoConfig.put("matrixType", matrixVo.getType());
                                            String toName = showAttributeObj.getString("name");
//                                            dependencyInfoConfig.put("attributeName", toName);
                                            List<String> pathList = new ArrayList<>();
                                            pathList.add("矩阵管理");
                                            pathList.add(matrixVo.getName());
                                            String lastName = toName;
//                                            String pathFormat = "矩阵-${DATA.matrixName}-${DATA.attributeName}";
                                            String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/framework.html#/matrix-view-edit?uuid=${DATA.matrixUuid}&name=${DATA.matrixName}&type=${DATA.matrixType}";
                                            return new DependencyInfoVo(matrixVo.getUuid(), dependencyInfoConfig, lastName, pathList, urlFormat, this.getGroupName());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public IFromType getFromType() {
        return FrameworkFromType.CMDBCIATTR;
    }
}
