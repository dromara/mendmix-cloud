package com.jeesuite.test.kafka;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.jeesuite.kafka.message.DefaultMessage;
import com.jeesuite.kafka.monitor.KafkaMonitor;
import com.jeesuite.kafka.spring.TopicProducerSpringProvider;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-kafka-producer.xml")
public class ProducerClient implements ApplicationContextAware{
	
	@Autowired
	private TopicProducerSpringProvider topicProducer;
	
	@Test
	public void testPublish() throws InterruptedException{
		
		final ExecutorService pool = Executors.newFixedThreadPool(3);
		Timer timer = new Timer(true);
		
		AtomicInteger count = new AtomicInteger(0);
		
		int nums = 10;
		timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				if(count.get() == nums){
					timer.cancel();
					return;
				}
				for (int i = 0; i < 1; i++) {
					
					pool.submit(new Runnable() {			
						@Override
						public void run() {
                            topicProducer.publish("demo-topic", new DefaultMessage(RandomStringUtils.random(5, true, true)));
							count.incrementAndGet();
						}
					});
				}
			}
		}, 3000, 200);
		
		while(true){
			if(count.get() >= nums){
				System.out.println(">>>>>>send count:"+count.get());
				break;
			}
		}
		
		Map stats = KafkaMonitor.getContext().stats(null);
        System.out.println(stats);
		pool.shutdown();
	}
	
	
	@Override
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {}

}
