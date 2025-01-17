/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.framework.dto.license;

import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.module.ModuleGroupVo;
import neatlogic.framework.restful.annotation.EntityField;

import java.io.Serializable;
import java.util.List;

public class LicenseInvalidVo implements Serializable {
    @EntityField(name = "模块组", type = ApiParamType.JSONARRAY)
    private List<ModuleGroupVo> moduleGroupVos;
    @EntityField(name = "类型", type = ApiParamType.STRING)
    private String type;
    @EntityField(name = "信息", type = ApiParamType.STRING)
    private String msg;

    public LicenseInvalidVo( List<ModuleGroupVo>  moduleGroupVos, String type, String msg) {
        this.moduleGroupVos = moduleGroupVos;
        this.type = type;
        this.msg = msg;
    }

    public List<ModuleGroupVo> getModuleGroupVos() {
        return moduleGroupVos;
    }

    public void setModuleGroupVos(List<ModuleGroupVo> moduleGroupVos) {
        this.moduleGroupVos = moduleGroupVos;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
