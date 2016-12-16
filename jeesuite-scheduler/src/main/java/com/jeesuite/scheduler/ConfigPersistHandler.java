/**
 * 
 */
package com.jeesuite.scheduler;

import com.jeesuite.scheduler.model.JobConfig;

/**
 * 配置持久化处理器
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月16日
 */
public interface ConfigPersistHandler {
	
	/**
	 * 启动时合并配置
	 * @param groupName
	 * @param jobName
	 * @return
	 */
	void merge(JobConfig config);
	
	/**
	 * 持久化配置
	 * @param config
	 */
	void persist(JobConfig config);
}
