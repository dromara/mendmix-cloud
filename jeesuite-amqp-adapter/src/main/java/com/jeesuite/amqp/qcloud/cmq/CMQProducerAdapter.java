package com.jeesuite.amqp.qcloud.cmq;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.qcloud.cmq.Topic;
import com.jeesuite.amqp.AbstractProducer;
import com.jeesuite.amqp.MQMessage;

/**
 * 
 * <br>
 * Class Name : CMQProducer
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年10月28日
 */
public class CMQProducerAdapter extends AbstractProducer {

	@Override
	public String sendMessage(MQMessage message, boolean async) {
		try {
			Topic topic = CMQManager.createTopicIfAbsent(message.getTopic());
			//发送返回的msgId与消费的msgId不一致 ，这里把msgId放消息体
			String msgId = StringUtils.replace(UUID.randomUUID().toString(), "-", StringUtils.EMPTY);
			message.setMsgId(msgId);
			topic.publishMessage(message.toMessageValue(false));
			//
			handleSuccess(message);
			return msgId;
		} catch (Exception e) {
			handleError(message, e);
			throw new RuntimeException("cmq_error", e);
		}
	}

}
