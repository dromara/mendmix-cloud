/**
 * 
 */
package com.jeesuite.kafka.consumer;

import java.util.Map;
import java.util.Properties;

import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.kafka.KafkaConst;
import com.jeesuite.kafka.consumer.hanlder.OffsetLogHanlder;
import com.jeesuite.kafka.handler.MessageHandler;
import com.jeesuite.kafka.message.DefaultMessage;
import com.jeesuite.kafka.serializer.ZKStringSerializer;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年2月10日
 */
public class ConsumerContext {
	
	private static final Logger log = LoggerFactory.getLogger(ConsumerContext.class);


	private String groupId;
	
	private String consumerId;
	
	
	private Properties configs;
	
	private Map<String, MessageHandler> messageHandlers;
	
	private int maxProcessThreads;
	
	private ErrorMessageProcessor errorMessageProcessor;
	private OffsetLogHanlder offsetLogHanlder;
	
	private ZkClient zkClient;
	
	public ConsumerContext(Properties configs, String groupId, String consumerId,
			Map<String, MessageHandler> messageHandlers, int maxProcessThreads) {
		super();
		this.configs = configs;
		this.groupId = groupId;
		this.consumerId = consumerId;
		this.messageHandlers = messageHandlers;
		this.maxProcessThreads = maxProcessThreads;
		
		String zkServers = ResourceUtils.getProperty("kafka.zkServers");
		if(StringUtils.isNotBlank(zkServers)){
        	zkClient = new ZkClient(zkServers, 10000, 5000, new ZKStringSerializer());
		}
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
	
    public void setErrorMessageProcessor(ErrorMessageProcessor errorMessageProcessor) {
		this.errorMessageProcessor = errorMessageProcessor;
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
    
    public void processErrorMessage(String topic,DefaultMessage message){
    	message.setTopic(topic);
    	errorMessageProcessor.submit(message, messageHandlers.get(topic));
    }
    
    public void sendConsumerAck(String messageId){
    	if(zkClient == null){
    		log.warn("Message set consumerAck = true,but not zookeeper client config[kafka.zkServers] found!!!");
    		return;
    	}
    	String path = KafkaConst.ZK_PRODUCER_ACK_PATH + messageId;
    	if(!zkClient.exists(path))return;
    	zkClient.writeData(path, groupId);
    }
    
    public void close(){
    	errorMessageProcessor.close();
    	if(zkClient != null)zkClient.close();
    }
}
