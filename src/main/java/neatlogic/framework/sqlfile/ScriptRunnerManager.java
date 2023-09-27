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

package neatlogic.framework.sqlfile;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.dao.mapper.TenantMapper;
import neatlogic.framework.dto.TenantModuleDmlSqlVo;
import neatlogic.framework.dto.TenantVo;
import neatlogic.framework.store.mysql.DatasourceManager;
import neatlogic.framework.store.mysql.NeatLogicBasicDataSource;
import neatlogic.framework.util.I18nUtils;
import neatlogic.framework.util.JdbcUtil;
import neatlogic.framework.util.Md5Util;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class ScriptRunnerManager {
    static Logger logger = LoggerFactory.getLogger(ScriptRunnerManager.class);

    private static TenantMapper tenantMapper;

    @Autowired
    public void setTenantMapper(TenantMapper _tenantMapper) {
        tenantMapper = _tenantMapper;
    }

    /**
     * 执行sql
     *
     * @param tenant       租户
     * @param moduleId     模块id
     * @param scriptReader 脚本读取
     * @param logWriter    日志
     * @param errWriter    错误日志
     */
    public static void runScript(TenantVo tenant, String moduleId, Reader scriptReader, PrintWriter logWriter, PrintWriter errWriter, boolean isDataDb) {
        Connection conn = null;
        String tenantUuid = tenant.getUuid();
        if (isDataDb) {
            tenantUuid = tenantUuid + "_data";
        }
        NeatLogicBasicDataSource tenantDatasource = DatasourceManager.getDatasource(tenantUuid);
        try {
            conn = tenantDatasource.getConnection();
            ScriptRunner runner = new ScriptRunner(conn);
            runner.setSendFullScript(false);
            runner.setAutoCommit(true);
            // 有错误会继续执行
            runner.setStopOnError(false);
            // Resources.setCharset(Charset.forName("UTF-8"));
            runner.setLogWriter(logWriter);
            runner.setErrorLogWriter(errWriter);
            runner.setDelimiter(";");
            runner.runScript(scriptReader);
            runner.closeConnection();
        } catch (SQLException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }
        }
    }

    /**
     * 仅执行一次sql，执行过的sql跳过不执行
     *
     * @param tenant       租户
     * @param moduleId     模块id
     * @param scriptReader 脚本读取
     * @param logWriter    日志
     * @param errWriter    错误日志
     */
    public static void runScriptOnce(TenantVo tenant, String moduleId, Reader scriptReader, PrintWriter logWriter, PrintWriter errWriter) {
        runScriptOnce(tenant, moduleId, scriptReader, logWriter, errWriter, false);
    }

    /**
     * 仅执行一次sql，执行过的sql跳过不执行
     *
     * @param tenant       租户
     * @param moduleId     模块id
     * @param scriptReader 脚本读取
     * @param logWriter    日志
     * @param errWriter    错误日志
     * @param isDataDb     是否data库
     */
    public static void runScriptOnce(TenantVo tenant, String moduleId, Reader scriptReader, PrintWriter logWriter, PrintWriter errWriter, Boolean isDataDb) {
        TenantContext.get().setUseDefaultDatasource(true);
        List<String> hasRunSqlMd5List = tenantMapper.getTenantModuleDmlSqlMd5ByTenantUuidAndModuleId(tenant.getUuid(), moduleId);
        List<String> currentRunSqlMd5List = new ArrayList<>();
        Connection conn = null;
        String tenantUuid = tenant.getUuid();
        if (isDataDb) {
            tenantUuid = tenantUuid + "_data";
        }
        NeatLogicBasicDataSource tenantDatasource = DatasourceManager.getDatasource(tenantUuid);
        try {
            BufferedReader scriptBufferedReader = new BufferedReader(scriptReader);
            conn = tenantDatasource.getConnection();
            ScriptRunner runner = new ScriptRunner(conn);
            runner.setLogWriter(logWriter);
            runner.setErrorLogWriter(errWriter);
            runner.setSendFullScript(false);
            runner.setAutoCommit(true);
            String line;
            while ((line = scriptBufferedReader.readLine()) != null) {
                if (StringUtils.isBlank(line.trim())) {
                    continue;
                }
                // 如果没有执行过改sql，则执行该 SQL 语句
                String sqlMd5 = Md5Util.encryptMD5(line);
                if (!hasRunSqlMd5List.contains(sqlMd5)) {
                    runner.runScript(new StringReader(line));
                    currentRunSqlMd5List.add(sqlMd5);
                    hasRunSqlMd5List.add(sqlMd5);
                }
            }
            runner.closeConnection();
            if (CollectionUtils.isNotEmpty(currentRunSqlMd5List)) {
                List<String> hasRunSqlMd5ListTmp = new ArrayList<>();
                for (int i = 0; i < currentRunSqlMd5List.size(); i++) {
                    if (i != 0 && i % 200 == 0) {
                        tenantMapper.insertTenantModuleDmlSql(tenant.getUuid(), moduleId, hasRunSqlMd5ListTmp, 1);
                        hasRunSqlMd5ListTmp.clear();
                    } else {
                        hasRunSqlMd5ListTmp.add(currentRunSqlMd5List.get(i));
                    }
                }
                if (CollectionUtils.isNotEmpty(hasRunSqlMd5ListTmp)) {
                    tenantMapper.insertTenantModuleDmlSql(tenant.getUuid(), moduleId, hasRunSqlMd5ListTmp, 1);
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }
        }
    }


    /**
     * 仅执行一次sql，执行过的sql跳过不执行
     *
     * @param tenant       租户
     * @param moduleId     模块id
     * @param scriptReader 脚本读取
     * @param isDataDb     是否data库
     */
    public static void runScriptOnceWithJdbc(TenantVo tenant, String moduleId, Reader scriptReader, Boolean isDataDb, String type) {
        StringWriter logStrWriter = new StringWriter();
        PrintWriter logWriter = new PrintWriter(logStrWriter);
        StringWriter errStrWriter = new StringWriter();
        PrintWriter errWriter = new PrintWriter(errStrWriter);
        PreparedStatement sqlMd5Statement = null;
        ResultSet sqlMd5ResultSet = null;
        Connection conn = null;
        Connection neatlogicConn = null;
        ScriptRunner runner = null;
        try {
            List<String> hasRunSqlMd5List = new ArrayList<>();
            DataSource neatlogicDatasource = JdbcUtil.getNeatlogicDataSource();
            neatlogicConn = neatlogicDatasource.getConnection();
            sqlMd5Statement = neatlogicConn.prepareStatement("select sql_uuid from tenant_module_dmlsql where tenant_uuid = ? and `module_id` = ? and `sql_status` = 1");
            sqlMd5Statement.setString(1, tenant.getUuid());
            sqlMd5Statement.setString(2, moduleId);
            sqlMd5ResultSet = sqlMd5Statement.executeQuery();
            while (sqlMd5ResultSet.next()) {
                hasRunSqlMd5List.add(sqlMd5ResultSet.getString("sql_uuid"));
            }
            BufferedReader scriptBufferedReader = new BufferedReader(scriptReader);
            try {
                conn = JdbcUtil.getNeatlogicDataSource(tenant, isDataDb).getConnection();
            } catch (Exception exception) {
                System.out.println(I18nUtils.getStaticMessage("nfs.scriptrunnermanager.runscriptoncewithjdbc.tenantnotconnect", tenant.getUuid()));
                System.exit(1);
            }
            runner = new ScriptRunner(conn);
            runner.setLogWriter(logWriter);
            runner.setErrorLogWriter(errWriter);
            runner.setSendFullScript(false);
            runner.setAutoCommit(true);
            String line;
            while ((line = scriptBufferedReader.readLine()) != null) {
                if (StringUtils.isBlank(line.trim())) {
                    continue;
                }
                // 如果没有执行过该sql，则执行
                String sqlMd5 = Md5Util.encryptMD5(line);
                if (!hasRunSqlMd5List.contains(sqlMd5)) {
                    runner.runScript(new StringReader(line));
                    TenantModuleDmlSqlVo tenantModuleDmlSqlVo;
                    if (StringUtils.isNotBlank(errStrWriter.toString())) {
                        String error = "  ✖" + tenant.getName() + "·" + moduleId+"."+type+": "+line;
                        //logger.error(error);
                        System.out.println(error);
                        System.exit(1);
                        tenantModuleDmlSqlVo = new TenantModuleDmlSqlVo(tenant.getUuid(), moduleId, sqlMd5, 0, errStrWriter.toString(), type);
                        errStrWriter.getBuffer().setLength(0);
                    } else {
                        tenantModuleDmlSqlVo = new TenantModuleDmlSqlVo(tenant.getUuid(), moduleId, sqlMd5, type);
                        hasRunSqlMd5List.add(sqlMd5);
                    }
                    insertTenantModuleDmlSql(tenantModuleDmlSqlVo, neatlogicConn);
                    insertTenantModuleDmlSqlDetail(sqlMd5, line, neatlogicConn);
                }
            }
        } catch (Exception ex) {
            logger.error(String.format("租户%s初始化dml失败", tenant.getName()));
            logger.error(ex.getMessage(), ex);
        } finally {
            if (runner != null) {
                runner.closeConnection();
            }
            JdbcUtil.closeConnection(conn);
            JdbcUtil.closeConnection(neatlogicConn);
            JdbcUtil.closeResultSet(sqlMd5ResultSet);
            JdbcUtil.closeStatement(sqlMd5Statement);
        }
    }


    /**
     * 执行sql
     *
     * @param tenant       租户
     * @param moduleId     模块id
     * @param scriptReader 脚本读取
     * @param logWriter    日志
     * @param errWriter    错误日志
     */
    public static void runScriptWithJdbc(TenantVo tenant, String moduleId, Reader scriptReader, PrintWriter logWriter, PrintWriter errWriter, boolean isDataDb) {
        Connection conn = null;
        try {
            conn = JdbcUtil.getNeatlogicDataSource(tenant, isDataDb).getConnection();
            ScriptRunner runner = new ScriptRunner(conn);
            runner.setSendFullScript(false);
            runner.setAutoCommit(true);
            // 有错误会继续执行
            runner.setStopOnError(false);
            // Resources.setCharset(Charset.forName("UTF-8"));
            runner.setLogWriter(logWriter);
            runner.setErrorLogWriter(errWriter);
            runner.setDelimiter(";");
            runner.runScript(scriptReader);
            runner.closeConnection();
        } catch (SQLException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }
        }
    }

    /**
     * 记录租户dml sql执行记录
     *
     * @param tenantModuleDmlSqlVo dml sql对象
     * @param neatlogicConn        连接neatlogic库 connection
     */
    private static void insertTenantModuleDmlSql(TenantModuleDmlSqlVo tenantModuleDmlSqlVo, Connection neatlogicConn) {
        PreparedStatement statement = null;
        try {
            String sql = "insert into `tenant_module_dmlsql` (`tenant_uuid`,`module_id`,`sql_uuid`,`sql_status`,`error_msg`,`fcd`,`type`) VALUES (?,?,?,?,?,now(),?) ON DUPLICATE KEY UPDATE `sql_status` = ? , `error_msg` = ?";
            statement = neatlogicConn.prepareStatement(sql);
            statement.setString(1, tenantModuleDmlSqlVo.getTenantUuid());
            statement.setString(2, tenantModuleDmlSqlVo.getModuleId());
            statement.setString(3, tenantModuleDmlSqlVo.getSqlMd5());
            statement.setInt(4, tenantModuleDmlSqlVo.getSqlStatus());
            statement.setString(5, tenantModuleDmlSqlVo.getErrorMsg());
            statement.setString(6, tenantModuleDmlSqlVo.getType());
            statement.setInt(7, tenantModuleDmlSqlVo.getSqlStatus());
            statement.setString(8, tenantModuleDmlSqlVo.getErrorMsg());
            statement.execute();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new RuntimeException(ex.getMessage(), ex);
        } finally {
            JdbcUtil.closeStatement(statement);
        }
    }

    /**
     * 记录租户dml sql语句
     *
     * @param md5           sql的哈希唯一值
     * @param dmlSql        sql语句
     * @param neatlogicConn 连接neatlogic库 connection
     */
    private static void insertTenantModuleDmlSqlDetail(String md5, String dmlSql, Connection neatlogicConn) {
        PreparedStatement statement = null;
        try {
            String sql = "insert ignore into `tenant_module_dmlsql_detail` (`sql_uuid`,`content`) VALUES (?,?) ";
            statement = neatlogicConn.prepareStatement(sql);
            statement.setString(1, md5);
            statement.setString(2, dmlSql);
            statement.execute();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new RuntimeException(ex.getMessage(), ex);
        } finally {
            JdbcUtil.closeStatement(statement);
        }
    }

    /**
     * 清楚异常dml sql记录
     *
     * @param tenantUuid    租户
     * @param moduleId      模块
     * @param neatlogicConn 连接neatlogic库 connection
     */
    private static void clearTenantModuleDmlSqlError(String tenantUuid, String moduleId, Connection neatlogicConn) {
        PreparedStatement statement = null;
        try {
            String sql = "delete from `tenant_module_dmlsql` where `tenant_uuid` = ? and `module_id` = ? and `sql_status` = 0 ";
            statement = neatlogicConn.prepareStatement(sql);
            statement.setString(1, tenantUuid);
            statement.setString(2, moduleId);
            statement.execute();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new RuntimeException(ex.getMessage(), ex);
        } finally {
            JdbcUtil.closeStatement(statement);
        }
    }
}
