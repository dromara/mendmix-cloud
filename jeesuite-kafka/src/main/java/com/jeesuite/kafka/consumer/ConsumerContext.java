/**
 * 
 */
package com.jeesuite.kafka.consumer;

import java.util.Map;
import java.util.Properties;

import com.jeesuite.kafka.handler.MessageHandler;
import com.jeesuite.kafka.handler.OffsetLogHanlder;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年2月10日
 */
public class ConsumerContext {

	private String groupId;
	
	private String consumerId;
	
	private OffsetLogHanlder offsetLogHanlder;
	
	private Properties configs;
	
	private Map<String, MessageHandler> messageHandlers;
	
	private int maxProcessThreads;
	
	
	
	public ConsumerContext(Properties configs, String groupId, String consumerId,
			Map<String, MessageHandler> messageHandlers, int maxProcessThreads) {
		super();
		this.configs = configs;
		this.groupId = groupId;
		this.consumerId = consumerId;
		this.messageHandlers = messageHandlers;
		this.maxProcessThreads = maxProcessThreads;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getConsumerId() {
		return consumerId;
	}

	public Properties getProperties() {
		return configs;
	}

	public Map<String, MessageHandler> getMessageHandlers() {
		return messageHandlers;
	}

	public int getMaxProcessThreads() {
		return maxProcessThreads;
	}

	public void setOffsetLogHanlder(OffsetLogHanlder offsetLogHanlder) {
		this.offsetLogHanlder = offsetLogHanlder;
	}
	
    public OffsetLogHanlder getOffsetLogHanlder() {
		return offsetLogHanlder;
	}

	public long getLatestProcessedOffsets(String topic,int partition){
    	return offsetLogHanlder != null ? offsetLogHanlder.getLatestProcessedOffsets(groupId, topic, partition) : -1;
    }

    public void saveOffsetsBeforeProcessed(String topic,int partition,long offset){
    	if(offsetLogHanlder != null){
    		offsetLogHanlder.saveOffsetsBeforeProcessed(groupId, topic, partition, offset);
    	}
    }
	
    public void saveOffsetsAfterProcessed(String topic,int partition,long offset){
        if(offsetLogHanlder != null){
        	offsetLogHanlder.saveOffsetsAfterProcessed(groupId, topic, partition, offset);
    	}
    }
}
