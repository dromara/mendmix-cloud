/**
 * 
 */
package com.jeesuite.kafka.consumer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

	private static ConsumerContext instance = new ConsumerContext();

	private String groupId;
	
	private String consumerId;
	
	
	private Properties configs;
	
	private Map<String, MessageHandler> messageHandlers;
	
	private int maxProcessThreads;
	
	private ErrorMessageProcessor errorMessageProcessor;
	private OffsetLogHanlder offsetLogHanlder;
	
	private ZkClient zkClient;
	
	private AtomicBoolean fetchEnabled = new AtomicBoolean(true);
	//<topic,[fetchNums,processNums]>
	private Map<String, AtomicInteger[]> consumerStats = new ConcurrentHashMap<>();
	
	public static ConsumerContext getInstance() {
		return instance;
	}

	private ConsumerContext(){}
	
	public void propertiesSetIfAbsent(Properties configs, String groupId, String consumerId,
			Map<String, MessageHandler> messageHandlers, int maxProcessThreads,
			OffsetLogHanlder offsetLogHanlder,ErrorMessageProcessor errorMessageProcessor) {
		if(this.configs != null)return;
		this.configs = configs;
		this.groupId = groupId;
		this.consumerId = consumerId;
		this.messageHandlers = messageHandlers;
		this.maxProcessThreads = maxProcessThreads;
		this.offsetLogHanlder = offsetLogHanlder;
		this.errorMessageProcessor = errorMessageProcessor;
		
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

	protected Properties getProperties() {
		return configs;
	}

	protected Map<String, MessageHandler> getMessageHandlers() {
		return messageHandlers;
	}

	protected int getMaxProcessThreads() {
		return maxProcessThreads;
	}

	protected OffsetLogHanlder getOffsetLogHanlder() {
		return offsetLogHanlder;
	}

	public long getLatestProcessedOffsets(String topic,int partition){
    	return offsetLogHanlder != null ? offsetLogHanlder.getLatestProcessedOffsets(groupId, topic, partition) : -1;
    }

	protected void saveOffsetsBeforeProcessed(String topic,int partition,long offset){
    	if(offsetLogHanlder != null){
    		offsetLogHanlder.saveOffsetsBeforeProcessed(groupId, topic, partition, offset);
    	}
    }
	
    protected void saveOffsetsAfterProcessed(String topic,int partition,long offset){
        if(offsetLogHanlder != null){
        	offsetLogHanlder.saveOffsetsAfterProcessed(groupId, topic, partition, offset);
    	}
    }
    
    public void processErrorMessage(String topic,DefaultMessage message){
    	errorMessageProcessor.submit(message, messageHandlers.get(topic));
    }
    
    protected void sendConsumerAck(String messageId){
    	if(zkClient == null){
    		log.warn("Message set consumerAck = true,but not zookeeper config[kafka.zkServers] found!!!");
    		return;
    	}
    	String path = KafkaConst.ZK_PRODUCER_ACK_PATH + messageId;
    	try {			
			zkClient.writeData(path, groupId);
		} catch (org.I0Itec.zkclient.exception.ZkNoNodeException e) {
			// do nothing
		}catch (Exception e) {
			log.warn("sendConsumerAck error",e);
		}
    }
    
    protected void updateConsumerStats(String topic,int nums){
    	if(nums == 0){
    		consumerStats.put(topic, new AtomicInteger[]{new AtomicInteger(0),new AtomicInteger(0) });
    	}else if(nums > 0){
    		consumerStats.get(topic)[0].addAndGet(nums);
    	}else{
    		consumerStats.get(topic)[1].addAndGet(Math.abs(nums));
    	}
    }
    
    public boolean fetchEnabled() {
		return fetchEnabled.get();
	}

	public void switchFetch(boolean enable){
		log.info(">set_kafka_cosumer_fetch:{}",enable);
		fetchEnabled.set(enable);
	}
	
	public Map<String, int[]> getConsumerStats(){
		Map<String, int[]> result =  new HashMap<>();
		consumerStats.forEach((k,v) -> {
			result.put(k, new int[]{v[0].get(),v[1].get()});
		});
		return result;
	}
    
	protected void close(){
    	errorMessageProcessor.close();
    	if(zkClient != null)zkClient.close();
    }
}
