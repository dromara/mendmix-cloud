/**
 * 
 */
package org.dromara.mendmix.amqp.adapter.jms;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.dromara.mendmix.amqp.MQContext;
import org.dromara.mendmix.amqp.MQMessage;
import org.dromara.mendmix.amqp.adapter.AbstractProducer;

/**
 * <br>
 * @author vakinge(vakinge)
 * @date 2023年10月25日
 */
public class JmsProducerAdapter extends AbstractProducer {

	private final Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.amqp.adapter");
	
	private Session session ;
	private Map<String, MessageProducer> topicProducerMappings = new ConcurrentHashMap<>();
	/**
	 * @param context
	 */
	public JmsProducerAdapter(MQContext context) {
		super(context);
		session = JmsResourceManager.createSession(context);
	}

	@Override
	public String sendMessage(MQMessage message, boolean async) {
		prepareHandle(message);
		try {
			TextMessage textMessage = session.createTextMessage(message.toMessageValue(true));
			if(message.getHeaders() != null) {
				String headerValue;
				for (String name : message.getHeaders().keySet()) {
					headerValue = message.getHeaders().get(name);
					if(headerValue == null)continue;
					textMessage.setStringProperty(name, headerValue);
				}
			}
			getMessageProducer(message.getTopic()).send(textMessage);
            //成功发送
            handleSuccess(message);
            logger.debug("发送成功, topic:{}", message.getTopic());
			return textMessage.getJMSMessageID();
		} catch (Exception e) {
            //发送失败
            handleError(message, e);
            logger.warn("发送失败, topic:{}, exception:{}", message.getTopic(), e);
        }
		return null;
		
	}
	
	private MessageProducer getMessageProducer(String topic) {
		if(topicProducerMappings.containsKey(topic)) {
			return topicProducerMappings.get(topic);
		}
		synchronized (topicProducerMappings) {
			try {
				Topic jmsTopic = session.createTopic(topic);
				MessageProducer producer = session.createProducer(jmsTopic);
				topicProducerMappings.put(topic, producer);
				return producer;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void shutdown() {
		super.shutdown();
		topicProducerMappings.values().forEach(o -> {
			try {o.close();} catch (Exception e) {}
		});
		try {
			session.close();
		} catch (JMSException e) {}
	}
	
	

}
