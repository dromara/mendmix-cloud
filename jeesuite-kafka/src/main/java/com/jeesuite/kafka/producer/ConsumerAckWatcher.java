package com.jeesuite.kafka.producer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.kafka.KafkaConst;

/**
 * 消费回执观察者
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年11月27日
 */
public class ConsumerAckWatcher {

	private static final Logger log = LoggerFactory.getLogger(ConsumerAckWatcher.class);

	private CountDownLatch latch;
	
	public ConsumerAckWatcher(String messageId, ZkClient zkClient) {
		this.latch = new CountDownLatch(1);
		
		String path = KafkaConst.ZK_PRODUCER_ACK_PATH + messageId;
		zkClient.createEphemeral(path);
		
		zkClient.subscribeDataChanges(path, new IZkDataListener() {
			@Override
			public void handleDataDeleted(String dataPath) throws Exception {}
			@Override
			public void handleDataChange(String dataPath, Object data) throws Exception {
				latch.countDown();
				log.debug("receive message[{}] consumer ack from group:{}",messageId,data);
				try {zkClient.delete(dataPath);} catch (Exception e) {}
			}
		});
	}
	
	
	public void waitAck(){
		try {
			this.latch.await(10000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			
		} 
	}
	
}
