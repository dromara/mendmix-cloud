/**
 * 
 */
package com.jeesuite.scheduler;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;

import com.jeesuite.scheduler.helper.ConsistencyHash;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年8月19日
 */
public class JobContext {

	private String nodeId;
	
	private static JobContext context = new JobContext();
	
	private Set<String> activeNodes = new HashSet<String>();
	
	private ConsistencyHash hash = new ConsistencyHash();
	
	private Map<String, AbstractJob> allJobs = new HashMap<>();
	

	private JobContext() {
		try {
			nodeId = InetAddress.getLocalHost().getHostName() + "_" + RandomStringUtils.random(3, true, true).toLowerCase();
		} catch (Exception e) {
			nodeId = UUID.randomUUID().toString();
		}
	}

	public static JobContext getContext() {
		return context;
	}

	public String getNodeId() {
		return nodeId;
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
		return expectNodeId.equals(nodeId);
	}
	
	public void addJob(AbstractJob job){
		String key = job.group + ":" + job.jobName;
		allJobs.put(key, job);
	}

	public Map<String, AbstractJob> getAllJobs() {
		return allJobs;
	}

}
