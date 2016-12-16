/**
 * 
 */
package com.jeesuite.scheduler.registry;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.jeesuite.scheduler.JobRegistry;
import com.jeesuite.scheduler.model.JobConfig;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月16日
 */
public class NullJobRegistry implements JobRegistry {

	private Map<String, JobConfig> schedulerConfgs = new ConcurrentHashMap<>();

	
	@Override
	public void register(JobConfig conf) {
		schedulerConfgs.put(conf.getJobName(), conf);
	}


	@Override
	public void updateJobConfig(JobConfig conf) {
		schedulerConfgs.put(conf.getJobName(), conf);
	}


	@Override
	public void setRuning(String jobName, Date fireTime) {
		JobConfig config = schedulerConfgs.get(jobName);
		config.setRunning(true);
		config.setLastFireTime(fireTime);
	}


	@Override
	public void setStoping(String jobName, Date nextFireTime,Exception e) {
		JobConfig config = schedulerConfgs.get(jobName);
		config.setRunning(false);
		config.setNextFireTime(nextFireTime);
	}

	@Override
	public JobConfig getConf(String jobName, boolean forceRemote) {
		return schedulerConfgs.get(jobName);
	}


	@Override
	public void unregister(String jobName) {
		schedulerConfgs.clear();
		schedulerConfgs = null;
	}

	@Override
	public List<JobConfig> getAllJobs() {
		return new ArrayList<>(schedulerConfgs.values());
	}

}
