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
package com.mendmix.amqp.adapter.memoryqueue;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.mendmix.amqp.MQMessage;
import com.mendmix.amqp.MQProducer;
import com.mendmix.amqp.MessageHandler;
import com.mendmix.common.CurrentRuntimeContext;

/**
 * 
 * <br>
 * Class Name : EventbusProducerAdapter
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年3月24日
 */
public class EventbusProducerAdapter implements MQProducer {

	private final static Logger logger = LoggerFactory.getLogger("com.mendmix.amqp.adapter");

	private final static Translator TRANSLATOR = new Translator();
	private static EventHandler<MQMessageEvent> eventHandler = new MQMessageEventHandler();
	private static Map<String, MessageHandler> messageHandlers = new HashMap<>(); 
	
	private Disruptor<MQMessageEvent> disruptor;
	
	@Override
	public void start() throws Exception {
		EventFactory<MQMessageEvent> eventFactory = new MQMessageEventFactory();
		int ringBufferSize = 1024 * 1024;
		BasicThreadFactory factory = new BasicThreadFactory.Builder()
				      .namingPattern("workerthread-%d")
				      .daemon(true)
				      .priority(Thread.MAX_PRIORITY)
				      .build();
		disruptor = new Disruptor<MQMessageEvent>(eventFactory,
		                ringBufferSize, factory, ProducerType.SINGLE,
		                new YieldingWaitStrategy());
		
		disruptor.handleEventsWith(eventHandler);
		
		disruptor.start();
	}

	@Override
	public String sendMessage(MQMessage message, boolean async) {
		message.mergeContextHeaders();
		RingBuffer<MQMessageEvent> ringBuffer = disruptor.getRingBuffer();
	    ringBuffer.publishEvent(TRANSLATOR, message);
		return null;
	}

	@Override
	public void shutdown() {
		disruptor.shutdown();
	}
	
	public static void setMessageHandlers(Map<String, MessageHandler> messageHandlers) {
		EventbusProducerAdapter.messageHandlers = messageHandlers;
	}

	static class MQMessageEvent {
		private MQMessage value;
		public void set(MQMessage value) {
			this.value = value;
		}
		public MQMessage get() {
			return value;
		}
	}

	static class MQMessageEventFactory implements EventFactory<MQMessageEvent> {
		public MQMessageEvent newInstance() {
			return new MQMessageEvent();
		}
	}

	static class MQMessageEventHandler implements EventHandler<MQMessageEvent> {
		public void onEvent(MQMessageEvent event, long sequence, boolean endOfBatch) {
			MQMessage message = event.get();
			//上下文
			if(message.getHeaders() != null) {	
				CurrentRuntimeContext.addContextHeaders(message.getHeaders());
			}
			MessageHandler handler = messageHandlers.get(message.getTopic());
			if(handler != null){
				try {
					messageHandlers.get(message.getTopic()).process(message);
					if(logger.isDebugEnabled())logger.debug("MENDMIX-TRACE-LOGGGING-->> MQ_MESSAGE_CONSUME_SUCCESS ->topic:{},message:{}",message.getTopic(),sequence);
				} catch (Exception e) {
					logger.error(String.format("MENDMIX-TRACE-LOGGGING-->> MQ_MESSAGE_CONSUME_ERROR ->topic:%s,msgId:%s", message.getTopic(),sequence),e);
				}				
			}
		}
	}

	static class Translator implements EventTranslatorOneArg<MQMessageEvent, MQMessage> {
		@Override
		public void translateTo(MQMessageEvent event, long sequence, MQMessage data) {
			event.set(data);
		}
	}

}
