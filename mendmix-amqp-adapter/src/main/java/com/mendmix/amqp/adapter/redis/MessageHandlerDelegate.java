package com.mendmix.amqp.adapter.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.amqp.MQContext;
import com.mendmix.amqp.MQContext.ActionType;
import com.mendmix.amqp.MQMessage;
import com.mendmix.amqp.MessageHandler;
import com.mendmix.common.CurrentRuntimeContext;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年5月9日
 */
public class MessageHandlerDelegate {

	private final static Logger logger = LoggerFactory.getLogger("com.mendmix.amqp");
	
	private MQContext context;
	private MessageHandler messageHandler;
	
	public MessageHandlerDelegate(MQContext context,String topic, MessageHandler messageHandler) {
		this.context = context;
		this.messageHandler = messageHandler;
	}


	public void onMessage(String body, String topic) {
		MQMessage message = MQMessage.build(body);
		try {
			//多租户支持
			if(message.getTenantId() != null) {	
				CurrentRuntimeContext.setTenantId(message.getTenantId());
			}
			messageHandler.process(message);
			if(logger.isDebugEnabled())logger.debug("MQ_MESSAGE_CONSUME_SUCCESS ->message:{}",message.toString());
			MQContext.processMessageLog(context,message, ActionType.sub,null);
		} catch (Exception e) {
			MQContext.processMessageLog(context,message, ActionType.sub,e);
			logger.error(String.format("MQ_MESSAGE_CONSUME_ERROR ->message:%s",body),e);
		}
		
	}

	
}
