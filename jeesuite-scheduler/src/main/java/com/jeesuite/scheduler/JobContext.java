/**
 * 
 */
package com.jeesuite.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.scheduler.helper.ConsistencyHash;
import com.jeesuite.scheduler.registry.NullJobRegistry;

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
		if(ResourceUtils.getBoolean("jeesuite.task.registry.disabled", false)){
        	return;
        }
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
		String expectNodeId = hash.getAssignedRealNode(shardFactor);
		return expectNodeId.equals(getNodeId());
	}
	
	public void addJob(AbstractJob job){
		String key = job.group + ":" + job.jobName;
		allJobs.put(key, job);
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
