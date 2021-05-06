/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.framework.auth.label;

import codedriver.framework.auth.core.AuthBase;

public class FRAMEWOKR_BASE extends AuthBase {

    @Override
    public String getAuthDisplayName() {
        return "基础模块权限";
    }

    @Override
    public String getAuthIntroduction() {
        return "查看基础功能";
    }

    @Override
    public String getAuthGroup() {
        return "framework";
    }

    @Override
    public Integer sort() {
        return 1;
    }

}
