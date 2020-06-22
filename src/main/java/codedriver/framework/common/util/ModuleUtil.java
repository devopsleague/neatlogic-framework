package codedriver.framework.common.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import codedriver.framework.dto.ModuleGroupVo;
import codedriver.framework.dto.ModuleVo;

public class ModuleUtil {
	static Logger logger = LoggerFactory.getLogger(ModuleUtil.class);
	private static List<ModuleVo> moduleList = new ArrayList<>();
	private static Map<String, ModuleVo> moduleMap = new HashMap<>();

	public static List<ModuleVo> getAllModuleList() {
		return moduleList;
	}

	public static List<ModuleGroupVo> getAllModuleGroupList() {
		List<ModuleGroupVo> moduleGroupList = new ArrayList<>();
		Set<String> groupSet = new HashSet<>();
		for (ModuleVo moduleVo : moduleList) {
			// master模块不允许用户添加,framework模块必选
			if (!moduleVo.getId().equals("master") && !moduleVo.getGroup().equals("framework") && !groupSet.contains(moduleVo.getGroup())) {
				groupSet.add(moduleVo.getGroup());
				ModuleGroupVo moduleGroup = new ModuleGroupVo();
				moduleGroup.setGroup(moduleVo.getGroup());
				moduleGroup.setGroupName(moduleVo.getGroupName());
				moduleGroup.setGroupDescription(moduleVo.getGroupDescription());
				moduleGroup.setGroupSort(moduleVo.getGroupSort());
				moduleGroupList.add(moduleGroup);
			}
		}
		moduleGroupList.sort(new Comparator<ModuleGroupVo>() {
			@Override
			public int compare(ModuleGroupVo o1, ModuleGroupVo o2) {
				return o1.getGroupSort() - o2.getGroupSort();
			}

		});
		return moduleGroupList;
	}

	public static ModuleVo getModuleById(String moduleId) {
		return moduleMap.get(moduleId);
	}

	public static void addModule(ModuleVo moduleVo) {
		moduleMap.put(moduleVo.getId(), moduleVo);
		moduleList.add(moduleVo);
	}

	public static List<ModuleVo> getTenantActiveModuleList(List<String> tenantModuleGroupList) {
		List<ModuleVo> returnList = new ArrayList<>();
		if (!tenantModuleGroupList.contains("framework")) {
			tenantModuleGroupList.add("framework");
		}

		for (ModuleVo moduleVo : moduleList) {
			if (tenantModuleGroupList.contains(moduleVo.getGroup())) {
				returnList.add(moduleVo);
			}
		}

		return returnList;
	}

	public static ModuleGroupVo getModuleGroup(String group) {
		ModuleGroupVo moduleGroup = null;
		Set<Entry<String, ModuleVo>> moduleSet = moduleMap.entrySet();
		for (Entry<String, ModuleVo> moduleEntry : moduleSet) {
			ModuleVo moduleVo = moduleEntry.getValue();
			if (moduleVo.getGroup().equals(group)) {
				if (moduleGroup == null) {
					moduleGroup = new ModuleGroupVo();
					moduleGroup.setGroup(moduleVo.getGroup());
					moduleGroup.setGroupName(moduleVo.getGroupName());
					moduleGroup.setGroupDescription(moduleVo.getGroupDescription());
					moduleGroup.setGroupSort(moduleVo.getGroupSort());
					List<ModuleVo> moduleList = new ArrayList<ModuleVo>();
					moduleList.add(moduleVo);
					moduleGroup.setModuleList(moduleList);
				} else {
					moduleGroup.getModuleList().add(moduleVo);
				}
			}
		}
		return moduleGroup;
	}

	public static Map<String, ModuleGroupVo> getModuleGroupMap() {
		Map<String, ModuleGroupVo> moduleGroupMap = new HashMap<String, ModuleGroupVo>();
		Set<Entry<String, ModuleVo>> moduleSet = moduleMap.entrySet();
		for (Entry<String, ModuleVo> moduleEntry : moduleSet) {
			ModuleVo moduleVo = moduleEntry.getValue();
			ModuleGroupVo moduleGroupVo = null;
			if (moduleGroupMap.containsKey(moduleVo.getGroup())) {
				moduleGroupVo = (ModuleGroupVo) moduleGroupMap.get(moduleVo.getGroup());
				moduleGroupVo.getModuleList().add(moduleVo);
			} else {
				moduleGroupVo = new ModuleGroupVo();
				moduleGroupVo.setGroup(moduleVo.getGroup());
				moduleGroupVo.setGroupName(moduleVo.getGroupName());
				moduleGroupVo.setGroupDescription(moduleVo.getGroupDescription());
				moduleGroupVo.setGroupSort(moduleVo.getGroupSort());
				List<ModuleVo> moduleList = new ArrayList<ModuleVo>();
				moduleList.add(moduleVo);
				moduleGroupVo.setModuleList(moduleList);
			}
			moduleGroupMap.put(moduleGroupVo.getGroup(), moduleGroupVo);

		}
		return moduleGroupMap;
	}
}
