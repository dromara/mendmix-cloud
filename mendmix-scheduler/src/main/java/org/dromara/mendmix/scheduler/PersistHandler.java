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

import org.dromara.mendmix.scheduler.model.JobConfig;
import org.dromara.mendmix.scheduler.model.JobExceResult;

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
