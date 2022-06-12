/*
 * Copyright 2016-2020 www.mendmix.com.
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
package com.mendmix.amqp.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.mendmix.amqp.AbstractProducer;
import com.mendmix.amqp.MQMessage;
import com.mendmix.cache.RedisTemplateGroups;

/**
 * 
 * <br>
 * Class Name   : RedisProducerAdapter
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2017年7月11日
 */
public class RedisProducerAdapter extends AbstractProducer {

	private final Logger logger = LoggerFactory.getLogger("com.zvosframework.adapter.amqp");

	private StringRedisTemplate redisTemplate;
	
	public RedisProducerAdapter() {
		
	}

	@Override
	public void start() throws Exception{
		super.start();
	}
	
	@Override
	public String sendMessage(MQMessage message,boolean async) {
		try {
			if(redisTemplate == null) {
				redisTemplate = RedisTemplateGroups.getDefaultStringRedisTemplate();
			}
			redisTemplate.convertAndSend(message.getTopic(), message.toMessageValue(false));
			handleSuccess(message);
		} catch (Exception e) {
			handleError(message, e);
			logger.warn("MENDMIX-TRACE-LOGGGING-->> MQ_SEND_FAIL:"+message.getTopic(),e);
		}
		
		return null;
	}

	@Override
	public void shutdown() {
		super.shutdown();
	}

	

}
