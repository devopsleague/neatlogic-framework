package codedriver.framework.scheduler.core;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.List;

import org.quartz.CronTrigger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.scheduler.annotation.Input;
import codedriver.framework.scheduler.annotation.Param;
import codedriver.framework.scheduler.dao.mapper.SchedulerMapper;
import codedriver.framework.scheduler.dto.JobClassVo;
import codedriver.framework.scheduler.dto.JobPropVo;
import codedriver.framework.scheduler.dto.JobVo;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class TestJob extends JobBase implements IJob {

	private Logger logger = LoggerFactory.getLogger(TestJob.class.getName());
	
	@Autowired
	private SchedulerMapper scheduleMapper;
	
	@Input({@Param(name="p_1", dataType="int", controlType="t1", controlValue="v1", description="p1", required=true),
		@Param(name="p_2", dataType="Integer", controlType="t2", controlValue="v2", description="p2", required=true),
		@Param(name="p_3", dataType="long", controlType="t3", controlValue="v3", description="p3", required=true),
		@Param(name="p_4", dataType="Long", controlType="t4", controlValue="v4", description="p4", required=true),
		@Param(name="p_5", dataType="String", controlValue="v5", description="p5", required=true)})
	@Override
	public void executeInternal(JobExecutionContext context) throws JobExecutionException {
		
//		System.out.println("睡眠中");
//		long begin = System.currentTimeMillis();
//		long time = 0;
//		while (time < 23000) {
//			time = System.currentTimeMillis() - begin;
//		}
//		System.out.println("醒来");
		
		JobDetail jobDetail = context.getJobDetail();
		JobKey jobKey = jobDetail.getKey();
		System.out.println(jobKey.getGroup());
		System.out.println(TenantContext.get().getTenantUuid());
		JobDataMap jobDataMap = jobDetail.getJobDataMap();		
		System.out.println(jobDataMap.getString("p_1"));
		System.out.println(jobDataMap.getString("p_2"));
		System.out.println(jobDataMap.getString("p_3"));
		System.out.println(jobDataMap.getString("p_4"));
		System.out.println(jobDataMap.getString("p_5"));
//		int count = jobDataMap.getInt("execCount");
//		System.err.println("jobDetail count: "+count);
//		jobDataMap.put("execCount",count+1);
		System.out.println("一分钟执行一次");
		String jobId = jobKey.getName();
		JobVo jobVo = scheduleMapper.getJobById(Long.valueOf(jobId));
		Trigger trigger = context.getTrigger();
//		JobDataMap jobDataMap2 = trigger.getJobDataMap();
//		int count2 = jobDataMap2.getInt("execCount");
//		System.err.println("trigger count: "+count2);
//		jobDataMap2.put("execCount",count2+1);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if(trigger instanceof SimpleTrigger) {
			SimpleTrigger simpleTrigger = (SimpleTrigger) trigger;
			int times = simpleTrigger.getTimesTriggered();
			System.err.println("---------------------times:"+times+"次");
			System.err.println("---------------------PreviousFireTime:"+sdf.format(simpleTrigger.getPreviousFireTime()));
			System.err.println("---------------------NextFireTime:"+(simpleTrigger.getNextFireTime() == null ? "null" : sdf.format(simpleTrigger.getNextFireTime())));	
		}else if(trigger instanceof CronTrigger) {			
			CronTrigger cronTrigger = (CronTrigger) trigger;
			System.err.println("---------------------PreviousFireTime:"+sdf.format(cronTrigger.getPreviousFireTime()));
			System.err.println("---------------------NextFireTime:"+(cronTrigger.getNextFireTime() == null ? "null" : sdf.format(cronTrigger.getNextFireTime())));					
		}
		System.err.println("---------------------FireTime:"+sdf.format(context.getFireTime()));
		System.err.println("---------------------NextFireTime:"+(context.getNextFireTime() == null ? "null" : sdf.format(context.getNextFireTime())));
		System.err.println("---------------------LastFireTime:"+sdf.format(jobVo.getLastFireTime()));
		System.err.println("---------------------NextFireTime:"+sdf.format(jobVo.getNextFireTime()));
		
		List<JobPropVo> propList = jobVo.getPropList();
		if(propList != null && !propList.isEmpty()) {
			for(JobPropVo prop : propList) {
				System.out.println(prop.getName() + ":" + prop.getValue());
			}
		}
		logger.info("一分钟执行一次");
		
//		OutputStreamWriter logOut = (OutputStreamWriter) context.get("logOutput");
//		try {
//			logOut.write("success");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		int i = 1/0;
	}

	@Override
	public String getJobClassName() {
		return "测试Job";
	}

	@Override
	public String getType() {
		return JobClassVo.FLOW_TYPE;
	}

}
