package com.jeesuite.kafka.producer;

import java.io.Serializable;

import com.jeesuite.kafka.message.DefaultMessage;
import com.jeesuite.kafka.producer.handler.ProducerEventHandler;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月14日
 */
public interface TopicProducer {
	
	void addEventHandler(ProducerEventHandler eventHandler);
	/**
	 * 发送消息（可选择发送模式）
	 * @param topic
	 * @param message
	 * @param asynSend 是否异步发送
	 * @return
	 */
	boolean publish(final String topic, final DefaultMessage message,boolean asynSend);
	
	/**
	 * 发送任意消息对象（兼容非配套使用的consumer端）
	 * @param topic
	 * @param message
	 * @param asynSend
	 * @return
	 */
	boolean publishNoWrapperObject(final String topic, final Serializable message,boolean asynSend);
	
	void close();
}
