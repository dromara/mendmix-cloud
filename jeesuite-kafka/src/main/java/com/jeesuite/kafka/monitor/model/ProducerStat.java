/**
 * 
 */
package com.jeesuite.kafka.monitor.model;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月10日
 */
public class ProducerStat implements Serializable {

	private static final long serialVersionUID = 3381280990522906667L;
	
	private String topic;
	private String group;
	private long successNums;
	private long errorNums;
	private long latestSuccessNums;
	private long latestErrorNums;
	private long updateTime;

	private String source;
	public ProducerStat() {}
	
	public ProducerStat(String topic, String group, AtomicLong successNums, AtomicLong errorNums, AtomicLong latestSuccessNums,
			AtomicLong latestErrorNums) {
		super();
		this.topic = topic;
		this.group = group;
		this.successNums = successNums.get();
		this.errorNums = errorNums.get();
		this.latestSuccessNums = latestSuccessNums.get();
		this.latestErrorNums = latestErrorNums.get();
		this.updateTime = System.currentTimeMillis();
	}

	public String getTopic() {
		return topic;
	}
	public void setTopic(String topic) {
		this.topic = topic;
	}
	public String getGroup() {
		return group;
	}
	public void setGroup(String group) {
		this.group = group;
	}
	public long getSuccessNums() {
		return successNums;
	}
	public void setSuccessNums(long successNums) {
		this.successNums = successNums;
	}
	public long getErrorNums() {
		return errorNums;
	}
	public void setErrorNums(long errorNums) {
		this.errorNums = errorNums;
	}
	public long getLatestSuccessNums() {
		return latestSuccessNums;
	}
	public void setLatestSuccessNums(long latestSuccessNums) {
		this.latestSuccessNums = latestSuccessNums;
	}
	public long getLatestErrorNums() {
		return latestErrorNums;
	}
	public void setLatestErrorNums(long latestErrorNums) {
		this.latestErrorNums = latestErrorNums;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public long getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}
	
	public String getFormatLastTime(){
		long diffSeconds = (System.currentTimeMillis() - updateTime)/1000;
		if(diffSeconds >= 86400){
			return (diffSeconds/86400) + " 天前";
		}
		if(diffSeconds >= 3600){
			return (diffSeconds/3600) + " 小时前";
		}
		if(diffSeconds >= 60){
			return (diffSeconds/60) + " 分钟前";
		}
		
		return diffSeconds + " 秒前";
		
	}
}
