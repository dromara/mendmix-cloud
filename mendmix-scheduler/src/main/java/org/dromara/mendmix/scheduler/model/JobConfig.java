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
package org.dromara.mendmix.scheduler.model;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.dromara.mendmix.common.json.deserializer.DateTimeConvertDeserializer;
import org.dromara.mendmix.common.json.serializer.DateTimeConvertSerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * 任务配置
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月16日
 */
public class JobConfig implements Serializable {

	private static final long serialVersionUID = 1L;

	private String groupName;

	private String jobName;

	private boolean running = false;// 是否运行中

	private boolean active = true;// 是否启用

	private String cronExpr; //

	@JsonSerialize(using = DateTimeConvertSerializer.class)
	@JsonDeserialize(using = DateTimeConvertDeserializer.class)
	private Date lastFireTime;// 上一次运行开始时间

	@JsonSerialize(using = DateTimeConvertSerializer.class)
	@JsonDeserialize(using = DateTimeConvertDeserializer.class)
	private Date nextFireTime;// 下一次运行开始时间

	// 当前执行节点id
	private String currentNodeId;

	private long modifyTime;
	
	private String errorMsg;
	
	private long execTimes;

	public JobConfig() {
	}

	/**
	 * @param schedulerName
	 * @param remark
	 * @param cronExpr
	 */
	public JobConfig(String groupName, String jobName, String cronExpr) {
		super();
		this.groupName = groupName;
		this.jobName = jobName;
		this.cronExpr = cronExpr;
		this.modifyTime = System.currentTimeMillis();
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getJobName() {
		return jobName;
	}

	public void seJobName(String jobName) {
		this.jobName = jobName;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getCronExpr() {
		return cronExpr;
	}

	public void setCronExpr(String cronExpr) {
		this.cronExpr = cronExpr;
	}

	public Date getLastFireTime() {
		return lastFireTime;
	}

	public void setLastFireTime(Date lastFireTime) {
		this.lastFireTime = lastFireTime;
	}

	public Date getNextFireTime() {
		return nextFireTime;
	}

	public void setNextFireTime(Date nextFireTime) {
		this.nextFireTime = nextFireTime;
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

	public String getCurrentNodeId() {
		return currentNodeId;
	}

	public void setCurrentNodeId(String currentNodeId) {
		this.currentNodeId = currentNodeId;
	}

	public long getModifyTime() {
		return modifyTime;
	}

	public void setModifyTime(long modifyTime) {
		this.modifyTime = modifyTime;
	}

	private static long allowDeviation = 1000 * 60 * 10;
	public String getErrorMsg() {
		if(errorMsg == null){
			if(lastFireTime != null && nextFireTime != null){
				long interval = nextFireTime.getTime() - lastFireTime.getTime();
				long nextFireTimeMils;
				if(running){
					nextFireTimeMils = lastFireTime.getTime() + interval;
				}else{
					nextFireTimeMils = nextFireTime.getTime();
				}
				if(Calendar.getInstance().getTimeInMillis() - nextFireTimeMils > (interval < allowDeviation ? interval : allowDeviation)){
					errorMsg = "nextFireTime Abnormal";
				}else{
					errorMsg = "";
				}
			}
		}
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}
	
	public boolean error(){
		return errorMsg != null;
	}

	public long getExecTimes() {
		return execTimes;
	}

	public void setExecTimes(long execTimes) {
		this.execTimes = execTimes;
	}

}
