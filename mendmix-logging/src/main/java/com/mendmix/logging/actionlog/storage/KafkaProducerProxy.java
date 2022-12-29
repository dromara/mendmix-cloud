/*
 * Copyright 2016-2022 www.mendmix.com.
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
package com.mendmix.logging.actionlog.storage;

import java.util.Properties;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.util.JsonUtils;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.common.util.TokenGenerator;
import com.mendmix.logging.actionlog.ActionLog;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Dec 29, 2022
 */
public class KafkaProducerProxy {

	private static Logger logger = LoggerFactory.getLogger("com.mendmix.logging");

	private KafkaProducer<String, Object> kafkaProducer;

	private String topicName;
	
	private boolean available;
	
	protected KafkaProducerProxy() {
		init();
	}

	private void init() {
		this.topicName = ResourceUtils.getProperty("mendmix.actionlog.topicName", "mendmix-actionLog");
		String kafkaServers = ResourceUtils.getProperty("mendmix.actionlog.kafka.servers");
		Properties properties = new Properties();
		properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
		properties.setProperty(ProducerConfig.CLIENT_ID_CONFIG, "log-client-" + GlobalRuntimeContext.APPID);
		properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		properties.put(ProducerConfig.RETRIES_CONFIG, "1"); 
		try {			
			kafkaProducer = new KafkaProducer<>(properties);
			available = true;
			logger.info("LogKafkaClient start OK !!!!!! -> kafkaServers:{}",kafkaServers);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public boolean isAvailable() {
		return available;
	}

	public void send(ActionLog log) {
		Integer partition = null; //
		String key = log.getTraceId();
		if(key == null)key = TokenGenerator.generate();
		String value = JsonUtils.toJson(log);		
		ProducerRecord<String,Object> producerRecord = new ProducerRecord<String, Object>(topicName, partition, key, value);
		kafkaProducer.send(producerRecord, new Callback() {
			@Override
			public void onCompletion(RecordMetadata metadata, Exception exception) {
				if(exception != null) {
					logger.warn("kafka_send_actionlog_error:{}",exception.getMessage());
				}
			}
		});
	}
	
}
