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

package neatlogic.framework.scheduler.core;

import neatlogic.framework.applicationlistener.core.ModuleInitializedListenerBase;
import neatlogic.framework.asynchronization.thread.NeatLogicThread;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.asynchronization.threadpool.CachedThreadPool;
import neatlogic.framework.bootstrap.NeatLogicWebApplicationContext;
import neatlogic.framework.common.RootComponent;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.dao.mapper.TenantMapper;
import neatlogic.framework.dto.TenantVo;
import neatlogic.framework.scheduler.dao.mapper.SchedulerMapper;
import neatlogic.framework.scheduler.dto.*;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.annotation.Resource;
import java.util.*;

@RootComponent
public class SchedulerManager extends ModuleInitializedListenerBase {
    private final Logger logger = LoggerFactory.getLogger(SchedulerManager.class);

    private static final Map<String, IJob> jobHandlerMap = new HashMap<>();
    private static final Map<String, JobClassVo> jobClassMap = new HashMap<>();
    private static final List<JobClassVo> publicJobClassList = new ArrayList<>();

    @Resource
    private TenantMapper tenantMapper;
    @Resource
    private SchedulerFactoryBean schedulerFactoryBean;
    @Resource
    private SchedulerMapper schedulerMapper;

    private List<TenantVo> tenantList = new ArrayList<>();

    protected void myInit() {
        tenantList = tenantMapper.getAllActiveTenant();
    }

    public static IJob getHandler(String className) {
        return jobHandlerMap.get(className);
    }

    public static List<JobClassVo> getAllPublicJobClassList() {
        return publicJobClassList;
    }

    public static JobClassVo getJobClassByClassName(String className) {
        JobClassVo jobClassVo = jobClassMap.get(className);
        if (jobClassVo != null && TenantContext.get().containsModule(jobClassVo.getModuleId())) {
            return jobClassVo;
        }
        return null;
    }

