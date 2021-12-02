package com.jeesuite.amqp.qcloud.cmq;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.qcloud.cmq.Message;
import com.jeesuite.amqp.AbstractConsumer;
import com.jeesuite.amqp.MQMessage;
import com.jeesuite.amqp.MessageHandler;

/**
 * 
 * <br>
 * Class Name   : CMQConsumer
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年10月28日
 */
public class CMQConsumerAdapter extends AbstractConsumer {

	public CMQConsumerAdapter(Map<String, MessageHandler> messageHandlers) {
		super(messageHandlers);
	}

	@Override
	public void start() throws Exception {
		Set<String> topicNames = messageHandlers.keySet();
		for (String topic : topicNames) {
			CMQManager.createSubscriptionIfAbsent(topic);
		}
		//
		super.startWorker();
	}

	@Override
	public List<MQMessage> fetchMessages() {
		try {
			List<Message> messages;
			try {
				messages = CMQManager.getQueue().batchReceiveMessage(batchSize);
			} catch (com.qcloud.cmq.CMQServerException e) {
				//(10200)no message
				if(e.getMessage().equals("(10200)no message")) {
					return null;					
				}
				throw e;
			}
			if(messages == null || messages.isEmpty()) {
				return new ArrayList<>(0);
			}
			return messages.stream().map(o -> {
				MQMessage message = MQMessage.build(o.msgBody);
				message.setOriginMessage(o);
				return message;
			}).collect(Collectors.toList());
		} catch (Exception e) {
			logger.error("",e);
			return new ArrayList<>(0);
		}
	}

	@Override
	public String handleMessageConsumed(MQMessage message) {
		try {
			Message originMessage = message.getOriginMessage(Message.class);
			String receiptHandle = originMessage.receiptHandle;
			CMQManager.getQueue().deleteMessage(receiptHandle);
		} catch (Exception e) {
			logger.error("",e);
		}
		return null;
	}
	
	
	
}
