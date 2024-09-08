package org.dromara.mendmix.amqp.adapter.eventbus;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年8月10日
 */
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.dromara.mendmix.amqp.MQContext;
import org.dromara.mendmix.amqp.MQMessage;
import org.dromara.mendmix.amqp.MessageHandler;
import org.dromara.mendmix.amqp.adapter.AbstractProducer;


/**
 * 
 * <br>
 * Class Name : EventbusProducerAdapter
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年3月24日
 */
public class EventbusProducerAdapter extends AbstractProducer {

	private final static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix");

	private final static Translator TRANSLATOR = new Translator();
	private static EventHandler<MQMessageEvent> eventHandler = new MQMessageEventHandler();
	private static Map<String, MessageHandler> messageHandlers = new HashMap<>(); 
	
	private Disruptor<MQMessageEvent> disruptor;
	
	public EventbusProducerAdapter(MQContext context) {
		super(context);
	}

	@Override
	public void start() throws Exception {
		EventFactory<MQMessageEvent> eventFactory = new MQMessageEventFactory();
		int ringBufferSize = 1024 * 1024;
		BasicThreadFactory factory = new BasicThreadFactory.Builder()
				      .namingPattern("mq-eventbus-%d")
				      .daemon(true)
				      .priority(Thread.NORM_PRIORITY)
				      .build();
		disruptor = new Disruptor<MQMessageEvent>(eventFactory,
		                ringBufferSize, factory, ProducerType.SINGLE,
		                new BlockingWaitStrategy());
		
		disruptor.handleEventsWith(eventHandler);
		
		disruptor.start();
	}

	@Override
	public String sendMessage(MQMessage message, boolean async) {
		prepareHandle(message);
		RingBuffer<MQMessageEvent> ringBuffer = disruptor.getRingBuffer();
	    ringBuffer.publishEvent(TRANSLATOR, message);
	    handleSuccess(message);
		return message.getMsgId();
	}

	@Override
	public void shutdown() {
		super.shutdown();
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
			message.setUserContextOnConsume();
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
