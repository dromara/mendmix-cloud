package com.jeesuite.kafka.handler;

import com.jeesuite.kafka.message.DefaultMessage;

/**
 * 消息处理器接口
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月15日
 */
public interface MessageHandler {

	void process(DefaultMessage message);
}
