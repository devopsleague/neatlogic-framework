/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.framework.restful.api;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.module.ModuleVo;
import neatlogic.framework.dto.UserAuthVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class ModuleListApi extends PrivateApiComponentBase {
	@Autowired
	UserMapper userMapper;

	@Override
	public String getToken() {
		return "/module/list";
	}

	@Override
	public String getName() {
		return "获取租户激活模块接口";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({@Param( name = "includeList", type = ApiParamType.JSONARRAY, desc = "白名单")})
	@Output({
		@Param( name = "value", type = ApiParamType.STRING, desc = "模块"),
		@Param( name = "text", type = ApiParamType.STRING, desc = "模块名") 
		})
	@Description(desc = "获取租户激活模块接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		JSONArray resultArray = new JSONArray();
		 Set<String> authGroupSet = new HashSet<String>();
        //获取用户权限
        List<UserAuthVo>  userAuthList = userMapper.searchUserAllAuthByUserAuth(new UserAuthVo(UserContext.get().getUserUuid()));
        for(UserAuthVo userAuth:userAuthList) {
        	authGroupSet.add(userAuth.getAuthGroup());
        }
		Set<String> checkSet = new HashSet<>();
		for (ModuleVo moduleVo : TenantContext.get().getActiveModuleList()) {
			if (authGroupSet.contains(moduleVo.getGroup())&&!checkSet.contains(moduleVo.getGroup())) {
				checkSet.add(moduleVo.getGroup());
				JSONObject returnObj = new JSONObject();
				returnObj.put("value", moduleVo.getGroup());
				returnObj.put("text", moduleVo.getGroupName());
				returnObj.put("sort", moduleVo.getGroupSort());
				resultArray.add(returnObj);
			}
		}
		JSONArray includeList = jsonObj.getJSONArray("includeList");
		if(CollectionUtils.isNotEmpty(includeList)&&includeList.contains("dashboard")) {
			//添加仪表板 TODO 等前端完成迁移，则删除此逻辑
			JSONObject returnObj = new JSONObject();
			returnObj.put("value", "dashboard");
			returnObj.put("text", "仪表板");
			returnObj.put("sort", 0);
			resultArray.add(returnObj);
		}
		resultArray.sort(Comparator.comparing(obj-> ((JSONObject) obj).getInteger("sort")));
		
		return resultArray;
	}
}
