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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.applicationlistener.core.ModuleInitializedListenerBase;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.bootstrap.NeatLogicWebApplicationContext;
import neatlogic.framework.common.RootComponent;
import neatlogic.framework.common.config.Config;
import neatlogic.framework.common.util.FileUtil;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.importexport.constvalue.FrameworkImportExportHandlerType;
import neatlogic.framework.importexport.dto.*;
import neatlogic.framework.importexport.exception.ImportExportHandlerNotFoundException;
import neatlogic.framework.importexport.exception.ImportExportTypeInconsistencyException;
import neatlogic.module.framework.file.handler.LocalFileSystemHandler;
import neatlogic.module.framework.file.handler.MinioFileSystemHandler;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RootComponent
public class ImportExportHandlerFactory extends ModuleInitializedListenerBase {

    private static Logger logger = LoggerFactory.getLogger(ImportExportHandlerFactory.class);

    private static Map<String, ImportExportHandler> componentMap = new HashMap<>();

    public static ImportExportHandler getHandler(String handler) {
        return componentMap.get(handler);
    }

    private static FileMapper fileMapper;

    @Resource
    public void setFileMapper(FileMapper _fileMapper) {
        fileMapper = _fileMapper;
    }

    @Override
    protected void onInitialized(NeatLogicWebApplicationContext context) {
        Map<String, ImportExportHandler> myMap = context.getBeansOfType(ImportExportHandler.class);
        for (Map.Entry<String, ImportExportHandler> entry : myMap.entrySet()) {
            ImportExportHandler component = entry.getValue();
            if (component.getType() == null) {
                logger.error("ImportExportHandler '" + component.getClass().getSimpleName() + "' type is null");
                System.exit(1);
            }
            if (componentMap.containsKey(component.getType().getValue())) {
                logger.error("ImportExportHandler '" + component.getClass().getSimpleName() + "(" + component.getType().getValue() + ")' repeat");
                System.exit(1);
            }
            componentMap.put(component.getType().getValue(), component);
        }
    }

    @Override
    protected void myInit() {

    }

