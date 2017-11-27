package com.jeesuite.kafka.producer;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.lang3.Validate;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.kafka.KafkaConst;
import com.jeesuite.kafka.message.DefaultMessage;
import com.jeesuite.kafka.producer.handler.ProducerEventHandler;

/**
 * 默认消息生产者实现
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月14日
 */
public class DefaultTopicProducer implements TopicProducer,Closeable{

    private static final Logger log = LoggerFactory.getLogger(DefaultTopicProducer.class);

    /**
     * KafkaProducer
     */
    private KafkaProducer<String, Object> kafkaProducer;
    
    private List<ProducerEventHandler> eventHanlders = new ArrayList<>();
    
    private ZkClient zkClient;
    
    private boolean consumerAckEnabled;
   
    public DefaultTopicProducer(KafkaProducer<String, Object> kafkaProducer,ZkClient zkClient,boolean consumerAckEnabled) {
    	this.kafkaProducer = kafkaProducer;
    	this.zkClient = zkClient;
    	this.consumerAckEnabled = consumerAckEnabled;
    	if(this.zkClient != null && this.consumerAckEnabled){
    		String rootPath = KafkaConst.ZK_PRODUCER_ACK_PATH.substring(0, KafkaConst.ZK_PRODUCER_ACK_PATH.length() - 1);
    		if(!this.zkClient.exists(rootPath)){
    			this.zkClient.createPersistent(rootPath);
    		}
    		log.info("consumer watcher rootPath:{}",rootPath);
    	}
    }
    
    public void addEventHandler(ProducerEventHandler eventHandler){
    	eventHanlders.add(eventHandler);
    }
    
    public boolean publish(final String topicName, final DefaultMessage message,boolean asynSend){
        Validate.notNull(topicName, "Topic is required");

        Validate.notNull(message, "Message is required");
        //
        boolean requireAck = consumerAckEnabled && message.isConsumerAck();
        //异步 ，如果需要回执强制同步发送
        if(asynSend && requireAck == false){
        	try {				
        		doAsynSend(topicName, message.getMsgId(),message);
			} catch (Exception e) {
	        	log.error("kafka_send_fail,topic="+topicName+",messageId="+message.getMsgId(),e);
	        	//同步发送直接抛异常
	        	throw new RuntimeException(e);
			}
        }else{        	
        	doSyncSend(topicName, message.getMsgId(), message);
        }
        
      //等待回执
		if(requireAck){
			if(zkClient == null){
				log.warn("Message set consumerAck = true,but not zookeeper client config[kafka.zkServers] found!!!");
			}else{
				log.debug("wait_consumer_ack,messageId:{}",message.getMsgId());
				new ConsumerAckWatcher(message.getMsgId(), zkClient).waitAck();
			}
		}
        
        return true;
    
	}

	private boolean doSyncSend(String topicName, String messageKey,DefaultMessage message){
		try {			
			Future<RecordMetadata> future = kafkaProducer.send(new ProducerRecord<String, Object>(topicName, messageKey,message.isSendBodyOnly() ? message.getBody() : message));
			RecordMetadata metadata = future.get();
			for (ProducerEventHandler handler : eventHanlders) {
				try {handler.onSuccessed(topicName, metadata);} catch (Exception e) {}
			}
			if (log.isDebugEnabled()) {
                log.debug("kafka_send_success,topic=" + topicName + ", messageId=" + messageKey + ", partition=" + metadata.partition() + ", offset=" + metadata.offset());
            }
			return true;
		} catch (Exception ex) {
        	log.error("kafka_send_fail,topic="+topicName+",messageId="+messageKey,ex);
        	//同步发送直接抛异常
        	throw new RuntimeException(ex);
		}
	}

	/**
	 * 异步发送消息
	 * @param topicName
	 * @param messageKey
	 * @param message
	 * @param currentCount 当前执行次数
	 */
	private void doAsynSend(final String topicName, final String messageKey,final DefaultMessage message) {
		// 异步发送
        this.kafkaProducer.send(new ProducerRecord<String, Object>(topicName, messageKey,message.isSendBodyOnly() ? message.getBody() : message), new Callback() {

            @Override
            public void onCompletion(RecordMetadata metadata, Exception ex) {
                if (ex != null) {
                	for (ProducerEventHandler handler : eventHanlders) {
                		try {handler.onError(topicName, message, true);} catch (Exception e) {}
    				}
                	log.error("kafka_send_fail,topic="+topicName+",messageId="+messageKey,ex);
                } else {
                	for (ProducerEventHandler handler : eventHanlders) {
                		try {handler.onSuccessed(topicName, metadata);} catch (Exception e) {}
    				}
                    if (log.isDebugEnabled()) {
                        log.debug("kafka_send_success,topic=" + topicName + ", messageId=" + messageKey + ", partition=" + metadata.partition() + ", offset=" + metadata.offset());
                    }
                }
            }
        });
	}
	
    public void close() {
        this.kafkaProducer.close();
        for (ProducerEventHandler handler : eventHanlders) {
        	try {
        		handler.close();				
			} catch (Exception e) {}
		}
    }

}