    public boolean checkJobIsExists(String jobName, String jobGroup) {
        JobKey jobKey = new JobKey(jobName, jobGroup);
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        try {
            if (scheduler.getJobDetail(jobKey) != null) {
                return true;
            }
        } catch (SchedulerException e) {
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * 加载定时作业，同时设置定时作业状态和锁
     *
     * @param jobObject 作业信息
     * @return 日期
     */
    public Date loadJob(JobObject jobObject) {
        // 如果结束时间比当前时间早，就不加载了
        if (jobObject.getEndTime() != null && jobObject.getEndTime().before(new Date())) {
            return null;
        }
        try {
            String jobName = jobObject.getJobName();
            String jobGroup = jobObject.getJobGroup();
            String className = jobObject.getJobHandler();

            JobKey jobKey = new JobKey(jobName, jobGroup);
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            if (scheduler.getJobDetail(jobKey) != null) {
                scheduler.deleteJob(jobKey);
            }

            try {
                JobLockVo jobLockVo = schedulerMapper.getJobLockByJobNameGroup(jobName, jobGroup);
                if (jobLockVo == null) {
                    jobLockVo = new JobLockVo(jobName, jobGroup, className);
                    schedulerMapper.insertJobLock(jobLockVo);
                }

                TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger().withIdentity(jobName, jobGroup);
                if (StringUtils.isNotBlank(jobObject.getCron()) && CronExpression.isValidExpression(jobObject.getCron())) {
                    triggerBuilder.withSchedule(CronScheduleBuilder.cronSchedule(jobObject.getCron()));
                } else if (jobObject.getIntervalInSeconds() != null && jobObject.getIntervalInSeconds() > 0) {
                    SimpleScheduleBuilder ssb = SimpleScheduleBuilder.simpleSchedule();
                    ssb = ssb.withIntervalInSeconds(jobObject.getIntervalInSeconds());
                    if (jobObject.getRepeatCount() != null) {
                        ssb.withRepeatCount(jobObject.getRepeatCount());
                    } else {
                        ssb = ssb.repeatForever();
                    }
                    triggerBuilder.withSchedule(ssb);
                } else {
                    return null;
                }

                Date startTime = jobObject.getBeginTime();
                if (startTime != null && startTime.after(new Date())) {
                    triggerBuilder.startAt(startTime);
                } else {
                    triggerBuilder.startNow();
                }
                triggerBuilder.endAt(jobObject.getEndTime());
                Trigger trigger = triggerBuilder.build();
                Class clazz = Class.forName(jobObject.getJobHandler());
                JobDetail jobDetail = JobBuilder.newJob(clazz).withIdentity(jobKey).build();
                jobDetail.getJobDataMap().put("jobObject", jobObject);
                // 写入jobstatus (如果数据库不存在job，则需先insert job到数据库，再创建job,否则jobBase 先触发execute，会导致跳过第一次执行)
                JobStatusVo jobStatusVo = schedulerMapper.getJobStatusByJobNameGroup(jobName, jobGroup);
                if (jobStatusVo == null) {
                    jobStatusVo = new JobStatusVo();
                    jobStatusVo.setJobName(jobName);
                    jobStatusVo.setJobGroup(jobGroup);
                    jobStatusVo.setHandler(className);
                    schedulerMapper.insertJobStatus(jobStatusVo);
                }
                Date nextFireDate = scheduler.scheduleJob(jobDetail, trigger);
                jobStatusVo.setNextFireTime(nextFireDate);
                schedulerMapper.updateJobNextFireTime(jobStatusVo);
//                schedulerMapper.insertJobLoadTime(new JobLoadTimeVo(jobObject.getJobName(), jobObject.getJobGroup(), jobObject.getCron(), jobObject.getLoadTime()));
                return nextFireDate;
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * @param jobObject 作业信息
     * @return void
     * @Description: 将定时作业从调度器中删除
     */
    public boolean unloadJob(JobObject jobObject) {
//        schedulerMapper.deleteJobLoadTime(new JobLoadTimeVo(jobObject.getJobName(), jobObject.getJobGroup()));
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            JobKey jobKey = new JobKey(jobObject.getJobName(), jobObject.getJobGroup());
            if (scheduler.getJobDetail(jobKey) != null) {
                scheduler.deleteJob(jobKey);
            }
            // 清除作业锁和作业状态信息
            schedulerMapper.deleteJobLock(jobObject.getJobName(), jobObject.getJobGroup());
            schedulerMapper.deleteJobStatus(jobObject.getJobName(), jobObject.getJobGroup());
            return true;
        } catch (SchedulerException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void onInitialized(NeatLogicWebApplicationContext context) {
        Map<String, IJob> myMap = context.getBeansOfType(IJob.class);
        List<IJob> tmpJobHandlerList = new ArrayList<>();
        for (Map.Entry<String, IJob> entry : myMap.entrySet()) {
            IJob job = entry.getValue();
            tmpJobHandlerList.add(job);
            jobHandlerMap.put(job.getClassName(), job);
            JobClassVo jobClassVo = new JobClassVo(job.getClassName(), context.getId());
            jobClassMap.put(job.getClassName(), jobClassVo);
            // 如果定时作业组件没有实现IPublicJob接口，不会插入schedule_job_class表
            if (job instanceof IPublicJob) {
                IPublicJob publicJob = (IPublicJob) job;
                jobClassVo.setName(publicJob.getName());
                jobClassVo.setType(JobClassVo.PUBLIC);
                publicJobClassList.add(jobClassVo);
            }
        }
        for (TenantVo tenantVo : tenantList) {
            CachedThreadPool.execute(new ScheduleLoadJobRunner(tenantVo.getUuid(), tmpJobHandlerList));
        }
        // TODO 这里要增加清理job_status的逻辑
    }

    class ScheduleLoadJobRunner extends NeatLogicThread {

        private final String tenantUuid;
        private final List<IJob> jobHandlerList;

        public ScheduleLoadJobRunner(String _tenantUuid, List<IJob> _jobHandlerList) {
            super("SCHEDULE-JOB-LOADER-" + _tenantUuid);
            tenantUuid = _tenantUuid;
            jobHandlerList = _jobHandlerList;
        }

        @Override
        protected void execute() {
            String oldThreadName = Thread.currentThread().getName();
            try {
                // 切换租户数据源
                TenantContext.get().switchTenant(tenantUuid).setUseDefaultDatasource(false);
                UserContext.init(SystemUser.SYSTEM.getUserVo(), SystemUser.SYSTEM.getTimezone());
                for (IJob jobHandler : jobHandlerList) {
                    jobHandler.initJob(tenantUuid);
                }
                schedulerMapper.deleteUnusedJobStatus();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                Thread.currentThread().setName(oldThreadName);
            }
        }
    }

}