    public static JSONObject importData(InputStream inputStream, String type, String userSelection) {
        boolean checkAll = false;
        List<ImportDependencyTypeVo> typeList = new ArrayList<>();
//        System.out.println("userSelection = " + userSelection);
        if (StringUtils.isNotBlank(userSelection)) {
            JSONObject userSelectionObj = JSONObject.parseObject(userSelection);
            if (MapUtils.isNotEmpty(userSelectionObj)) {
                checkAll = userSelectionObj.getBooleanValue("checkAll");
                JSONArray typeArray = userSelectionObj.getJSONArray("typeList");
                if (CollectionUtils.isNotEmpty(typeArray)) {
                    typeList = typeArray.toJavaList(ImportDependencyTypeVo.class);
                }
            }
        }
        byte[] buf = new byte[1024];
        // 第一次遍历压缩包，检查是否有依赖项需要询问用户是否导入
        if (StringUtils.isBlank(userSelection)) {
            try (ZipInputStream zipIs = new ZipInputStream(inputStream);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()
            ) {
                ZipEntry zipEntry = null;
                while ((zipEntry = zipIs.getNextEntry()) != null) {
                    if (zipEntry.isDirectory()) {
                        continue;
                    }
                    out.reset();
                    String zipEntryName = zipEntry.getName();
                    if (zipEntryName.startsWith("dependency-folder/")) {
                        continue;
                    }
                    if (zipEntryName.startsWith("attachment-folder/")) {
                        continue;
                    }
                    if (zipEntry.getName().endsWith(".json")) {
                        System.out.println("zipEntryName0 = " + zipEntryName);
                        int len;
                        while ((len = zipIs.read(buf)) != -1) {
                            out.write(buf, 0, len);
                        }
                        ImportExportVo mainImportExportVo = JSONObject.parseObject(new String(out.toByteArray(), StandardCharsets.UTF_8), ImportExportVo.class);
                        if (!Objects.equals(mainImportExportVo.getType(), type)) {
                            throw new ImportExportTypeInconsistencyException(mainImportExportVo.getType(), type);
                        }
                        List<ImportExportBaseInfoVo> dependencyBaseInfoList = mainImportExportVo.getDependencyBaseInfoList();
                        if (CollectionUtils.isNotEmpty(dependencyBaseInfoList) && StringUtils.isBlank(userSelection)) {
                            ImportExportHandler importExportHandler = ImportExportHandlerFactory.getHandler(mainImportExportVo.getType());
                            if (importExportHandler == null) {
                                throw new ImportExportHandlerNotFoundException(mainImportExportVo.getType());
                            }
                            List<ImportDependencyTypeVo> importDependencyTypeList = importExportHandler.checkDependencyList(dependencyBaseInfoList);
                            if (CollectionUtils.isNotEmpty(importDependencyTypeList)) {
                                JSONObject resultObj = new JSONObject();
                                resultObj.put("checkedAll", false);
                                resultObj.put("typeList", importDependencyTypeList);
                                System.out.println("resultObj = " + resultObj);
                                return resultObj;
                            }
                            checkAll = true;
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        List<ImportExportPrimaryChangeVo> primaryChangeList = new ArrayList<>();
        Map<Long, FileVo> fileMap = new HashMap<>();
        // 第二次遍历压缩包，导入dependency-folder/{primaryKey}.json和{primaryKey}.json文件
        try (ZipInputStream zipIs = new ZipInputStream(inputStream);
             ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {
            ZipEntry zipEntry = null;
            while ((zipEntry = zipIs.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) {
                    continue;
                }
                out.reset();
                String zipEntryName = zipEntry.getName();
                if (zipEntryName.startsWith("dependency-folder/")) {
                    System.out.println("zipEntryName1 = " + zipEntryName);
                    int len;
                    while ((len = zipIs.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                    ImportExportVo dependencyVo = JSONObject.parseObject(new String(out.toByteArray(), StandardCharsets.UTF_8), ImportExportVo.class);
                    boolean flag = true;
                    if (!checkAll) {
                        flag = check(typeList, dependencyVo.getType(), dependencyVo.getPrimaryKey());
                    }
                    if (flag) {
                        ImportExportHandler importExportHandler = ImportExportHandlerFactory.getHandler(dependencyVo.getType());
                        if (importExportHandler == null) {
                            throw new ImportExportHandlerNotFoundException(dependencyVo.getType());
                        }
                        Object oldPrimaryKey = dependencyVo.getPrimaryKey();
                        Long newPrimaryKey = importExportHandler.importData(dependencyVo, primaryChangeList);
                        if (!Objects.equals(oldPrimaryKey, newPrimaryKey)) {
                            primaryChangeList.add(new ImportExportPrimaryChangeVo(dependencyVo.getType(), oldPrimaryKey, newPrimaryKey));
                        }
                        if (Objects.equals(dependencyVo.getType(), "file")) {
                            FileVo fileVo = dependencyVo.getData().toJavaObject(FileVo.class);
                            fileMap.put(fileVo.getId(), fileVo);
                        }
                    }
                } else if (zipEntryName.startsWith("attachment-folder/")) {
                    continue;
                } else {
                    System.out.println("zipEntryName1 = " + zipEntryName);
                    int len;
                    while ((len = zipIs.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                    ImportExportVo importExportVo = JSONObject.parseObject(new String(out.toByteArray(), StandardCharsets.UTF_8), ImportExportVo.class);
                    ImportExportHandler importExportHandler = ImportExportHandlerFactory.getHandler(importExportVo.getType());
                    if (importExportHandler == null) {
                        throw new ImportExportHandlerNotFoundException(importExportVo.getType());
                    }
                    importExportHandler.importData(importExportVo, primaryChangeList);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        // 第三次遍历压缩包，导入attachment-folder/{primaryKey}/xxx文件
        try (ZipInputStream zipIs = new ZipInputStream(inputStream);
             ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {
            ZipEntry zipEntry = null;
            while ((zipEntry = zipIs.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) {
                    continue;
                }
                out.reset();
                String zipEntryName = zipEntry.getName();
                if (zipEntryName.startsWith("attachment-folder/")) {
                    System.out.println("zipEntryName2 = " + zipEntryName);
                    String tenantUuid = TenantContext.get().getTenantUuid();
                    int beginIndex = zipEntryName.indexOf("/");
                    int endIndex = zipEntryName.indexOf("/", beginIndex + 1);
                    String fileIdStr = zipEntryName.substring(beginIndex + 1, endIndex);
                    Long fileId = Long.valueOf(fileIdStr);
                    boolean flag = true;
                    if (!checkAll) {
                        flag = check(typeList, FrameworkImportExportHandlerType.FILE.getValue(), fileId);
                    }
                    if (flag) {
                        FileVo fileVo = fileMap.get(fileId);
                        Object newPrimary = getNewPrimaryKey(FrameworkImportExportHandlerType.FILE.getValue(), fileId, primaryChangeList);
                        if (newPrimary != null) {
                            fileVo.setId((Long) newPrimary);
                        }
                        int len;
                        while ((len = zipIs.read(buf)) != -1) {
                            out.write(buf, 0, len);
                        }
                        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
                        String filePath;
                        try {
                            filePath = FileUtil.saveData(MinioFileSystemHandler.NAME, tenantUuid, in, fileVo.getId().toString(), fileVo.getContentType(), fileVo.getType());
                        } catch (Exception ex) {
                            //如果没有配置minioUrl，则表示不使用minio，无需抛异常
                            if (StringUtils.isNotBlank(Config.MINIO_URL())) {
                                logger.error(ex.getMessage(), ex);
                            }
                            // 如果minio出现异常，则上传到本地
                            filePath = FileUtil.saveData(LocalFileSystemHandler.NAME, tenantUuid, in, fileVo.getId().toString(), fileVo.getContentType(), fileVo.getType());
                        } finally {
                            in.close();
                        }
                        fileVo.setPath(filePath);
                        fileMapper.updateFile(fileVo);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }


    private static Object getNewPrimaryKey(String type, Object oldPrimary, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        for (ImportExportPrimaryChangeVo primaryChangeVo : primaryChangeList) {
            if (Objects.equals(primaryChangeVo.getType(), type) && Objects.equals(primaryChangeVo.getOldPrimaryKey(), oldPrimary)) {
                return primaryChangeVo.getNewPrimaryKey();
            }
        }
        return null;
    }

    private static boolean check(List<ImportDependencyTypeVo> typeList, String type, Object primaryKey) {
        for (ImportDependencyTypeVo typeVo : typeList) {
            if (Objects.equals(typeVo.getValue(), type)) {
                if (!Objects.equals(typeVo.getCheckedAll(), true)) {
                    for (ImportDependencyOptionVo optionVo : typeVo.getOptionList()) {
                        if (Objects.equals(optionVo.getValue(), primaryKey)) {
                            if (!Objects.equals(optionVo.getChecked(), true)) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
}
