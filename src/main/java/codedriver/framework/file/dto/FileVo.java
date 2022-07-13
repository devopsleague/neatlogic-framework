/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.framework.file.dto;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BaseEditorVo;
import codedriver.framework.file.core.FileTypeHandlerFactory;
import codedriver.framework.file.core.IFileTypeHandler;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;
import codedriver.framework.util.TimeUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class FileVo extends BaseEditorVo {
    @EntityField(name = "附件id", type = ApiParamType.LONG)
    private Long id;
    @EntityField(name = "附件保存路径", type = ApiParamType.STRING)
    private String path;
    @EntityField(name = "附件名称", type = ApiParamType.STRING)
    private String name;
    @EntityField(name = "附件大小（字节）", type = ApiParamType.INTEGER)
    private Long size;
    @EntityField(name = "附件大小", type = ApiParamType.STRING)
    private String sizeText;
    @EntityField(name = "上传用户", type = ApiParamType.STRING)
    private String userUuid;
    @EntityField(name = "上传时间", type = ApiParamType.STRING)
    private Date uploadTime;

    @JSONField(serialize = false)
    private JSONObject uploadTimeRange;//搜索条件上传时间范围
    @EntityField(name = "附件归属", type = ApiParamType.STRING)
    private String type;
    @EntityField(name = "附件归属名称", type = ApiParamType.STRING)
    private String typeText;
    @EntityField(name = "扩展名", type = ApiParamType.STRING)
    private String ext;
    @EntityField(name = "内容类型", type = ApiParamType.STRING)
    private String contentType;
    @EntityField(name = "下载url", type = ApiParamType.STRING)
    private String url;
    @JSONField(serialize = false)
    private List<Map<String, String>> sortList;//排序设置

    public Long getId() {
        if (id == null) {
            id = SnowflakeUtil.uniqueLong();
        }
        return id;
    }

    public String getTypeText() {
        if (StringUtils.isNotBlank(type)) {
            IFileTypeHandler handler = FileTypeHandlerFactory.getHandler(type);
            if (handler != null) {
                typeText = handler.getDisplayName();
            }
        }
        return typeText;
    }


    DecimalFormat decimalFormat = new DecimalFormat("0.##");
    String[] units = new String[]{"字节", "KB", "MB", "GB"};

    public String getSizeText() {
        if (StringUtils.isBlank(sizeText)) {
            double d = size;
            int unitindex = 0;
            while (d > 1024 && unitindex <= 3) {
                d = d / 1024;
                unitindex += 1;
            }
            sizeText = decimalFormat.format(d) + units[unitindex];
        }
        return sizeText;
    }

    public List<Map<String, String>> getSortList() {
        return sortList;
    }

    public void setSortList(List<Map<String, String>> sortList) {
        this.sortList = sortList;
    }

    public List<Long> getUploadTimeRange() {
        return TimeUtil.getTimeRangeList(this.uploadTimeRange);
    }

    public void setUploadTimeRange(JSONObject uploadTimeRange) {
        this.uploadTimeRange = uploadTimeRange;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public Date getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(Date uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExt() {
        if (StringUtils.isNotBlank(name) && name.contains(".")) {
            ext = name.substring(name.lastIndexOf(".") + 1);
        }
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
