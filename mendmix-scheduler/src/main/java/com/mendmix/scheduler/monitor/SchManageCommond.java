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
public class SchManageCommond implements Serializable {

	private static final long serialVersionUID = 1L;

	public static enum CommondType {
		exec,toggle,updateCron
	}

	
	private CommondType cmdType;
	private String jobName; 
	private Object body;
	
	public SchManageCommond() {}

	public SchManageCommond(CommondType cmdType, String jobName, Object body) {
		super();
		this.cmdType = cmdType;
		this.jobName = jobName;
		this.body = body;
	}
	public CommondType getCmdType() {
		return cmdType;
	}
	public void setCmdType(CommondType cmdType) {
		this.cmdType = cmdType;
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
		return "MonitorCommond [cmdType=" + cmdType + ", jobName=" + jobName + "]";
	}
	
	
}
