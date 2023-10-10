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

package neatlogic.framework.bootstrap;

import neatlogic.framework.asynchronization.thread.ModuleInitApplicationListener;
import neatlogic.framework.common.config.Config;
import neatlogic.framework.common.util.ModuleUtil;
import neatlogic.framework.dto.DatasourceVo;
import neatlogic.framework.dto.TenantVo;
import neatlogic.framework.dto.module.ModuleVo;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.sqlfile.ScriptRunnerManager;
import neatlogic.framework.util.I18nUtils;
import neatlogic.framework.util.JdbcUtil;
import neatlogic.framework.util.TimeUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.web.WebApplicationInitializer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

public class ModuleInitializer implements WebApplicationInitializer {
    static Logger logger = LoggerFactory.getLogger(ModuleInitializer.class);

    @Override
    public void onStartup(ServletContext context) throws ServletException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        System.out.println("    _   __              __   __                  _          _____    ____ \n" + "   / | / /___   ____ _ / /_ / /   ____   ____ _ (_)_____   |__  /   / __ \\\n" + "  /  |/ // _ \\ / __ `// __// /   / __ \\ / __ `// // ___/    /_ <   / / / /\n" + " / /|  //  __// /_/ // /_ / /___/ /_/ // /_/ // // /__    ___/ /_ / /_/ / \n" + "/_/ |_/ \\___/ \\__,_/ \\__//_____/\\____/ \\__, //_/ \\___/   /____/(_)\\____/  \n" + "                                      /____/                             \n" + "===========================================================================");
        ModuleVo module = null;
        try {
            List<ModuleVo> moduleListFromServletContext = getModuleListByServletContext(resolver);
            List<TenantVo> activeTenantList = getAllTenantList();
            initDmlSql(resolver, activeTenantList, moduleListFromServletContext);
            updateChangeLogVersion(resolver, activeTenantList, moduleListFromServletContext);
            System.out.println("⚡" + I18nUtils.getStaticMessage("common.startloadmodule"));
            for (ModuleVo moduleFromServletContext : moduleListFromServletContext) {
                module = moduleFromServletContext;
                NeatLogicWebApplicationContext appContext = new NeatLogicWebApplicationContext();
                appContext.setConfigLocation("classpath*:" + module.getPath());
                appContext.setId(module.getId());
                appContext.setModuleId(module.getId());
                appContext.setModuleName(module.getNameWithoutTranslate());
                appContext.setGroupName(module.getGroupNameWithoutTranslate());
                appContext.setGroup(module.getGroup());
                ModuleUtil.addModule(module);
                ServletRegistration.Dynamic sr = context.addServlet(module.getId() + "[" + I18nUtils.getStaticMessage(module.getNameWithoutTranslate()) + "] " + module.getVersion(), new NeatLogicDispatcherServlet(module, appContext));
                if (StringUtils.isNotBlank(module.getUrlMapping())) {
                    sr.addMapping(module.getUrlMapping());
                }
                /* 模块加载开始，计数器加一 **/
                ModuleInitApplicationListener.getModuleinitphaser().register();
                if (module.getId().equalsIgnoreCase("framework")) {
                    sr.addMapping("/");
                    sr.setLoadOnStartup(1);
                } else {
                    sr.setLoadOnStartup(2);
                }
                System.out.println("  ✓" + module.getId() + "·" + I18nUtils.getStaticMessage(module.getNameWithoutTranslate()));
            }
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
            System.exit(1);
        } catch (Exception ex) {
            if (ModuleInitApplicationListener.getModuleinitphaser().getRegisteredParties() > 0) {
                ModuleInitApplicationListener.getModuleinitphaser().arriveAndDeregister();
            }
            logger.error(ex.getMessage(), ex);
        } finally {
            if (module != null) {
                System.out.println("  ✖" + module.getId() + "·" + I18nUtils.getStaticMessage(module.getNameWithoutTranslate()));
            }
        }
    }

    /**
     * 根据每个模块的servlet context 获取模块列表
     */
    private List<ModuleVo> getModuleListByServletContext(ResourcePatternResolver resolver) throws Exception {
        String moduleId = null;
        String moduleName = null;
        List<ModuleVo> moduleVoList = new ArrayList<>();
        Resource[] resources = resolver.getResources("classpath*:neatlogic/**/*-servlet-context.xml");
        for (Resource resource : resources) {
            String path = resource.getURL().getPath();
            path = path.substring(path.indexOf("!") + 1);
            SAXReader reader = new SAXReader();
            Document document = reader.read(resource.getURL());
            Element rootE = document.getRootElement();
            Element neatlogicE = rootE.element("module");
            // Element nameE = rootE.element("name");
            String urlMapping, moduleDescription, version, group, groupName, groupSort, groupDescription;
            moduleId = neatlogicE.attributeValue("id");
            moduleName = neatlogicE.attributeValue("name");
            urlMapping = neatlogicE.attributeValue("urlMapping");
            moduleDescription = neatlogicE.attributeValue("description");
            group = neatlogicE.attributeValue("group");
            groupName = neatlogicE.attributeValue("groupName");
            groupSort = neatlogicE.attributeValue("groupSort");
            groupDescription = neatlogicE.attributeValue("groupDescription");
            version = Config.getProperty("META-INF/maven/com.neatlogic/neatlogic-" + moduleId + "/pom.properties", "version");
            moduleVoList.add(new ModuleVo(moduleId, moduleName, urlMapping, moduleDescription, version, group, groupName, groupSort, groupDescription, path));
        }
        return moduleVoList;
    }

    /**
     * 执行模块的dml
     */
    private void initDmlSql(ResourcePatternResolver resolver, List<TenantVo> activeTenantList, List<ModuleVo> moduleVoList) throws Exception {
        System.out.printf("⚡" + (I18nUtils.getStaticMessage("nfb.moduleinitializer.initdmlsql.tenant")) + "%n");
        for (TenantVo tenantVo : activeTenantList) {
            for (ModuleVo moduleVo : moduleVoList) {
                Resource[] dmlResources = resolver.getResources("classpath*:neatlogic/resources/" + moduleVo.getId() + "/sqlscript/dml.sql");
                if (dmlResources.length == 1) {
                    Resource dmlResource = dmlResources[0];
                    Reader scriptReader = new InputStreamReader(dmlResource.getInputStream());
                    ScriptRunnerManager.runScriptOnceWithJdbc(tenantVo, moduleVo.getId(), scriptReader, false, "sqlscript·dml");
                }
            }
            System.out.println("  ✓" + tenantVo.getName());
        }
    }

    /**
     * 从数据库查询所有激活租户
     *
     * @return 激活的租户
     */
    private List<TenantVo> getAllTenantList() throws Exception {
        List<TenantVo> activeTenantList = new ArrayList<>();
        Connection connection = null;
        PreparedStatement tenantStatement = null;
        ResultSet tenantResultSet = null;
        try {
            DataSource datasource = JdbcUtil.getNeatlogicDataSource();
            try {
                connection = datasource.getConnection();
            } catch (Exception exception) {
                throw new ApiRuntimeException("ERROR: " + I18nUtils.getStaticMessage("nfb.moduleinitializer.getactivetenantlist.neatlogicdb"));
            }
            String tenantSql = "SELECT a.*,b.* FROM tenant a left join datasource b on a.uuid = b.tenant_uuid";
            tenantStatement = connection.prepareStatement(tenantSql);
            tenantResultSet = tenantStatement.executeQuery();
            while (tenantResultSet.next()) {
                TenantVo tenantVo = new TenantVo();
                tenantVo.setUuid(tenantResultSet.getString("uuid"));
                tenantVo.setName(tenantResultSet.getString("name"));
                DatasourceVo datasourceVo = new DatasourceVo();
                datasourceVo.setUrl(tenantResultSet.getString("url"));
                datasourceVo.setUsername(tenantResultSet.getString("username"));
                datasourceVo.setPasswordCipher(tenantResultSet.getString("password"));
                datasourceVo.setDriver(tenantResultSet.getString("driver"));
                datasourceVo.setHost(tenantResultSet.getString("host"));
                datasourceVo.setPort(tenantResultSet.getInt("port"));
                tenantVo.setDatasource(datasourceVo);
                activeTenantList.add(tenantVo);
            }
        } catch (Exception ex) {
            throw new Exception(ex);
        } finally {
            JdbcUtil.closeConnection(connection);
            JdbcUtil.closeStatement(tenantStatement);
            JdbcUtil.closeResultSet(tenantResultSet);
        }
        return activeTenantList;
    }

    /**
     * 获取激活租户的对应模块版本
     *
     * @param activeTenantList 激活的租户
     * @return 激活租户对应的模块版本
     */
    private Map<String, Map<String, String>> getTenantModuleVersionMap(List<TenantVo> activeTenantList) throws Exception {
        Connection connection = null;
        PreparedStatement tenantGroupStatement = null;
        ResultSet tenantGroupResultSet = null;
        Map<String, Map<String, String>> tenantModuleGroupMap = new HashMap<>();
        try {
            DataSource datasource = JdbcUtil.getNeatlogicDataSource();
            try {
                connection = datasource.getConnection();
            } catch (Exception exception) {
                throw new ApiRuntimeException("ERROR: " + I18nUtils.getStaticMessage("nfb.moduleinitializer.getactivetenantlist.neatlogicdb"));
            }
            List<String> activeTenantUuidList = activeTenantList.stream().map(TenantVo::getUuid).collect(Collectors.toList());
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < activeTenantUuidList.size(); i++) {
                placeholders.append("?");
                if (i < activeTenantUuidList.size() - 1) {
                    placeholders.append(",");
                }
            }
            String tenantGroupSql = "SELECT a.* FROM tenant_module a where a.tenant_uuid in (" + placeholders + ") ";
            tenantGroupStatement = connection.prepareStatement(tenantGroupSql);
            for (int i = 1; i <= activeTenantUuidList.size(); i++) {
                tenantGroupStatement.setString(i, activeTenantUuidList.get(i - 1));
            }
            tenantGroupResultSet = tenantGroupStatement.executeQuery();
            while (tenantGroupResultSet.next()) {
                tenantModuleGroupMap.computeIfAbsent(tenantGroupResultSet.getString("tenant_uuid"), k -> new HashMap<>()).put(tenantGroupResultSet.getString("module_id"), tenantGroupResultSet.getString("version"));
            }
        } catch (Exception ex) {
            throw new Exception(ex);
        } finally {
            JdbcUtil.closeConnection(connection);
            JdbcUtil.closeStatement(tenantGroupStatement);
            JdbcUtil.closeResultSet(tenantGroupResultSet);
        }
        return tenantModuleGroupMap;
    }

    /**
     * 检查并更新数据库ddl和dml版本
     */
    private void updateChangeLogVersion(ResourcePatternResolver resolver, List<TenantVo> activeTenantList, List<ModuleVo> moduleVoList) throws Exception {
        System.out.println("⚡" + "开始初始化版本更新...");
        List<String> errorList = new ArrayList<>();
        Map<String, List<String>> moduleVersionListMap = new HashMap<>();
        //检查changelog下合法版本
        Resource[] resources = resolver.getResources("classpath*:neatlogic/resources/*/changelog/*/");
        for (Resource resource : resources) {
            //目前
            String fileName = resource.getURL().getPath().substring(0, resource.getURL().getPath().lastIndexOf("/"));
            String version = fileName.substring(fileName.lastIndexOf("/") + 1);
            if (!Objects.equals("changelog", version) && StringUtils.isNotBlank(version)) {
                String path = resource.getURL().getPath();
                path = path.substring(path.indexOf("!") + 1);
                if (StringUtils.isNotBlank(path)) {
                    String moduleId = path.split("/")[3];
                    if (version.matches("\\d{4}-\\d{2}-\\d{2}(-\\d{2})?")) {
                        moduleVersionListMap.computeIfAbsent(moduleId, k -> new ArrayList<>()).add(version);
                    } else {
                        errorList.add(path);
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(errorList)) {
            for (String path : errorList) {
                System.out.println(I18nUtils.getStaticMessage("nfb.moduleinitializer.checkchangelog.invalid", path, TimeUtil.YYYY_MM_DD));
            }
            System.exit(1);
        }
        updateNeatlogicDatabase(resolver);
        updateTenantDatabase(resolver, moduleVersionListMap, activeTenantList, moduleVoList);
    }

    /**
     * 更新neatlogic库
     *
     * @param resolver Strategy interface for resolving a location pattern (for example, an Ant-style path pattern) into Resource objects.
     */
    private void updateNeatlogicDatabase(ResourcePatternResolver resolver) throws Exception {
        String currentVersion = getNeatlogicVersion();
        //如果版本为空,说明第一次建立基线，需要手动比对更新数据库后，再重启服务
        if (StringUtils.isBlank(currentVersion)) {
            currentVersion = TimeUtil.descDateStr(new Date(), 1, TimeUtil.YYYY_MM_DD);
            insertNeatLogicVersion(currentVersion);
        } else {
            List<String> versionList = new ArrayList<>();
            Resource[] resources = resolver.getResources("classpath*:neatlogic/resources/framework/changelog/*/neatlogic.sql");
            for (Resource resource : resources) {
                //目前仅支持dll
                String fileName = resource.getURL().getPath().substring(0, resource.getURL().getPath().lastIndexOf("/"));
                String version = fileName.substring(fileName.lastIndexOf("/") + 1);
                int versionTmp = Integer.parseInt((version.replace("-", StringUtils.EMPTY) + "00").substring(0, 10));
                int currentVersionTmp = Integer.parseInt((currentVersion.replace("-", StringUtils.EMPTY) + "00").substring(0, 10));
                if (versionTmp > currentVersionTmp) {
                    versionList.add(version);
                }
            }
            // 定义正序比较器
            Comparator<String> fileNameComparator = Comparator.naturalOrder();
            versionList.sort(fileNameComparator);
            for (String version : versionList) {
                Resource[] resourceDlls = resolver.getResources("classpath*:neatlogic/resources/framework/changelog/" + version + "/neatlogic.sql");
                Reader scriptReader = new InputStreamReader(resourceDlls[0].getInputStream());
                ScriptRunnerManager.runScriptWithJdbc(scriptReader, version, JdbcUtil.getNeatlogicDataSource(), "neatlogic.sql");
                updateNeatLogicVersion(currentVersion, version);
                currentVersion = version;
            }
        }
    }

    /**
     * 从数据库查询所有激活租户
     *
     * @return 激活的租户
     */
    private String getNeatlogicVersion() throws Exception {
        Connection connection = null;
        PreparedStatement versionStatement = null;
        ResultSet versionResultSet = null;
        try {
            DataSource datasource = JdbcUtil.getNeatlogicDataSource();
            try {
                connection = datasource.getConnection();
            } catch (Exception exception) {
                throw new ApiRuntimeException("ERROR: " + I18nUtils.getStaticMessage("nfb.moduleinitializer.getactivetenantlist.neatlogicdb"));
            }
            String versionSql = "SELECT * FROM version limit 1";
            versionStatement = connection.prepareStatement(versionSql);
            versionResultSet = versionStatement.executeQuery();
            if (versionResultSet.next()) {
                return versionResultSet.getString("version");
            }
        } catch (Exception ex) {
            throw new Exception(ex);
        } finally {
            JdbcUtil.closeConnection(connection);
            JdbcUtil.closeStatement(versionStatement);
            JdbcUtil.closeResultSet(versionResultSet);
        }
        return null;
    }


    /**
     * 更新租户库
     *
     * @param resolver             Strategy interface for resolving a location pattern (for example, an Ant-style path pattern) into Resource objects.
     * @param moduleVersionListMap 模块类路径定义的版本
     * @param allTenantList        所有的租户
     * @param moduleVoList         所有来自context文件定义的模块
     * @throws Exception 异常
     */
    private void updateTenantDatabase(ResourcePatternResolver resolver, Map<String, List<String>> moduleVersionListMap, List<TenantVo> allTenantList, List<ModuleVo> moduleVoList) throws Exception {
        // 定义倒序比较器
        Comparator<String> fileNameComparatorReversed = Comparator.reverseOrder();
        // 定义正序比较器
        Comparator<String> fileNameComparator = Comparator.naturalOrder();
        //获取最新版本
        Map<String, String> moduleLatestVersionMap = new HashMap<>();
        //获取模块最新版本
        if (MapUtils.isNotEmpty(moduleVersionListMap)) {
            for (Map.Entry<String, List<String>> entry : moduleVersionListMap.entrySet()) {
                entry.getValue().sort(fileNameComparatorReversed);
                moduleLatestVersionMap.put(entry.getKey(), entry.getValue().get(0));
            }
        }

        Map<String, Map<String, String>> tenantModuleVersionMap = getTenantModuleVersionMap(allTenantList);
        //循环需要执行的所有模块当前版本
        for (TenantVo tenant : allTenantList) {
            Map<String, String> moduleVersionMap = tenantModuleVersionMap.get(tenant.getUuid());
            if (tenantModuleVersionMap.containsKey(tenant.getUuid())) {
                for (ModuleVo moduleVo : moduleVoList) {
                    String moduleId = moduleVo.getId();
                    String latestVersion = moduleLatestVersionMap.get(moduleId);
                    //第一次启用基线。 即该租户该模块没有版本基线，则直接更新版本基线，不执行sql，启动服务后需要手动更新对比schema后重启tomcat实例服务
                    if (!moduleVersionMap.containsKey(moduleId) || StringUtils.isBlank(moduleVersionMap.get(moduleId)) || moduleVersionMap.get(moduleId) == null) {
                        if (StringUtils.isBlank(latestVersion)) {
                            //其他没有最新版本日志的模块则选择昨天作为最新版本，为了后续自动更新版本
                            latestVersion = TimeUtil.descDateStr(new Date(), 1, TimeUtil.YYYY_MM_DD);
                        }
                        insertTenantModuleVersionSql(tenant.getUuid(), moduleId, latestVersion);
                        //如果模块版本小于最新版本，则执行sql并更新为最新版本
                    } else {
                        //循环执行所有
                        List<String> versionList = moduleVersionListMap.get(moduleId);
                        if (CollectionUtils.isNotEmpty(versionList)) {
                            versionList.sort(fileNameComparator);
                            for (String version : versionList) {
                                int versionTmp = Integer.parseInt((version.replace("-", StringUtils.EMPTY) + "00").substring(0, 10));
                                int currentVersionTmp = Integer.parseInt((moduleVersionMap.get(moduleId).replace("-", StringUtils.EMPTY) + "00").substring(0, 10));
                                if (versionTmp > currentVersionTmp) {
                                    Resource[] ddlResources = resolver.getResources("classpath*:neatlogic/resources/" + moduleId + "/changelog/" + version + "/neatlogic_tenant.sql");
                                    if (ddlResources.length == 1) {
                                        Resource ddlResource = ddlResources[0];
                                        Reader scriptReader = new InputStreamReader(ddlResource.getInputStream());
                                        ScriptRunnerManager.runScriptWithJdbc(tenant, moduleId, scriptReader, version, JdbcUtil.getNeatlogicDataSource(tenant, false), "neatlogic_tenant.sql");
                                    }
                                    /*Resource[] dmlResources = resolver.getResources("classpath*:neatlogic/resources/" + moduleId + "/changelog/" + version + "/tenant_dml.sql");
                                    if (dmlResources.length == 1) {
                                        Resource dmlResource = dmlResources[0];
                                        Reader scriptReader = new InputStreamReader(dmlResource.getInputStream());
                                        ScriptRunnerManager.runScriptOnceWithJdbc(tenant, moduleId, scriptReader, false, "changelog·" + version + "·tenant_dml");
                                    }*/
                                    insertTenantModuleVersionSql(tenant.getUuid(), moduleId, version);
                                    System.out.println("  ✓" + tenant.getName() + "·" + moduleId);
                                }
                            }
                        }
                    }
                }
            } else {
                //第一次启用基线。 即该租户所有模块没有版本基线，则直接更新版本基线，不执行sql，启动服务后需要手动更新对比schema后重启tomcat实例服务
                for (ModuleVo moduleVo : moduleVoList) {
                    String latestVersion = moduleLatestVersionMap.get(moduleVo.getId());
                    if (StringUtils.isBlank(latestVersion)) {
                        //其他没有最新版本日志的模块则选择昨天作为最新版本，为了后续自动更新版本
                        latestVersion = TimeUtil.descDateStr(new Date(), 1, TimeUtil.YYYY_MM_DD);
                    }
                    insertTenantModuleVersionSql(tenant.getUuid(), moduleVo.getId(), latestVersion);
                }
            }
        }
    }

    /**
     * 插入租户模块信息
     */
    private void insertTenantModuleVersionSql(String tenantUuid, String moduleId, String version) throws Exception {
        PreparedStatement statement = null;
        java.sql.Connection neatlogicConn = null;
        try {
            String sql = "insert into `tenant_module` (`tenant_uuid`,`module_id`,`version`,`fcd`,`lcd`) VALUES (?,?,?,now(),now()) ON DUPLICATE KEY UPDATE version = ?,`lcd` = now()";
            neatlogicConn = JdbcUtil.getNeatlogicDataSource().getConnection();
            statement = neatlogicConn.prepareStatement(sql);
            statement.setString(1, tenantUuid);
            statement.setString(2, moduleId);
            statement.setString(3, version);
            statement.setString(4, version);
            statement.execute();
        } catch (Exception ex) {
            throw new Exception(ex);
        } finally {
            JdbcUtil.closeConnection(neatlogicConn);
            JdbcUtil.closeStatement(statement);
        }
    }

    /**
     * 插入neatlogic版本
     */
    private void insertNeatLogicVersion(String version) throws Exception {
        PreparedStatement statement = null;
        java.sql.Connection neatlogicConn = null;
        try {
            String sql = "insert into `version` (`version`,`fcd`,`lcd`) VALUES (?,now(),now())";
            neatlogicConn = JdbcUtil.getNeatlogicDataSource().getConnection();
            statement = neatlogicConn.prepareStatement(sql);
            statement.setString(1, version);
            statement.execute();
        } catch (Exception ex) {
            throw new Exception(ex);
        } finally {
            JdbcUtil.closeConnection(neatlogicConn);
            JdbcUtil.closeStatement(statement);
        }
    }

    /**
     * 更新neatlogic版本
     */
    private void updateNeatLogicVersion(String oldVersion, String version) throws Exception {
        PreparedStatement statement = null;
        java.sql.Connection neatlogicConn = null;
        try {
            String sql = "update version set version = ?,lcd = now() where version = ?";
            neatlogicConn = JdbcUtil.getNeatlogicDataSource().getConnection();
            statement = neatlogicConn.prepareStatement(sql);
            statement.setString(1, version);
            statement.setString(2, oldVersion);
            statement.execute();
        } catch (Exception ex) {
            throw new Exception(ex);
        } finally {
            JdbcUtil.closeConnection(neatlogicConn);
            JdbcUtil.closeStatement(statement);
        }
    }
}
