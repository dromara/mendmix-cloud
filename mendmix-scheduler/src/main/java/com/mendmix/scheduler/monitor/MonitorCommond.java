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
package com.mendmix.scheduler.monitor;

import java.io.Serializable;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年10月31日
 */
public class MonitorCommond implements Serializable {

	private static final long serialVersionUID = 1L;


	public final static byte TYPE_EXEC = 0x01; 	//执行定时任务
	public final static byte TYPE_STATUS_MOD = 0x02;//开停状态修改 
	public final static byte TYPE_CRON_MOD = 0x03;//执行时间策略修改 
	
	
	private byte cmdType;
	private String jobGroup; 
	private String jobName; 
	private Object body;
	
	public MonitorCommond() {}

	public MonitorCommond(byte cmdType, String jobGroup, String jobName, Object body) {
		super();
		this.cmdType = cmdType;
		this.jobGroup = jobGroup;
		this.jobName = jobName;
		this.body = body;
	}
	public byte getCmdType() {
		return cmdType;
	}
	public void setCmdType(byte cmdType) {
		this.cmdType = cmdType;
	}
	public String getJobGroup() {
		return jobGroup;
	}
	public void setJobGroup(String jobGroup) {
		this.jobGroup = jobGroup;
	}
	public String getJobName() {
		return jobName;
	}
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public Object getBody() {
		return body;
	}

	public void setBody(Object body) {
		this.body = body;
	}

	@Override
	public String toString() {
		return "MonitorCommond [cmdType=" + cmdType + ", jobGroup=" + jobGroup + ", jobName=" + jobName + "]";
	}
	
	
}
