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

import java.util.Date;
import java.util.List;

import com.mendmix.scheduler.model.JobConfig;

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
public interface JobRegistry {

	void register(JobConfig conf);
	
	void updateJobConfig(JobConfig conf);
	
	void setRuning(String jobName,Date fireTime);
	
	void setStoping(String jobName,Date nextFireTime,Exception e);
	
	JobConfig getConf(String jobName,boolean forceRemote);
	
	void unregister(String jobName);
	
	List<JobConfig> getAllJobs();
	
	void onRegistered();
}
