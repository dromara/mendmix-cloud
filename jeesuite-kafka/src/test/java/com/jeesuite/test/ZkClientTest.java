package com.jeesuite.test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.lang3.RandomStringUtils;

import com.jeesuite.kafka.KafkaConst;
import com.jeesuite.kafka.serializer.ZKStringSerializer;

public class ZkClientTest {

	
	public static void main(String[] args) throws Exception {
		ZkClient zkClient = new ZkClient("127.0.0.1:2181", 10000, 5000, new ZKStringSerializer());
		
		ExecutorService service = Executors.newFixedThreadPool(5);
		
		for (int i = 0; i < 100; i++) {
			service.execute(new Runnable() {
				@Override
				public void run() {
					String path = KafkaConst.ZK_PRODUCER_ACK_PATH + RandomStringUtils.random(5, true, true);
					zkClient.createEphemeral(path);
					System.out.println(path);
				}
			});
			if(i % 5 == 0){
				Thread.sleep(500);
				System.out.println("Sleep");
			}
		}
		
	}
}
