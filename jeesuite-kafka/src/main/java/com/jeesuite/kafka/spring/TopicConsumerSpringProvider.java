package com.jeesuite.kafka.spring;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.jeesuite.kafka.consumer.NewApiTopicConsumer;
import com.jeesuite.kafka.consumer.OldApiTopicConsumer;
import com.jeesuite.kafka.consumer.TopicConsumer;
import com.jeesuite.kafka.handler.MessageHandler;
import com.jeesuite.kafka.serializer.MessageDeserializer;


/**
 * 消息订阅者集成spring封装对象
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月25日
 */
public class TopicConsumerSpringProvider implements InitializingBean, DisposableBean {
	
	private final static Logger logger = LoggerFactory.getLogger(TopicConsumerSpringProvider.class);

    private TopicConsumer consumer;

    /**
     * 配置
     */
    private Properties configs;
    
    //是否独立进程
    private boolean independent;
    
    private boolean useNewAPI = false;
    
    private Map<String, MessageHandler> topicHandlers;
    
    private int processThreads = 200;
    
    private String groupId;
    
    private String consumerId;
    
    //标记状态（0：未运行，1：启动中，2：运行中，3：停止中，4：重启中）
    private AtomicInteger status = new AtomicInteger(0);
    
	@Override
    public void afterPropertiesSet() throws Exception {
		
		Validate.isTrue(topicHandlers != null && topicHandlers.size() > 0, "at latest one topic");
		List<String> topics = new ArrayList<>(topicHandlers.keySet());
		//当前状态
		if(status.get() > 0)return;
		
		//make sure that rebalance.max.retries * rebalance.backoff.ms > zookeeper.session.timeout.ms.
		configs.put("rebalance.max.retries", "5");  
		configs.put("rebalance.backoff.ms", "1205"); 
		configs.put("zookeeper.session.timeout.ms", "6000"); 
		
		configs.put("key.deserializer",StringDeserializer.class.getName());  
		configs.put("value.deserializer",MessageDeserializer.class.getName());

		//同步节点信息
		groupId = configs.get(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG).toString();
		
		logger.info("\n===============KAFKA Consumer group[{}] begin start=================\n",groupId);
		
		try {
			consumerId = InetAddress.getLocalHost().getHostName() + "_" + RandomStringUtils.random(6, true, true).toLowerCase();
		} catch (Exception e) {
			consumerId = UUID.randomUUID().toString();
		}
		//
		configs.put("consumer.id", consumerId);
		
		//kafka 内部处理 consumerId ＝ groupId + "_" + consumerId
		consumerId = groupId + "_" + consumerId;
		//
    	start();
    	
    	logger.info("\n===============KAFKA Consumer group[{}],consumerId[{}] start finished!!=================\n",groupId,consumerId);
    }



	/**
	 * 启动
	 */
	private void start() {
		if (independent) {
			logger.info("KAFKA 启动模式[independent]");
			new Thread(new Runnable() {
				@Override
				public void run() {
					registerKafkaSubscriber();
				}
			}).start();
		} else {
			registerKafkaSubscriber();
		}
	}



	/**
	 * 
	 */
	@SuppressWarnings("rawtypes")
	private void registerKafkaSubscriber() {
		//状态：启动中
		status.set(1);
		
		Validate.notEmpty(this.configs, "configs is required");
		Validate.notEmpty(this.configs.getProperty("group.id"), "kafka configs[group.id] is required");
		Validate.notEmpty(this.configs.getProperty("bootstrap.servers"), "kafka configs[bootstrap.servers] is required");

        //configs.put("max.poll.records", "100");// It's not available in Kafka 0.9.0.1 version
        configs.put("max.partition.fetch.bytes", "131072");//128 kb
        configs.put("key.deserializer",StringDeserializer.class.getName());  
        configs.put("value.deserializer",MessageDeserializer.class.getName());
        
        StringBuffer sb = new StringBuffer();
        Iterator itr = this.configs.entrySet().iterator();
		while (itr.hasNext()) {
			Entry e = (Entry) itr.next();
			sb.append(e.getKey()).append("  =  ").append(e.getValue()).append("\n");
		}
		logger.info("\n============kafka.Consumer.Config============\n" + sb.toString() + "\n");

		if(useNewAPI){			
			consumer = new NewApiTopicConsumer(configs, topicHandlers,processThreads);
		}else{
			consumer = new OldApiTopicConsumer(configs, topicHandlers, processThreads);
		}

        consumer.start();
        //状态：运行中
        status.set(2);
	}
	

    /**
     * kafka 配置
     *
     * @param configs kafka 配置
     */
    public void setConfigs(Properties configs) {
        this.configs = configs;
    }

	public void setTopicHandlers(Map<String, MessageHandler> topicHandlers) {
		this.topicHandlers = topicHandlers;
	}

	public void setIndependent(boolean independent) {
		this.independent = independent;
	}

	public void setProcessThreads(int processThreads) {
		this.processThreads = processThreads;
	}

	public void setUseNewAPI(boolean useNewAPI) {
		this.useNewAPI = useNewAPI;
	}

	@Override
    public void destroy() throws Exception {
		consumer.close();
    }

}
