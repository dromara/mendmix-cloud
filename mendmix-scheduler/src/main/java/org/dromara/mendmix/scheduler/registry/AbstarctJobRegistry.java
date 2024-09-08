/*
 * Copyright 2016-2018 dromara.org.
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.util.BeanUtils;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.scheduler.AbstractJob;
import org.dromara.mendmix.scheduler.JobContext;
import org.dromara.mendmix.scheduler.JobRegistry;
import org.dromara.mendmix.scheduler.model.JobConfig;
import org.dromara.mendmix.scheduler.monitor.SchManageCommond;
import org.dromara.mendmix.scheduler.monitor.SchManageCommond.CommondType;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年12月11日
 */
public abstract class AbstarctJobRegistry implements JobRegistry{

	private static final Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.scheduler");
	
	protected static String groupName = ResourceUtils.getProperty("mendmix-cloud.task.groupName", GlobalContext.APPID);
	
	protected Map<String, JobConfig> schedulerConfgs = new ConcurrentHashMap<>();
	protected volatile boolean updatingStatus;
	
	/**
	 * 重新分配执行节点
	 * 
	 * @param nodes
	 */
	protected synchronized void rebalanceJobNode(List<String> nodes) {
		while (updatingStatus);
		Collection<JobConfig> jobs = schedulerConfgs.values();
		int nodeIndex = 0;
		JobConfig remoteJobConfig;
		for (JobConfig job : jobs) {
			String currentNodeId = job.getCurrentNodeId();
			//获取远程最新的
			remoteJobConfig = getConf(job.getJobName(), true);			
			if(remoteJobConfig != null) {
				BeanUtils.copy(remoteJobConfig, job);
				job.setCurrentNodeId(currentNodeId);
			}
			//
			String nodeId = nodes.get(nodeIndex++);
			if (!StringUtils.equals(currentNodeId, nodeId)) {
				job.setCurrentNodeId(nodeId);
				logger.info(">> rebalance Job[{}-{}] To Node[{}] ", job.getGroupName(), job.getJobName(), nodeId);
			}
			if (nodeIndex >= nodes.size()) {
				nodeIndex = 0;
			}
			//
			updateJobConfig(job);
		}

	}

	public void execCommond(SchManageCommond cmd){
		if(cmd == null)return;
		JobConfig config = schedulerConfgs.get(cmd.getJobName());
		if(config == null) {
			logger.info(">> 任务[{}]不存在",cmd.getJobName());
			return;
		}
		final AbstractJob abstractJob = JobContext.getJob(cmd.getJobName());
		if(CommondType.exec == cmd.getCmdType()){
			if(config.isRunning()){
				logger.info(">> 任务正在执行中，请稍后再执行");
				return;
			}
			if(abstractJob != null){
				JobContext.getContext().submitSyncTask(new Runnable() {
					@Override
					public void run() {
						try {
							logger.info(">> begin execute job[{}] by MonitorCommond",abstractJob.getJobName());
							abstractJob.doJob(JobContext.getContext());
						} catch (Exception e) {
							logger.error(abstractJob.getJobName(),e);
						}
					}
				});
			}else{
				logger.warn("Not found job:{} !!!!",cmd.getJobName());
			}
		}else if(CommondType.toggle == cmd.getCmdType() 
				|| CommondType.updateCron == cmd.getCmdType()){
			
			if(config != null){
				if(CommondType.toggle == cmd.getCmdType()){					
					config.setActive(!config.isActive());
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

	@Override
	public void onDestroy() {}
	
}
