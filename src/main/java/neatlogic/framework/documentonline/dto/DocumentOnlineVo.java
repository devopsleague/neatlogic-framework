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

package neatlogic.framework.documentonline.dto;

import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.EntityField;

import java.io.Serializable;
import java.util.List;

public class DocumentOnlineVo implements Serializable {

    private static final long serialVersionUID = -928973151322839787L;

    @EntityField(name = "文件名", type = ApiParamType.STRING)
    private String fileName;
    @EntityField(name = "文件路径", type = ApiParamType.STRING)
    private String filePath;
    @EntityField(name = "文件上层名称列表", type = ApiParamType.JSONARRAY)
    private List<String> upwardNameList;
    @EntityField(name = "内容", type = ApiParamType.STRING)
    private String content;
    @EntityField(name = "锚点", type = ApiParamType.STRING)
    private String anchorPoint;
    @EntityField(name = "映射配置列表", type = ApiParamType.JSONARRAY)
    private List<DocumentOnlineConfigVo> configList;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public List<String> getUpwardNameList() {
        return upwardNameList;
    }

    public void setUpwardNameList(List<String> upwardNameList) {
        this.upwardNameList = upwardNameList;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAnchorPoint() {
        return anchorPoint;
    }

    public void setAnchorPoint(String anchorPoint) {
        this.anchorPoint = anchorPoint;
    }

    public List<DocumentOnlineConfigVo> getConfigList() {
        return configList;
    }

    public void setConfigList(List<DocumentOnlineConfigVo> configList) {
        this.configList = configList;
    }
}
