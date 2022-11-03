/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.framework.datawarehouse.service;

import codedriver.framework.datawarehouse.dto.DataSourceVo;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;

public interface DataSourceService {
    void deleteReportDataSource(DataSourceVo reportDataSourceVo);

    @Transactional
    void executeReportDataSource(DataSourceVo dataSourceVo) throws SQLException;

    /**
     * 新增数据源
     *
     * @param vo 数据源vo
     */
    void insertDataSource(DataSourceVo vo);

    /**
     * 更新数据源
     *
     * @param newVo        新数据源vo
     * @param newXmlConfig 新数据源xml配置
     * @param oldVo        旧数据源vo
     */
    void updateDataSource(DataSourceVo newVo, DataSourceVo newXmlConfig, DataSourceVo oldVo);

    /**
     * 创建数据源动态表
     *
     * @param dataSourceVo 数据源vo
     */
    void createDataSourceTSchema(DataSourceVo dataSourceVo);

    /**
     * 加载或卸载报表数据源数据同步定时作业
     *
     * @param dataSourceVo 数据源vo
     */
    void loadOrUnloadReportDataSourceJob(DataSourceVo dataSourceVo);
}
