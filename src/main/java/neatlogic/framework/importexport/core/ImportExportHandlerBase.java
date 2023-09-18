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

package neatlogic.framework.importexport.core;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.importexport.dto.*;
import neatlogic.framework.importexport.exception.ImportExportHandlerNotFoundException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class ImportExportHandlerBase implements ImportExportHandler {

    private static Logger logger = LoggerFactory.getLogger(ImportExportHandlerBase.class);
    /**
     * 检查导入依赖列表中的对象是否已经存在，存在则需要让用户决定是否覆盖
     * @param dependencyBaseInfoList
     * @return
     */
    @Override
    public List<ImportDependencyTypeVo> checkDependencyList(List<ImportExportBaseInfoVo> dependencyBaseInfoList) {
        if (CollectionUtils.isEmpty(dependencyBaseInfoList)) {
            return null;
        }
        Map<String, ImportDependencyTypeVo> importDependencyTypeMap = new HashMap<>();
        for (ImportExportBaseInfoVo dependencyBaseInfoVo : dependencyBaseInfoList) {
            ImportExportHandler importExportHandler = ImportExportHandlerFactory.getHandler(dependencyBaseInfoVo.getType());
            if (importExportHandler == null) {
                throw new ImportExportHandlerNotFoundException(dependencyBaseInfoVo.getType());
            }
            if (StringUtils.isBlank(dependencyBaseInfoVo.getName())) {
                if (logger.isWarnEnabled()) {
                    logger.warn("The name of the dependency object " + JSONObject.toJSONString(dependencyBaseInfoVo) + " is null");
                }
            }
            if (importExportHandler.checkIsExists(dependencyBaseInfoVo)) {
                ImportDependencyTypeVo importDependencyTypeVo = importDependencyTypeMap.get(dependencyBaseInfoVo.getType());
                if (importDependencyTypeVo == null) {
                    importDependencyTypeVo = new ImportDependencyTypeVo();
                    ImportExportHandlerType type = importExportHandler.getType();
                    importDependencyTypeVo.setValue(type.getValue());
                    importDependencyTypeVo.setText(type.getText());
                    importDependencyTypeMap.put(dependencyBaseInfoVo.getType(), importDependencyTypeVo);
                }
                List<ImportDependencyOptionVo> optionList = importDependencyTypeVo.getOptionList();
                if (optionList == null) {
                    optionList = new ArrayList<>();
                    importDependencyTypeVo.setOptionList(optionList);
                }
                ImportDependencyOptionVo importDependencyOptionVo = new ImportDependencyOptionVo();
                importDependencyOptionVo.setValue(dependencyBaseInfoVo.getPrimaryKey());
                importDependencyOptionVo.setText(dependencyBaseInfoVo.getName());
                importDependencyOptionVo.setChecked(false);
                optionList.add(importDependencyOptionVo);
            }
        }
        if (MapUtils.isNotEmpty(importDependencyTypeMap)) {
            return new ArrayList<>(importDependencyTypeMap.values());
        }
        return null;
    }
    /**
     * 导出数据
     * @param primaryKey 导出对象主键
     * @param dependencyList 导出对象依赖列表
     * @param zipOutputStream 压缩输出流
     * @return
     */
    @Override
    public ImportExportVo exportData(Object primaryKey, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream) {
        ImportExportVo importExportVo = myExportData(primaryKey, dependencyList, zipOutputStream);
        if (importExportVo != null) {
            importExportVo.setType(this.getType().getValue());
            return importExportVo;
        }
        return null;
    }

    /**
     * 导出数据
     * @param primaryKey 导出对象主键
     * @param dependencyList 导出对象依赖列表
     * @param zipOutputStream 压缩输出流
     * @return
     */
    protected abstract ImportExportVo myExportData(Object primaryKey, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream);

    /**
     * 导出依赖对象数据
     * @param type
     * @param primaryKey
     * @param dependencyList
     * @param zipOutputStream
     */
    protected void doExportData(ImportExportHandlerType type, Object primaryKey, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream) {
        ImportExportHandler importExportHandler = ImportExportHandlerFactory.getHandler(type.getValue());
        if (importExportHandler == null) {
            throw new ImportExportHandlerNotFoundException(type.getText());
        }
        for (ImportExportBaseInfoVo importExportVo : dependencyList) {
            if (Objects.equals(importExportVo.getPrimaryKey(), primaryKey) && Objects.equals(importExportVo.getType(), type.getValue())) {
                if (logger.isWarnEnabled()) {
                    logger.warn("The data of type " + importExportVo.getType() + " whose primary key is " + importExportVo.getPrimaryKey() + " has been exported");
                }
                return;
            }
        }
        ImportExportBaseInfoVo dependencyVo = new ImportExportBaseInfoVo(type.getValue(), primaryKey);
        dependencyList.add(dependencyVo);
        ImportExportVo importExportVo = importExportHandler.exportData(primaryKey, dependencyList, zipOutputStream);
        if (importExportVo != null) {
            if (logger.isWarnEnabled()) {
                logger.warn("export data: " + importExportVo.getType() + "-" + importExportVo.getName() + "-" + importExportVo.getPrimaryKey());
            }
            dependencyVo.setName(importExportVo.getName());
            try {
                zipOutputStream.putNextEntry(new ZipEntry("dependency-folder/" + importExportVo.getPrimaryKey() + ".json"));
                zipOutputStream.write(JSONObject.toJSONBytes(importExportVo));
                zipOutputStream.closeEntry();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 获取新的primary，如果返回结果为null，说明primary没有变化
     * @param type
     * @param oldPrimary
     * @param primaryChangeList
     * @return
     */
    protected Object getNewPrimaryKey(ImportExportHandlerType type, Object oldPrimary, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        for (ImportExportPrimaryChangeVo primaryChangeVo : primaryChangeList) {
            if (Objects.equals(primaryChangeVo.getType(), type.getValue()) && Objects.equals(primaryChangeVo.getOldPrimaryKey(), oldPrimary)) {
                return primaryChangeVo.getNewPrimaryKey();
            }
        }
        return null;
    }
}
