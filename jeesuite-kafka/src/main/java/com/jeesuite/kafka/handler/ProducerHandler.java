/**
 * 
 */
package com.jeesuite.kafka.handler;

import com.jeesuite.kafka.message.DefaultMessage;
import com.jeesuite.kafka.monitor.KafkaMonitor;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月22日
 */
public class ProducerHandler {

	public void onSuccessed(String topicName, DefaultMessage message) {
		//计数统计
		KafkaMonitor.getContext().updateProducerStat(topicName, false);
	}

	public void onError(String topicName, DefaultMessage message) {
		//计数统计
		KafkaMonitor.getContext().updateProducerStat(topicName, true);
	}
}
