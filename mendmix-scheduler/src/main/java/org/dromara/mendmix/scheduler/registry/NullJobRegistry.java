/*
 * Copyright 2016-2022 dromara.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dromara.mendmix.scheduler.registry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.scheduler.JobContext;
import org.dromara.mendmix.scheduler.model.JobConfig;
import org.dromara.mendmix.scheduler.monitor.SchManageCommond;

import com.google.common.eventbus.Subscribe;

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
		config.setExecTimes(config.getExecTimes() + 1);
		config.setCurrentNodeId(JobContext.getContext().getNodeId());
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
	public void onRegistered() {
		JobContext.getContext().addNode(GlobalContext.getNodeName());
	}
	
	@Subscribe
	public void processCommand(SchManageCommond cmd){
		execCommond(cmd);
	}

}
