package com.jeesuite.amqp;

/**
 * 
 * <br>
 * Class Name   : MQInstanceDelegate
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2017年7月11日
 */
public class MQInstanceDelegate {
	
	private static MQProducer producer;
	
	private MQInstanceDelegate() {}

	public static void setProducer(MQProducer producer) {
		MQInstanceDelegate.producer = producer;
	}

	public static void send(MQMessage message){
		if(producer == null){
			System.err.println("MQProducer did not Initialization,Please check config[mq.provider] AND [mq.producer.enabled]");
			return;
		}
		message.setTopic(MQContext.rebuildWithNamespace(message.getTopic()));
		producer.sendMessage(message, false);
	}
	
    public static void asyncSend(MQMessage message){
    	if(producer == null){
    		System.err.println("MQProducer did not Initialization,Please check config[mq.provider] AND [mq.producer.enabled]");
    		return;
		}
    	producer.sendMessage(message, true);
	}
}
