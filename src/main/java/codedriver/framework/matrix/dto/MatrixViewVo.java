/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.framework.matrix.dto;

import codedriver.framework.file.dto.FileVo;

/**
 * @author linbq
 * @since 2021/7/15 10:15
 **/
public class MatrixViewVo {
    private String matrixUuid;
    private Long fileId;
    private FileVo fileVo;
    private transient String config;
    public String getMatrixUuid() {
        return matrixUuid;
    }

    public void setMatrixUuid(String matrixUuid) {
        this.matrixUuid = matrixUuid;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public FileVo getFileVo() {
        return fileVo;
    }

    public void setFileVo(FileVo fileVo) {
        this.fileVo = fileVo;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }
}