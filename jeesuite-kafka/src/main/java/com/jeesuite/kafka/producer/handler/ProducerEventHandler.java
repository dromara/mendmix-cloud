/**
 * 
 */
package com.jeesuite.kafka.producer.handler;

import java.io.Closeable;

import org.apache.kafka.clients.producer.RecordMetadata;

import com.jeesuite.kafka.message.DefaultMessage;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月10日
 */
public interface ProducerEventHandler extends Closeable{

	public void onSuccessed(String topicName, RecordMetadata metadata);

	public void onError(String topicName, DefaultMessage message);

}
