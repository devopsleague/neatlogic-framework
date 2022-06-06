/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.framework.dependency.handler;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.dependency.constvalue.FrameworkFromType;
import codedriver.framework.dependency.core.FixedTableDependencyHandlerBase;
import codedriver.framework.dependency.core.IFromType;
import codedriver.framework.dependency.dto.DependencyInfoVo;
import codedriver.framework.dependency.dto.DependencyVo;
import codedriver.framework.matrix.dao.mapper.MatrixMapper;
import codedriver.framework.matrix.dto.MatrixCiVo;
import codedriver.framework.matrix.dto.MatrixVo;
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
