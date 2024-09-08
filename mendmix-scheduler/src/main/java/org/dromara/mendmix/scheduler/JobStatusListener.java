/**
 * 
 */
package org.dromara.mendmix.scheduler;

import org.dromara.mendmix.common.util.ExceptionFormatUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <br>
 * @author vakinge
 * @date 2022年月12日
 */
public class JobStatusListener implements JobListener {

	private static final Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.scheduler");
	
	@Override
	public String getName() {
		return JobStatusListener.class.getSimpleName();
	}


	@Override
	public void jobToBeExecuted(JobExecutionContext context) {}

	@Override
	public void jobExecutionVetoed(JobExecutionContext context) {
		String jobName = context.getJobDetail().getKey().getName();
		logger.warn("<job_listener> job[{}] executionVetoed!!!!",jobName);
	}

	@Override
	public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
		if(jobException != null) {
			String jobName = context.getJobDetail().getKey().getName();
			logger.error("<job_listener> job[{}] wasExecuted but exceptioned,error:{}",jobName,ExceptionFormatUtils.buildExceptionMessages(jobException, 5));
		}
	}

}
