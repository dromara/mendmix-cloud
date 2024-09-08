package org.dromara.mendmix.amqp.adapter.rabbitmq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.dromara.mendmix.amqp.MQConsumer;
import org.dromara.mendmix.amqp.MQContext;
import org.dromara.mendmix.amqp.MQContext.ActionType;
import org.dromara.mendmix.amqp.MQMessage;
import org.dromara.mendmix.amqp.MessageHandler;
import org.dromara.mendmix.common.ThreadLocalContext;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

/**
 * 
 * <br>
 * @author jiangwei
 * @version 1.0.0
 * @date 2023年5月4日
 */
public class RabbitmqConsumerAdapter implements MQConsumer{

	private final static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.amqp.adapter");
	
	private Map<String, MessageHandler> messageHandlers = new HashMap<>(); 
	
	private MQContext context;
	private String queueName;
	
	public RabbitmqConsumerAdapter(MQContext context,Map<String, MessageHandler> messageHandlers) {
        this.context = context;
		this.messageHandlers = messageHandlers;
	}

	@Override
	public void start() throws Exception {
		queueName = context.getGroupName();
		Set<String> topics = messageHandlers.keySet();
		
		Channel channel = CachingChannelFactory.createChannel(context);
		
		for (String topic : topics) {
			channel.queueBind(queueName, CachingChannelFactory.getExchangeName(), topic);
		}
		//
		DeliverCallback deliverCallback;
		deliverCallback = new TopicDeliverCallback(channel);
		channel.basicConsume(queueName, false, deliverCallback , consumerTag -> {
        });
	}

	@Override
	public void shutdown() {
		CachingChannelFactory.closeAll();
	}
	
	private class TopicDeliverCallback implements DeliverCallback{

		private Channel channel;
		
		public TopicDeliverCallback(Channel channel) {
			this.channel = channel;
		}

		@Override
		public void handle(String consumerTag, Delivery message) throws IOException {
			String routingKey = message.getEnvelope().getRoutingKey();
			MessageHandler messageHandler = messageHandlers.get(routingKey);
			if(messageHandler == null)return;
			
			MQMessage mqMessage = new MQMessage(routingKey, message.getBody());
			BasicProperties properties = message.getProperties();
			mqMessage.setMsgId(properties.getMessageId());
			
			Map<String,Object> headers = properties.getHeaders();
			if(headers != null) {
				headers.forEach((k,v)->{
					if(v != null)mqMessage.addHeader(k, v.toString());
				});
			}
			
			long deliveryTag = message.getEnvelope().getDeliveryTag();
			try {
				if(context.matchedOnFilter(mqMessage)) {
					//用户上下文
					mqMessage.setUserContextOnConsume();
					messageHandler.process(mqMessage);
					if(logger.isDebugEnabled())logger.debug("MQ_MESSAGE_CONSUME_SUCCESS ->message:{}",message);
					//
					MQContext.processMessageLog(context,mqMessage, ActionType.sub,null);
				}
				//消费确认
				channel.basicAck(deliveryTag, false);
			} catch (Exception e) {
				//重新放入队列
				channel.basicReject(deliveryTag, true);
				logger.error(String.format("MQ_MESSAGE_CONSUME_ERROR ->message:%s",message.toString()),e);
				//
				MQContext.processMessageLog(context,mqMessage,ActionType.sub, e);
			}finally{
				ThreadLocalContext.unset();
			}
		}
		
	}
}
