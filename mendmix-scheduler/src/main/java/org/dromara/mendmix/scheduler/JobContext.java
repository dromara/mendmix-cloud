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
package org.dromara.mendmix.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.util.DigestUtils;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.scheduler.helper.ConsistencyHash;
import org.dromara.mendmix.scheduler.model.JobConfig;
import org.dromara.mendmix.scheduler.registry.NullJobRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年8月19日
 */
public class JobContext {

	private static final Logger logger = LoggerFactory.getLogger("org.dromara.mendmix");
	
	private static JobContext context = new JobContext();
	
	private Set<String> activeNodes = new HashSet<String>();
	
	private ConsistencyHash hash = new ConsistencyHash(4);
	
	private String groupName;
	
	private Map<String, AbstractJob> allJobs = new HashMap<>();
	
	private PersistHandler persistHandler;
	
	private TaskRetryProcessor retryProcessor;
		
	private JobRegistry registry;
	
	private ExecutorService syncExecutor = Executors.newFixedThreadPool(1);
	
	public void startRetryProcessor(){
		if(retryProcessor == null){
			synchronized (context) {
				if(retryProcessor != null)return;
				retryProcessor = new TaskRetryProcessor(1);
			}
		}
	}

	public static JobContext getContext() {
		return context;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getNodeId() {
		return GlobalContext.getNodeName();
	}
	
	public PersistHandler getPersistHandler() {
		return persistHandler;
	}

	public void setPersistHandler(PersistHandler persistHandler) {
		this.persistHandler = persistHandler;
	}

	public JobRegistry getRegistry() {
		if(registry == null){
			registry = new NullJobRegistry();
		}
		return registry;
	}

	public void setRegistry(JobRegistry registry) {
		this.registry = registry;
	}

	public TaskRetryProcessor getRetryProcessor() {
		return retryProcessor;
	}

	public void refreshNodes(List<String> nodes){
		activeNodes.clear();
		activeNodes.addAll(nodes);
		if(activeNodes.isEmpty())return;
		hash.refresh(nodes);
	}
	
	public void addNode(String node){
		activeNodes.add(node);
		hash.refresh(new ArrayList<>(activeNodes));
	}
	
	public void removeNode(String node){
		activeNodes.remove(node);
		if(activeNodes.isEmpty())return;
		hash.refresh(new ArrayList<>(activeNodes));
	}
	
	public boolean matchCurrentNode(Object shardFactor){
		if(activeNodes.size() == 1)return true;
		String expectNodeId = hash.matchOne(DigestUtils.md5(shardFactor),JobContext.getContext().getNodeId());
		boolean matched = expectNodeId.equals(getNodeId());
		return matched;
	}
	
	public void addJob(AbstractJob job){
		allJobs.put(job.jobName, job);
	}
	
	public static AbstractJob getJob(String jobName) {
		return getContext().allJobs.get(jobName);
	}

	public static Set<String> getAllJobNames() {
		return getContext().allJobs.keySet();
	}
	
	public static Collection<AbstractJob> getAllJobs() {
		return getContext().allJobs.values();
	}

	public Set<String> getActiveNodes() {
		return activeNodes;
	}
	
	public ExecutorService getSyncExecutor() {
		return syncExecutor;
	}

	public void submitSyncTask(Runnable task){
		syncExecutor.execute(task);
	}
	
	public static JobConfig getJobConfig(String jobName) {
		JobConfig conf = getContext().getRegistry().getConf(jobName, true);
		return conf;
	}
	
	public static void reloadCronFromConfigs() {
		Map<String, String> cronExpressions = ResourceUtils.getMappingValues("mendmix-cloud.task.cronExpr");
		AbstractJob job;
		JobConfig jobConfig;
		String cronExpr;
		for (String jobName : cronExpressions.keySet()) {
			job = getJob(jobName);
			if(job == null)continue;
			jobConfig = getContext().getRegistry().getConf(jobName, true);
			cronExpr = cronExpressions.get(jobName);
			if(StringUtils.equals(cronExpr, jobConfig.getCronExpr())) {
				continue;
			}
			job.resetTriggerCronExpr(cronExpr);
			jobConfig.setCronExpr(cronExpr);
			getContext().getRegistry().updateJobConfig(jobConfig);
			if(getContext().getPersistHandler() != null){
				getContext().getPersistHandler().persist(jobConfig);
			}
			logger.info(">>> reload job Cron finish -> job:{},cronExpr:{}",jobName,cronExpr);
		}
	}
	
	public void close(){
		getRegistry().onDestroy();
		if(retryProcessor != null){
			retryProcessor.close();
		}
		if(persistHandler != null){
			persistHandler.close();
		}
		syncExecutor.shutdown();
	}
	

}
