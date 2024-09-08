package org.dromara.mendmix.amqp.adapter.rabbitmq;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.dromara.mendmix.amqp.MQContext;
import org.dromara.mendmix.amqp.MQMessage;
import org.dromara.mendmix.amqp.adapter.AbstractProducer;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;

/**
 * 
 * <br>
 * @author jiangwei
 * @version 1.0.0
 * @date 2023年5月4日
 */
public class RabbitmqProducerAdapter extends AbstractProducer {

	private final Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.amqp.adapter");
	
	public RabbitmqProducerAdapter(MQContext context) {
		super(context);
	}

	@Override
	public String sendMessage(MQMessage message, boolean async) {
		prepareHandle(message);
		Channel channel = CachingChannelFactory.getProduceChannel(context);
		String exchangeName = CachingChannelFactory.getExchangeName();
		AMQP.BasicProperties props = buildMessageBasicProperties(message);
		byte[] data = message.bodyAsBytes();
		try {
			channel.basicPublish(exchangeName, message.getTopic(), props, data);
			if(logger.isDebugEnabled())logger.debug("MQ_SEND_SUCCESS:{} -> msgId:{},status:{}",message.getTopic(),message.getMsgId());
			message.onProducerFinished(message.getMsgId(),0,0);
			handleSuccess(message);
		} catch (Exception e) {
			handleError(message, e);
			logger.warn("MQ_SEND_FAIL:"+message.getTopic(),e);
		}
		
		return null;
	}

	private AMQP.BasicProperties buildMessageBasicProperties(MQMessage message) {
		AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
		builder.deliveryMode(MessageProperties.PERSISTENT_TEXT_PLAIN.getDeliveryMode());
		builder.priority(MessageProperties.PERSISTENT_TEXT_PLAIN.getPriority());
		builder.messageId(message.getMsgId());
		//
		Map<String, Object> headers = new HashMap<String, Object>();
		if(message.getHeaders() != null) {
			String headerValue;
			for (String name : message.getHeaders().keySet()) {
				headerValue = message.getHeaders().get(name);
				if(headerValue == null)continue;
				headers.put(name, headerValue);
			}
		}
		builder.headers(headers);
		return builder.build();
	}

	@Override
	public void shutdown() {
		super.shutdown();
		CachingChannelFactory.closeAll();
	}

	
	
}
