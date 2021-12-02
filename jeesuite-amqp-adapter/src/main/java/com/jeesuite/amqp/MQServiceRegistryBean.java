package com.jeesuite.amqp;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

import com.jeesuite.amqp.memoryqueue.MemoryQueueProducerAdapter;
import com.jeesuite.amqp.qcloud.cmq.CMQConsumerAdapter;
import com.jeesuite.amqp.qcloud.cmq.CMQProducerAdapter;
import com.jeesuite.amqp.rocketmq.RocketProducerAdapter;
import com.jeesuite.amqp.rocketmq.RocketmqConsumerAdapter;
import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.spring.InstanceFactory;
import com.jeesuite.spring.SpringInstanceProvider;
import com.jeesuite.spring.helper.SpringAopHelper;

public class MQServiceRegistryBean implements InitializingBean,DisposableBean,ApplicationContextAware,PriorityOrdered {

	protected  static final Logger logger = LoggerFactory.getLogger("com.jeesuite.amqp");
	
	private ApplicationContext applicationContext;
	private MQConsumer consumer;
	private MQProducer producer;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		
		String providerName = MQContext.getProviderName();
		//
		if(MQContext.isProducerEnabled()){			
			startProducer(providerName);
			MQInstanceDelegate.setProducer(producer);
		}
		//
		if(MQContext.isConsumerEnabled()){			
			startConsumer(providerName);
		}
	}
	
    private void startProducer(String providerName) throws Exception{
    	
		if("rocketmq".equals(providerName)){
			producer = new RocketProducerAdapter();
		}else if("cmq".equals(providerName)){
			producer = new CMQProducerAdapter();
		}else if("memoryqueue".equals(providerName)){
			producer = new MemoryQueueProducerAdapter();
		}else{
			throw new JeesuiteBaseException("NOT_SUPPORT[providerName]:" + providerName);
		}
		producer.start();
		logger.info("MQ_PRODUCER started -> groupName:{},providerName:{}",MQContext.getGroupName(),providerName);
	}
    
    private void startConsumer(String providerName) throws Exception{
		Map<String, MessageHandler> messageHanlders = applicationContext.getBeansOfType(MessageHandler.class);
		if(messageHanlders != null && !messageHanlders.isEmpty()){
			Map<String, MessageHandler> messageHandlerMaps = new HashMap<>(); 
			messageHanlders.values().forEach(e -> {
				Object origin = e;
				try {origin = SpringAopHelper.getTarget(e);} catch (Exception ex) {ex.printStackTrace();}
				MQTopicRef topicRef = origin.getClass().getAnnotation(MQTopicRef.class);
				String topicName = MQContext.rebuildWithNamespace(topicRef.value());
				messageHandlerMaps.put(topicName, e);
				logger.info("ADD MQ_COMSUMER_HANDLER ->topic:{},handlerClass:{} ",topicName,e.getClass().getName());
			});
			
			if("rocketmq".equals(providerName)){
				consumer = new RocketmqConsumerAdapter(messageHandlerMaps);
			}else if("cmq".equals(providerName)){
				consumer = new CMQConsumerAdapter(messageHandlerMaps);
			}else if("memoryqueue".equals(providerName)){
				MemoryQueueProducerAdapter.setMessageHandlers(messageHandlerMaps);
			}else{
				throw new JeesuiteBaseException("NOT_SUPPORT[providerName]:" + providerName);
			}
			
			consumer.start();
			logger.info("MQ_COMSUMER started -> groupName:{},providerName:{}",MQContext.getGroupName(),providerName);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
		InstanceFactory.setInstanceProvider(new SpringInstanceProvider(applicationContext));
	}
	
	@Override
	public void destroy() throws Exception {
		if(consumer != null) {
			consumer.shutdown();
		}
		if(producer != null) {
			producer.shutdown();
		}
		MQContext.close();
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

}
