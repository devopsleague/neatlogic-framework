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

package neatlogic.framework.dao.node;

import neatlogic.framework.asynchronization.threadlocal.LicensePolicyContext;
import org.apache.commons.collections4.MapUtils;
import org.apache.ibatis.scripting.xmltags.DynamicContext;
import org.apache.ibatis.scripting.xmltags.SqlNode;

public class LicensePolicySqlNode implements SqlNode {
    private final String licenseType;
    private final String column;

    public LicensePolicySqlNode(String licenseType, String column) {
        this.licenseType = licenseType;
        this.column = column;
    }

    @Override
    public boolean apply(DynamicContext context) {
        if (LicensePolicyContext.get() != null && MapUtils.isNotEmpty(LicensePolicyContext.get().getSqlPolicyValue()) && LicensePolicyContext.get().getSqlPolicyValue().get(licenseType) != null) {
            context.appendSql(String.format("and %s <= %d", column, LicensePolicyContext.get().getSqlPolicyValue().get(licenseType)));
        }
        return true;
    }
}
