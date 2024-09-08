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
package org.dromara.mendmix.logging.autoconfigure;

import org.dromara.mendmix.logging.LogKafkaClient;
import org.dromara.mendmix.logging.tracelog.processor.KafkaApmLogProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2021年9月7日
 */
@Configuration
public class LoggingSupportConfiguration {
	
	@Bean
	@ConditionalOnProperty(name = "mendmix-cloud.logging.loghandle.kafka.servers")
	@ConditionalOnClass(name = "org.apache.kafka.clients.producer.KafkaProducer")
	public LogKafkaClient logKafkaClient(@Value("${application.apm.loghandle.kafka.servers}") String servers) {
		return new LogKafkaClient(servers);
	}
	
	@Bean
	@ConditionalOnProperty(name = "mendmix-cloud.logging.tracelog.enabled",havingValue = "true")
	@ConditionalOnBean(LogKafkaClient.class)
	public KafkaApmLogProcessor kafkaApmLogProcessor(@Autowired LogKafkaClient kafkaClient) {
		return new KafkaApmLogProcessor(kafkaClient);
	}

}
