/*
 * Copyright 2016-2020 www.jeesuite.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dromara.mendmix.logging;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.dromara.mendmix.cache.CacheUtils;
import org.dromara.mendmix.common.util.ExceptionFormatUtils;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * 
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2020年4月23日
 */
public class LogKafkaClient implements InitializingBean,DisposableBean {

	private KafkaProducer<String, String> kafkaProducer;
    
	private String kafkaServers;
	
	public LogKafkaClient(String kafkaServers) {
		this.kafkaServers = kafkaServers;
	}

	public void send(String topic,String logJson) {
		ProducerRecord<String,String> producerRecord = new ProducerRecord<>(topic, logJson);
		kafkaProducer.send(producerRecord, new Callback(){
			@Override
			public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                if (e != null && CacheUtils.setIfAbsent("kafka_send_error_counter", "1", 30, TimeUnit.SECONDS)) {
                	System.err.println("send_log_error:" + ExceptionFormatUtils.buildExceptionMessages(e, 2));
                }
			}
		});
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		Properties props = new Properties();
		props.putAll(ResourceUtils.getMappingValues("mendmix-cloud.logging.loghandle.kafka"));
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
		if (!props.containsKey(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)) {
			props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()); // key serializer
		}
		if (!props.containsKey(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)) {
			props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		}
		//关闭重试
		if (!props.containsKey(ProducerConfig.RETRIES_CONFIG)) {			
			props.setProperty(ProducerConfig.RETRIES_CONFIG, "0");
		}
		//立即返回，不需要等待leader的任何确认
        if (!props.containsKey(ProducerConfig.ACKS_CONFIG)) {			
        	props.setProperty(ProducerConfig.ACKS_CONFIG, "0");
		}
        //压缩算法，支持none（不压缩），gzip，snappy和lz4
        if (!props.containsKey(ProducerConfig.COMPRESSION_TYPE_CONFIG)) {
        	props.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
		}
		//缓存消息的缓冲区大小，单位为字节，默认值为：33554432 (32M)
        if (!props.containsKey(ProducerConfig.BUFFER_MEMORY_CONFIG)) {
        	props.setProperty(ProducerConfig.BUFFER_MEMORY_CONFIG, "33554432");
		}
		//申请空闲内存等待时间，默认为60s，超时报TimeoutException
        if (!props.containsKey(ProducerConfig.MAX_BLOCK_MS_CONFIG)) {			
        	props.setProperty(ProducerConfig.MAX_BLOCK_MS_CONFIG, "60000");
		}
		//每一个批次的内存大小,默认为16K
        if (!props.containsKey(ProducerConfig.BATCH_SIZE_CONFIG)) {
        	props.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, "16384");
		}
		//等待时间如果批次没满也将触发发送，默认为：0
        if (!props.containsKey(ProducerConfig.LINGER_MS_CONFIG)) {			
        	props.setProperty(ProducerConfig.LINGER_MS_CONFIG, "5");
		}
		//设置每一个客户端与服务端连接，在应用层一个通道的积压消息数量，默认为5
        if (!props.containsKey(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION)) {			
        	props.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");
		}
		
		kafkaProducer = new KafkaProducer<>(props);
	}

	@Override
	public void destroy() throws Exception {
		kafkaProducer.close();
	}

}
