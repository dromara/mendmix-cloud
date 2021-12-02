package com.jeesuite.amqp;

/**
 * 
 * <br>
 * Class Name   : MessageHanlder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2017年7月11日
 */
public interface MessageHandler {

	/**
	 * 预处理消息（同步）
	 * @param message
	 */
	default void prepare(MQMessage message) {}
	
	/**
	 * 处理消息（异步）
	 * @param message
	 * @throws Exception
	 */
	void process(MQMessage message) throws Exception;
}
