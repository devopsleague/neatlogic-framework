/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.framework.datawarehouse.dao.mapper;

import neatlogic.framework.datawarehouse.dto.DataSourceFieldVo;
import neatlogic.framework.datawarehouse.dto.DataSourceParamVo;
import neatlogic.framework.datawarehouse.dto.DataSourceVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface DataWarehouseDataSourceMapper {
    List<DataSourceVo> getAllHasCronReportDataSource();

    int checkDataSourceNameIsExists(DataSourceVo dataSourceVo);

    List<DataSourceVo> getDataSourceByIdList(@Param("idList") List<Long> dataSourceIdList);

    DataSourceVo getDataSourceById(Long id);

    List<DataSourceVo> getAllDataSource();

    DataSourceVo getDataSourceDetailByName(String name);

    List<DataSourceVo> searchDataSource(DataSourceVo dataSourceVo);

    int searchDataSourceCount(DataSourceVo reportDataSourceVo);

    List<Long> getExistIdListByIdList(List<Long> idList);

    DataSourceVo getDataSourceNameAndFieldNameListById(Long id);

    List<DataSourceVo> getDataSourceListByNameList(List<String> nameList);

    void insertDataSource(DataSourceVo reportDataSourceVo);

    void insertDataSourceParam(DataSourceParamVo dataSourceParamVo);

    void batchInsertDataSourceParam(List<DataSourceParamVo> list);

    void insertDataSourceField(DataSourceFieldVo dataSourceFieldVo);

    void batchInsertDataSourceField(List<DataSourceFieldVo> list);

    // void insertReportDataSourceCondition(DataSourceConditionVo reportDataSourceConditionVo);

    void updateDataSourceField(DataSourceFieldVo dataSourceFieldVo);

    void updateDataSourceParam(DataSourceParamVo dataSourceParamVo);

    void updateDataSourceParamCurrentValue(DataSourceParamVo dataSourceParamVo);

    void updateDataSourceFieldCondition(DataSourceFieldVo dataSourceFieldVo);

    void updateDataSource(DataSourceVo dataSourceVo);

    void updateDataSourcePolicy(DataSourceVo dataSourceVo);

    void updateReportDataSourceIsActive(DataSourceVo dataSourceVo);

    void updateReportDataSourceDataCount(DataSourceVo dataSourceVo);

    void updateReportDataSourceStatus(DataSourceVo dataSourceVo);

    void updateReportDataSourceConditionValue(DataSourceParamVo dataSourceConditionVo);

    void resetReportDataSourceStatus();

    void updateDataSourceJobTimeById(DataSourceVo dataSourceVo);

    void updateDataSourceNextFireTimeById(@Param("id") Long id, @Param("nextFireTime") Date nextFireTime);

    void deleteReportDataSourceById(Long id);

    //void deleteReportDataSourceConditionByDataSourceId(Long dataSourceId);

    void deleteDataSourceFieldByDataSourceId(Long dataSourceId);

    void deleteDataSourceFieldById(Long id);

    void deleteDataSourceFieldByIdList(List<Long> idList);

    void deleteDataSourceParamById(Long id);

    void deleteDataSourceParamByIdList(List<Long> idList);

    void deleteDataSourceParamByDataSourceId(Long dataSourceId);
}
