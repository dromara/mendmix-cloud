package com.jeesuite.kafka.producer;

import java.io.Closeable;
import java.util.concurrent.Future;

import org.apache.commons.lang3.Validate;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.kafka.handler.ProducerResultHandler;
import com.jeesuite.kafka.message.DefaultMessage;

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
    
    private ProducerResultHandler producerResultHanlder;
   
    public DefaultTopicProducer(KafkaProducer<String, Object> kafkaProducer,boolean defaultAsynSend) {
    	this.kafkaProducer = kafkaProducer;
    	this.producerResultHanlder = new ProducerResultHandler(this);
    }
	
    public boolean publish(final String topicName, final DefaultMessage message,boolean asynSend){
        Validate.notNull(topicName, "Topic is required");

        Validate.notNull(message, "Message is required");
        //异步
        if(asynSend){
        	try {				
        		doAsynSend(topicName, message.getMsgId(),message);
			} catch (Exception e) {
				producerResultHanlder.onError(topicName,message,asynSend);
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
			producerResultHanlder.onSuccessed(topicName, message);
			if (log.isDebugEnabled()) {
                log.debug("kafka_send_success,topic=" + topicName + ", messageId=" + messageKey + ", partition=" + metadata.partition() + ", offset=" + metadata.offset());
            }
			return true;
		} catch (Exception ex) {
			producerResultHanlder.onError(topicName,message,false);
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
                	producerResultHanlder.onError(topicName,message,true);
                	log.error("kafka_send_fail,topic="+topicName+",messageId="+messageKey,ex);
                } else {
                	producerResultHanlder.onSuccessed(topicName, message);
                    if (log.isDebugEnabled()) {
                        log.debug("kafka_send_success,topic=" + topicName + ", messageId=" + messageKey + ", partition=" + metadata.partition() + ", offset=" + metadata.offset());
                    }
                }
            }
        });
	}
	
    public void close() {
        this.kafkaProducer.close();
        producerResultHanlder.close();
    }
    
}
