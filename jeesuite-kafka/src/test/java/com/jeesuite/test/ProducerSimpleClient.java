package com.jeesuite.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.jeesuite.kafka.message.DefaultMessage;
import com.jeesuite.kafka.spring.TopicProducerSpringProvider;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-kafka-producer.xml")
public class ProducerSimpleClient implements ApplicationContextAware{
	
	@Autowired
	private TopicProducerSpringProvider topicProducer;
	
	@Test
	public void testPublish() throws InterruptedException{

		for (int i = 0; i < 3; i++) {			
			topicProducer.publish("demo-topic", new DefaultMessage("demo-topic:" + i));
			topicProducer.publish("demo2-topic", new DefaultMessage("demo2-topic:" + i));
		}
	}
	
	
	@Override
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {}

}
