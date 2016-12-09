/**
 * 
 */
package com.jeesuite.kafka.monitor.model;

import java.util.Date;

import com.jeesuite.common.util.DateUtils;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月22日
 */
public class TopicPartitionInfo {

	private String topic;
	private int partition;
	private long logSize;
	private long offset;
	private Date createTime;
	private Date lastTime;
	private String owner;
	
	
	
	public TopicPartitionInfo() {}
	public TopicPartitionInfo(String topic, int partition) {
		super();
		this.topic = topic;
		this.partition = partition;
	}
	
	public TopicPartitionInfo(String topic, int partition, long offset) {
		super();
		this.topic = topic;
		this.partition = partition;
		this.offset = offset;
	}
	
	public TopicPartitionInfo(String topic, int partition, long offset, String owner) {
		super();
		this.topic = topic;
		this.partition = partition;
		this.offset = offset;
		this.owner = owner;
	}
	
	public String getTopic() {
		return topic;
	}
	public void setTopic(String topic) {
		this.topic = topic;
	}
	public int getPartition() {
		return partition;
	}
	public void setPartition(int partition) {
		this.partition = partition;
	}
	public long getLogSize() {
		return logSize;
	}
	public void setLogSize(long logSize) {
		this.logSize = logSize;
	}
	public long getOffset() {
		return offset;
	}
	public void setOffset(long offset) {
		this.offset = offset;
	}
	public Date getCreateTime() {
		return createTime;
	}
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
	public Date getLastTime() {
		return lastTime;
	}
	public void setLastTime(Date lastTime) {
		this.lastTime = lastTime;
	}

	public long getLat(){
		return getLogSize() - getOffset();
	}

	

	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public String getFormatLastTime(){
		if(getLastTime() == null)return null;
		long diffSeconds = DateUtils.getDiffSeconds(new Date(), getLastTime());
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
