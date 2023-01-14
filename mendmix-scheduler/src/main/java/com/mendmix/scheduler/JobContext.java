/*
 * Copyright 2016-2022 www.mendmix.com.
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
package com.mendmix.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.scheduler.helper.ConsistencyHash;
import com.mendmix.scheduler.registry.NullJobRegistry;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年8月19日
 */
public class JobContext {

	private static JobContext context = new JobContext();
	
	private Set<String> activeNodes = new HashSet<String>();
	
	private ConsistencyHash hash = new ConsistencyHash();
	
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
		return GlobalRuntimeContext.getNodeName();
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
		String expectNodeId = hash.matchOneNode(shardFactor);
		return expectNodeId.equals(getNodeId());
	}
	
	public void addJob(AbstractJob job){
		allJobs.put(job.jobName, job);
	}

	public Map<String, AbstractJob> getAllJobs() {
		return allJobs;
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
	
	public void close(){
		if(retryProcessor != null){
			retryProcessor.close();
		}
		if(persistHandler != null){
			persistHandler.close();
		}
		syncExecutor.shutdown();
	}
	

}
