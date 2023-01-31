/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.framework.auditconfig.handler;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.auditconfig.core.AuditCleanerBase;
import neatlogic.framework.common.config.Config;
import neatlogic.framework.file.core.AuditType;
import neatlogic.framework.healthcheck.dao.mapper.DatabaseFragmentMapper;
import neatlogic.framework.restful.dao.mapper.ApiAuditMapper;
import neatlogic.framework.util.TimeUtil;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;

@Component
public class ApiAuditCleaner extends AuditCleanerBase {

    private static Logger logger = LoggerFactory.getLogger(ApiAuditCleaner.class);

    @Resource
    private ApiAuditMapper apiAuditMapper;
    @Resource
    private DatabaseFragmentMapper databaseFragmentMapper;

    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(TimeUtil.YYYY_MM_DD_HH_MM_SS_SSS);
    @Override
    public String getName() {
        return "API-AUDIT";
    }

    @Override
    protected void myClean(int dayBefore) {
        String directoryPath = Config.DATA_HOME() + TenantContext.get().getTenantUuid() + File.separator + AuditType.API_AUDIT.getType();
        File dir = new File(directoryPath);
        if (dir.exists()) {
            File[] listFiles = dir.listFiles();
            Arrays.sort(listFiles, Comparator.comparing(File::lastModified));
            for (File file : listFiles) {
                try (ReversedLinesFileReader rlfr = new ReversedLinesFileReader(file, StandardCharsets.UTF_8)) {
                    String lastLine = rlfr.readLine();
                    if (lastLine.startsWith("fileFooter##########") && lastLine.endsWith("##########fileFooter")) {
                        String formatStr = lastLine.substring(20, lastLine.length() - 20);
                        LocalDate endDate = LocalDate.parse(formatStr, dateTimeFormatter);
                        if (LocalDate.now().toEpochDay() - endDate.toEpochDay() > dayBefore) {
                            file.delete();
                        }
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        apiAuditMapper.deleteAuditByDayBefore(dayBefore);
        databaseFragmentMapper.rebuildTable(TenantContext.get().getDbName(), "api_audit");
    }
}
