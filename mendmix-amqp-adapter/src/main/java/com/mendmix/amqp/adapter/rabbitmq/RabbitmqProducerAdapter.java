package com.mendmix.amqp.adapter.rabbitmq;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.amqp.MQContext;
import com.mendmix.amqp.MQMessage;
import com.mendmix.amqp.MessageHeaderNames;
import com.mendmix.amqp.adapter.AbstractProducer;
import com.mendmix.common.guid.GUID;
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

	private final Logger logger = LoggerFactory.getLogger("com.mendmix.amqp");
	
	public RabbitmqProducerAdapter(MQContext context) {
		super(context);
	}

	@Override
	public String sendMessage(MQMessage message, boolean async) {
		String exchangeName = CachingChannelFactory.getExchangeName();
		Channel channel = CachingChannelFactory.getProduceChannel(context);
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
		message.setMsgId(GUID.uuid());
		AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
		builder.deliveryMode(MessageProperties.PERSISTENT_TEXT_PLAIN.getDeliveryMode());
		builder.priority(MessageProperties.PERSISTENT_TEXT_PLAIN.getPriority());
		builder.messageId(message.getMsgId());
		//
		Map<String, Object> headers = new HashMap<String, Object>();
		if (StringUtils.isNotBlank(message.getProduceBy())) {
			headers.put(MessageHeaderNames.produceBy.name(), message.getProduceBy());
		}
		if (StringUtils.isNotBlank(message.getRequestId())) {
			headers.put(MessageHeaderNames.requestId.name(), message.getRequestId());
		}
		if (StringUtils.isNotBlank(message.getTenantId())) {
			headers.put(MessageHeaderNames.tenantId.name(), message.getTenantId());
		}
		if (StringUtils.isNotBlank(message.getStatusCheckUrl())) {
			headers.put(MessageHeaderNames.statusCheckUrl.name(), message.getStatusCheckUrl());
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
