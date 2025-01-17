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
import neatlogic.framework.common.constvalue.license.ILicensePolicy;
import neatlogic.framework.restful.annotation.EntityField;
import neatlogic.framework.util.LicenseUtil;

import java.io.Serializable;

public class LicenseModulePolicyVo implements Serializable {
    @EntityField(name = "common.key", type = ApiParamType.STRING)
    String key;
    @EntityField(name = "common.value", type = ApiParamType.STRING)
    String value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getText() {
        ILicensePolicy licensePolicy = LicenseUtil.licensePolicyMap.get(key);
        if(licensePolicy != null){
            return licensePolicy.getDesc(value);
        }
        return null;
    }
}
