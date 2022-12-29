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

import org.springframework.beans.factory.InitializingBean;

import com.mendmix.common.util.ResourceUtils;
import com.mendmix.logging.actionlog.ActionLog;
import com.mendmix.logging.actionlog.LogStorageProvider;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Dec 29, 2022
 */
public abstract class AbstractStorageProvider implements LogStorageProvider ,InitializingBean {

	private KafkaProducerProxy kafkaProxy;

	protected boolean tryKafkaSend(ActionLog actionLog) {
		if(kafkaProxy == null || !kafkaProxy.isAvailable())return false;
		kafkaProxy.send(actionLog);
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		try {
			Class.forName("org.apache.kafka.clients.producer.KafkaProducer");
			if(ResourceUtils.containsProperty("mendmix.actionlog.kafka.servers")) {
				kafkaProxy = new KafkaProducerProxy();
			}
		} catch (ClassNotFoundException e) {}
	}

}
