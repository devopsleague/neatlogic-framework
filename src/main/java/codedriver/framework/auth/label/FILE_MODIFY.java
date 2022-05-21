/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.framework.auth.label;

import codedriver.framework.auth.core.AuthBase;

/**
 * @author longrf
 * @date 2022/4/8 3:49 下午
 */
public class FILE_MODIFY extends AuthBase {

    @Override
    public String getAuthDisplayName() {
        return "附件管理权限";
    }

    @Override
    public String getAuthIntroduction() {
        return "对所有附件进行管理";
    }

    @Override
    public String getAuthGroup() {
        return "framework";
    }

    @Override
    public Integer getSort() {
        return 24;
    }
}
