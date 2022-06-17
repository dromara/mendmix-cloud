/*
 * Copyright 2016-2018 www.mendmix.com.
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
package com.mendmix.scheduler.registry;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.scheduler.AbstractJob;
import com.mendmix.scheduler.JobContext;
import com.mendmix.scheduler.JobRegistry;
import com.mendmix.scheduler.model.JobConfig;
import com.mendmix.scheduler.monitor.MonitorCommond;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年12月11日
 */
public abstract class AbstarctJobRegistry implements JobRegistry{

	private static final Logger logger = LoggerFactory.getLogger("com.mendmix.scheduler.registry");
	
	protected Map<String, JobConfig> schedulerConfgs = new ConcurrentHashMap<>();

	protected volatile boolean updatingStatus;
	
	/**
	 * 重新分配执行节点
	 * 
	 * @param nodes
	 */
	protected synchronized void rebalanceJobNode(List<String> nodes) {
		while (updatingStatus)
			;
		Collection<JobConfig> jobs = schedulerConfgs.values();
		int nodeIndex = 0;
		for (JobConfig job : jobs) {
			String nodeId = nodes.get(nodeIndex++);
			if (!StringUtils.equals(job.getCurrentNodeId(), nodeId)) {
				job.setCurrentNodeId(nodeId);
				logger.info("MENDMIX-TRACE-LOGGGING-->> rebalance Job[{}-{}] To Node[{}] ", job.getGroupName(), job.getJobName(), nodeId);
			}
			if (nodeIndex >= nodes.size()) {
				nodeIndex = 0;
			}
			//
			updateJobConfig(job);
		}

	}
	
	public void execCommond(MonitorCommond cmd){
		if(cmd == null)return;
		JobConfig config = schedulerConfgs.get(cmd.getJobName());
		final AbstractJob abstractJob = JobContext.getContext().getAllJobs().get(cmd.getJobName());
		if(MonitorCommond.TYPE_EXEC == cmd.getCmdType()){
			if(config.isRunning()){
				logger.info("MENDMIX-TRACE-LOGGGING-->> tasl[{}] is running,return",abstractJob.getJobName());
				return;
			}
			if(abstractJob != null){
				JobContext.getContext().submitSyncTask(new Runnable() {
					@Override
					public void run() {
						try {
							logger.info("MENDMIX-TRACE-LOGGGING-->> begin execute job[{}] by MonitorCommond",abstractJob.getJobName());
							abstractJob.doJob(JobContext.getContext());
						} catch (Exception e) {
							logger.error(abstractJob.getJobName(),e);
						}
					}
				});
			}else{
				logger.warn("MENDMIX-TRACE-LOGGGING-->> Not found job :{} !!!!",cmd.getJobName());
			}
		}else if(MonitorCommond.TYPE_STATUS_MOD == cmd.getCmdType() 
				|| MonitorCommond.TYPE_CRON_MOD == cmd.getCmdType()){
			
			if(config != null){
				if(MonitorCommond.TYPE_STATUS_MOD == cmd.getCmdType()){					
					config.setActive("1".equals(cmd.getBody()));
				}else{
					try {
						new CronExpression(cmd.getBody().toString());
					} catch (Exception e) {
						throw new RuntimeException("cron表达式格式错误");
					}
					abstractJob.resetTriggerCronExpr(cmd.getBody().toString());
					config.setCronExpr(cmd.getBody().toString());
					
				}
				updateJobConfig(config);
				if(JobContext.getContext().getPersistHandler() != null){
					JobContext.getContext().getPersistHandler().persist(config);
				}
			}
		}
	}
}
