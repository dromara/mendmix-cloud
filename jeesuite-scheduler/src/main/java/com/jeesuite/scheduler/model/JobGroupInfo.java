/**
 * 
 */
package com.jeesuite.scheduler.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年10月30日
 */
public class JobGroupInfo {

	private String name;
	
	List<JobConfig> jobs = new ArrayList<>();
	
	List<String >clusterNodes = new ArrayList<>();
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<JobConfig> getJobs() {
		return jobs;
	}

	public void setJobs(List<JobConfig> jobs) {
		this.jobs = jobs;
	}

	public List<String> getClusterNodes() {
		return clusterNodes;
	}

	public void setClusterNodes(List<String> clusterNodes) {
		this.clusterNodes = clusterNodes;
	}
}
