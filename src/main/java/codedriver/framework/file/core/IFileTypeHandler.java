/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.framework.file.core;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.file.dto.FileVo;

public interface IFileTypeHandler {
    /**
     * 校验附件是否允许访问
     *
     * @param userUuid 用户uuid
     * @param fileVo   附件信息
     * @param jsonObj  校验所需参数
     * @return 是否允许访问
     */
    boolean valid(String userUuid, FileVo fileVo, JSONObject jsonObj);

    String getName();

    String getDisplayName();

    void deleteFile(Long fileId) throws Exception;

    void afterUpload(FileVo fileVo, JSONObject jsonObj);
}
