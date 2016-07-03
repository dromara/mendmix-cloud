/**
 * 
 */
package com.jeesuite.kafka.monitor;

import java.util.Date;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月22日
 */
public class TopicPartitionStat {

	private String topic;
	private int partition;
	private long logSize;
	private long offset;
	private Date createTime;
	private Date lastTime;
	
	
	
	public TopicPartitionStat() {}
	public TopicPartitionStat(String topic, int partition) {
		super();
		this.topic = topic;
		this.partition = partition;
	}
	
	public TopicPartitionStat(String topic, int partition, long offset) {
		super();
		this.topic = topic;
		this.partition = partition;
		this.offset = offset;
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
}
