package com.jeesuite.amqp;

/**
 * 
 * <br>
 * Class Name : MQProducer
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2017年7月11日
 */
public interface MQProducer {

	public void start() throws Exception;

	/**
	 * 
	 * @param message
	 * @param async  是否异步
	 * @return
	 */
	public String sendMessage(MQMessage message,boolean async);
	
	public void shutdown();
}
