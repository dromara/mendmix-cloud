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
package com.mendmix.amqp.adapter.qcloud.cmq;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.mendmix.amqp.MQMessage;
import com.mendmix.amqp.adapter.AbstractProducer;
import com.qcloud.cmq.Topic;

/**
 * 
 * <br>
 * Class Name : CMQProducer
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年10月28日
 */
public class CMQProducerAdapter extends AbstractProducer {

	@Override
	public String sendMessage(MQMessage message, boolean async) {
		try {
			Topic topic = CMQManager.createTopicIfAbsent(message.getTopic());
			//发送返回的msgId与消费的msgId不一致 ，这里把msgId放消息体
			String msgId = StringUtils.replace(UUID.randomUUID().toString(), "-", StringUtils.EMPTY);
			message.setMsgId(msgId);
			topic.publishMessage(message.toMessageValue(false));
			//
			handleSuccess(message);
			return msgId;
		} catch (Exception e) {
			handleError(message, e);
			throw new RuntimeException("cmq_error", e);
		}
	}

}
