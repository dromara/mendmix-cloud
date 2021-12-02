package com.jeesuite.amqp;

import com.jeesuite.amqp.MQContext.ActionType;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月10日
 */
public interface MQLogHandler {
    
	public void onSuccess(String groupName,ActionType actionType,MQMessage message);

	public void onError(String groupName,ActionType actionType,MQMessage message, Throwable e);

}
