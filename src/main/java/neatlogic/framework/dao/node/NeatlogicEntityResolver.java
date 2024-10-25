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

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import java.io.InputStream;

public class NeatlogicEntityResolver implements EntityResolver {
    @Override
    public InputSource resolveEntity(String publicId, String systemId) {
        // 检查 systemId 是否指向你的 DTD
        if (systemId.contains("mybatis_custom.dtd")) {
            // 使用类加载器加载 DTD 文件
            InputStream dtdStream = getClass().getClassLoader()
                    .getResourceAsStream("neatlogic/framework/dao/node/neatlogic_mybatis.dtd");
            return new InputSource(dtdStream);
        }
        // 如果不是目标 DTD，则返回 null，让解析器继续处理
        return null;
    }
}