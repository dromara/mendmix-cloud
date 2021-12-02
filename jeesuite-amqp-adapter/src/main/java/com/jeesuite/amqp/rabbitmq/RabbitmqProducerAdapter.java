package com.jeesuite.amqp.rabbitmq;

import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.jeesuite.amqp.MQMessage;
import com.jeesuite.amqp.MQProducer;

/**
 * 
 * <br>
 * Class Name   : RabbitMQProducer
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年9月3日
 */
public class RabbitmqProducerAdapter implements MQProducer{


	@Override
	public void start() throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("");
		factory.setPort(101);
		factory.setUsername("");
		factory.setPassword("");
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();
		//声明 exchange 的type 为 fanout 广播模式
		channel.exchangeDeclare("my.fanout3","fanout",true);
		DeclareOk declareOk = channel.queueDeclare("test", false, false, false, null);
		channel.basicPublish("my.fanout3", "", null, "hellow my fanout".getBytes());
		channel.close();
		connection.close();
	}


	@Override
	public String sendMessage(MQMessage message, boolean async) {

		return null;
	}


	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

}
