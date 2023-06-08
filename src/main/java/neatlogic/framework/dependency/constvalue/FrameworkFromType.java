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

package neatlogic.framework.dependency.constvalue;

import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.util.$;
import neatlogic.framework.util.I18n;

/**
 * 被引用者（上游）类型
 *
 * @author: linbq
 * @since: 2021/4/2 10:30
 **/
public enum FrameworkFromType implements IFromType {
    MATRIX("matrix", new I18n("矩阵")),
    MATRIXATTR("matrixattr", new I18n("矩阵属性")),
    FORM("form", new I18n("表单")),
    FORMSCENE("formscene", new I18n("表单场景")),
    FORMATTR("formattr", new I18n("表单属性")),
    INTEGRATION("integration", new I18n("集成")),
    CMDBCI("cmdbci", new I18n("cmdb模型")),
    CMDBCIATTR("cmdbciattr", new I18n("cmdb模型属性")),
    WORKTIME("worktime", new I18n("服务窗口")),
    NOTIFY_POLICY("notifypolicy", new I18n("通知策略"));

    private String value;
    private I18n text;

    FrameworkFromType(String value, I18n text) {
        this.value = value;
        this.text = text;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getText() {
        return $.t(text.toString());
    }
}
