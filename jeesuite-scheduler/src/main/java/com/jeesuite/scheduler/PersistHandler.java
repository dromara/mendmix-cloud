/**
 * 
 */
package com.jeesuite.scheduler;

import com.jeesuite.scheduler.model.JobConfig;
import com.jeesuite.scheduler.model.JobExceResult;

/**
 * 持久化处理器
 * 
 * <br>
 * Class Name   : PersistHandler
 *
 * @author jiangwei
 * @version 1.0.0
 * @date Mar 21, 2021
 */
public interface PersistHandler {

	/**
	 * 获取保存的配置
	 * @param jobName
	 * @return
	 */
	JobConfig get(String jobName);

	/**
	 * 持久化配置
	 * 
	 * @param config
	 */
	void persist(JobConfig conf);
	/**
	 * 运行日志
	 * @param conf
	 * @param e
	 */
	public void saveLog(JobConfig conf, Exception e);
	
	/**
	 * 最近一次运行结果
	 * @param conf
	 * @return
	 */
	public JobExceResult getLatestResult(String jobName);
	
	public void close();
	
}
