package com.jeesuite.amqp;

import com.jeesuite.amqp.MQContext.ActionType;

public abstract class AbstractProducer implements MQProducer {
	
	@Override
	public void start() throws Exception {}


	@Override
	public void shutdown() {}
	
	
	public void handleSuccess(MQMessage message) {
		MQContext.processMessageLog(message,ActionType.pub, null);
	}

	public void handleError(MQMessage message, Throwable e) {
		MQContext.processMessageLog(message,ActionType.pub, e);
	}

}
