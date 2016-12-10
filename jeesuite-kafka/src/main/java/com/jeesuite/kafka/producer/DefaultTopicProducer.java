package com.jeesuite.kafka.producer;

import java.io.Closeable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

import org.apache.commons.lang3.Validate;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
   
    public DefaultTopicProducer(KafkaProducer<String, Object> kafkaProducer,boolean defaultAsynSend) {
    	this.kafkaProducer = kafkaProducer;
    }
    
    public void addEventHandler(ProducerEventHandler eventHandler){
    	eventHanlders.add(eventHandler);
    }
    
    public boolean publish(final String topicName, final DefaultMessage message,boolean asynSend){
        Validate.notNull(topicName, "Topic is required");

        Validate.notNull(message, "Message is required");
        //异步
        if(asynSend){
        	try {				
        		doAsynSend(topicName, message.getMsgId(),message);
			} catch (Exception e) {
				for (ProducerEventHandler handler : eventHanlders) {
					handler.onError(topicName, message, asynSend);
				}
	        	log.error("kafka_send_fail,topic="+topicName+",messageId="+message.getMsgId(),e);
				return false;
			}
        	return true;
        }else{        	
        	return doSyncSend(topicName, message.getMsgId(), message);
        }
    
	}

	private boolean doSyncSend(String topicName, String messageKey,DefaultMessage message){
		try {			
			Future<RecordMetadata> future = kafkaProducer.send(new ProducerRecord<String, Object>(topicName, messageKey,message));
			RecordMetadata metadata = future.get();
			for (ProducerEventHandler handler : eventHanlders) {
				handler.onSuccessed(topicName, metadata);
			}
			if (log.isDebugEnabled()) {
                log.debug("kafka_send_success,topic=" + topicName + ", messageId=" + messageKey + ", partition=" + metadata.partition() + ", offset=" + metadata.offset());
            }
			return true;
		} catch (Exception ex) {
			for (ProducerEventHandler handler : eventHanlders) {
				handler.onError(topicName, message, false);
			}
        	log.error("kafka_send_fail,topic="+topicName+",messageId="+messageKey,ex);
        	return false;
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
        this.kafkaProducer.send(new ProducerRecord<String, Object>(topicName, messageKey,message), new Callback() {

            @Override
            public void onCompletion(RecordMetadata metadata, Exception ex) {
                if (ex != null) {
                	for (ProducerEventHandler handler : eventHanlders) {
    					handler.onError(topicName, message, true);
    				}
                	log.error("kafka_send_fail,topic="+topicName+",messageId="+messageKey,ex);
                } else {
                	for (ProducerEventHandler handler : eventHanlders) {
    					handler.onSuccessed(topicName, metadata);
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


	@Override
	public boolean publishNoWrapperObject(final String topic, final Serializable message, boolean asynSend) {

		final String messageKey = UUID.randomUUID().toString();
		
		if(asynSend){
			// 异步发送
	        this.kafkaProducer.send(new ProducerRecord<String, Object>(topic, messageKey,message), new Callback() {
	            @Override
	            public void onCompletion(RecordMetadata metadata, Exception ex) {
	                if (ex != null) {
	                	log.error("kafka_send_fail,topic="+topic+",messageId="+messageKey,ex);
	                } else {
	                    if (log.isDebugEnabled()) {
	                        log.debug("kafka_send_success,topic=" + topic + ", messageId=" + messageKey + ", partition=" + metadata.partition() + ", offset=" + metadata.offset());
	                    }
	                }
	            }
	        });
	        return true;
		}else{
			try {			
				Future<RecordMetadata> future = kafkaProducer.send(new ProducerRecord<String, Object>(topic, messageKey,message));
				RecordMetadata metadata = future.get();
				if (log.isDebugEnabled()) {
	                log.debug("kafka_send_success,topic=" + topic + ", messageId=" + messageKey + ", partition=" + metadata.partition() + ", offset=" + metadata.offset());
	            }
				return true;
			} catch (Exception ex) {
	        	log.error("kafka_send_fail,topic="+topic+",messageId="+messageKey,ex);
	        	throw new RuntimeException(ex);
			}
		}

	}
    
}
