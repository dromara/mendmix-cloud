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
package com.mendmix.amqp.adapter;

import com.mendmix.amqp.MQContext;
import com.mendmix.amqp.MQContext.ActionType;
import com.mendmix.amqp.MQMessage;
import com.mendmix.amqp.MQProducer;

public abstract class AbstractProducer implements MQProducer {
	
	@Override
	public void start() throws Exception {}


	@Override
	public void shutdown() {}
	
	
	public void handleSuccess(MQMessage message) {
		MQContext.processMessageLog(message,ActionType.pub, null);
	}

	public void handleError(MQMessage message, Throwable e) {
		MQContext.processMessageLog(message,ActionType.pub, e);
	}

}
