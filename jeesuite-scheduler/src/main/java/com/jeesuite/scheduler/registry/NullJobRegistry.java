/**
 * 
 */
package com.jeesuite.scheduler.registry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.google.common.eventbus.Subscribe;
import com.jeesuite.scheduler.model.JobConfig;
import com.jeesuite.scheduler.monitor.MonitorCommond;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月16日
 */
public class NullJobRegistry extends AbstarctJobRegistry {

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
		config.setModifyTime(Calendar.getInstance().getTimeInMillis());
		config.setErrorMsg(e == null ? null : e.getMessage());
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

	@Override
	public void onRegistered() {}
	
	@Subscribe
	public void processCommand(MonitorCommond cmd){
		execCommond(cmd);
	}

}
