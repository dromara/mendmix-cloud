/**
 * 
 */
package com.jeesuite.scheduler;

import java.util.Date;

/**
 * 类    名：ControlHandler.java<br />
 *   
 * 功能描述：  定时任务执行控制接口	<br />
 *  
 * 创建日期：2012-11-22下午04:44:06  <br />   
 * 
 * 版本信息：v 1.0<br />
 * 
 * 版权信息：Copyright (c) 2011 Csair All Rights Reserved<br />
 * 
 * 作    者：<a href="mailto:vakinge@gmail.com">vakin jiang</a><br />
 * 
 * 修改记录： <br />
 * 修 改 者    修改日期     文件版本   修改说明	
 */
public interface ControlHandler {

	void register(SchedulerConfg conf);
	
	void setRuning(String jobName,Date fireTime);
	
	void setStoping(String jobName,Date nextFireTime);
	
	SchedulerConfg getConf(String jobName,boolean forceRemote);
	
	void unregister(String jobName);
}
