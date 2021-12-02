package com.jeesuite.amqp;

/**
 * 
 * <br>
 * Class Name   : MQConsumer
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年7月11日
 */
public interface MQConsumer {

	public void start() throws Exception;
	
	public void shutdown();
}
