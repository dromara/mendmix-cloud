/**
 * 
 */
package com.jeesuite.kafka.producer;

/**
 * 消息已消费回执
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年10月28日
 */
public interface ComsumerAckHandler {

	int ack(String msgId);
}
