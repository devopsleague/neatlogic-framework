/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.framework.datawarehouse.core;

import neatlogic.framework.applicationlistener.core.ModuleInitializedListenerBase;
import neatlogic.framework.bootstrap.NeatLogicWebApplicationContext;
import neatlogic.framework.common.RootComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RootComponent
public class DataSourceServiceHandlerFactory extends ModuleInitializedListenerBase {
    private static final Map<String, IDataSourceServiceHandler> handlerMap = new HashMap<>();
    private static final List<IDataSourceServiceHandler> handlerList = new ArrayList<>();

    public static IDataSourceServiceHandler getHandler(String type) {
        return handlerMap.get(type);
    }

    public static List<IDataSourceServiceHandler> getHandlerList() {
        return handlerList;
    }

    @Override
    public void onInitialized(NeatLogicWebApplicationContext context) {
        Map<String, IDataSourceServiceHandler> myMap = context.getBeansOfType(IDataSourceServiceHandler.class);
        for (Map.Entry<String, IDataSourceServiceHandler> entry : myMap.entrySet()) {
            IDataSourceServiceHandler handler = entry.getValue();
            if (handler.getHandler() != null) {
                if (handlerMap.containsKey(handler.getHandler())) {
                    System.err.println("datasource handler：" + handler.getHandler() + "已存在，请检查代码");
                } else {
                    handlerMap.put(handler.getHandler(), handler);
                    handlerList.add(handler);
                }
            }
        }
    }

    @Override
    protected void myInit() {

    }
}
