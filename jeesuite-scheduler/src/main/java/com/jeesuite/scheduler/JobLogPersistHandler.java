/**
 * 
 */
package com.jeesuite.scheduler;

import java.util.Date;

import com.jeesuite.scheduler.model.JobConfig;

/**
 * 任务运行日志持久化接口
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月30日
 */
public interface JobLogPersistHandler {

	public void onSucess(JobConfig conf, Date nextFireTime);
	
	public void onError(JobConfig conf, Date nextFireTime,Exception e);
}
