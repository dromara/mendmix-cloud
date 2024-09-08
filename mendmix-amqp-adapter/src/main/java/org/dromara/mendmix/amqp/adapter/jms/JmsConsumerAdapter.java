/**
 * 
 */
package org.dromara.mendmix.amqp.adapter.jms;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.dromara.mendmix.amqp.MQConsumer;
import org.dromara.mendmix.amqp.MQContext;
import org.dromara.mendmix.amqp.MQContext.ActionType;
import org.dromara.mendmix.amqp.MQMessage;
import org.dromara.mendmix.amqp.MessageHandler;
import org.dromara.mendmix.common.ThreadLocalContext;

/**
 * <br>
 * @author vakinge(vakinge)
 * @date 2023年10月25日
 */
public class JmsConsumerAdapter  implements MQConsumer {
	
	private final static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.amqp.adapter");
	
	private MQContext context;
	private Map<String, MessageHandler> messageHandlers = new HashMap<>();
	private Session session ;
	private List<MessageConsumer> consumers = new ArrayList<>();
	
	public JmsConsumerAdapter(MQContext context, Map<String, MessageHandler> messageHandlers) {
		super();
		this.context = context;
		this.messageHandlers = messageHandlers;
	}

	@Override
	public void start() throws Exception {
		session = JmsResourceManager.createSession(context);
		Set<String> topicNames = messageHandlers.keySet();
		Topic jmsTopic;
		for (String topicName : topicNames) {
			jmsTopic = session.createTopic(topicName);
			MessageConsumer consumer = session.createConsumer(jmsTopic);
	        consumer.setMessageListener(message -> {
	            // 处理消息
	            MQMessage _message = null;
				try {
					_message = new MQMessage(topicName,((TextMessage)message).getText());
					_message.setMsgId(message.getJMSMessageID());
					Enumeration propertyNames = message.getPropertyNames();
					String headerName;
					while(propertyNames.hasMoreElements()) {
						headerName = Objects.toString(propertyNames.nextElement(), null);
						if(message.propertyExists(headerName)) {
							_message.addHeader(headerName, message.getStringProperty(headerName));
						}
						
					}
					//用户上下文
		            _message.setUserContextOnConsume();
					messageHandlers.get(topicName).process(_message);
					message.acknowledge();
					if(logger.isDebugEnabled())logger.debug("MQ_MESSAGE_CONSUME_SUCCESS ->message:{}",message);
					//
					MQContext.processMessageLog(context,_message, ActionType.sub,null);
				} catch (Exception e) {
					logger.error(String.format("MQ_MESSAGE_CONSUME_ERROR ->message:%s",message.toString()),e);
					//
					if(_message != null) {
						MQContext.processMessageLog(context,_message,ActionType.sub, e);
					}
				}finally{
					ThreadLocalContext.unset();
				}
	            
	        });
	        consumers.add(consumer);
		}
	}

	@Override
	public void shutdown() {
		for (MessageConsumer consumer : consumers) {
			try {consumer.close();} catch (Exception e) {}
		}
		try {
			session.close();
		} catch (JMSException e) {}
	} 
}
