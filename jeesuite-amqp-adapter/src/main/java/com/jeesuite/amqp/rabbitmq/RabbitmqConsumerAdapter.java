package com.jeesuite.amqp.rabbitmq;

import com.rabbitmq.client.AMQP.Exchange.DeclareOk;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.jeesuite.amqp.MQConsumer;

/**
 * 
 * <br>
 * Class Name   : RabbitMQConsumer
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年9月3日
 */
public class RabbitmqConsumerAdapter implements MQConsumer {

	
	@Override
	public void start() throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("");
		factory.setPort(101);
		factory.setUsername("");
		factory.setPassword("");
		Connection connect = factory.newConnection();
		Channel channel = connect.createChannel();
		//声明exchange
		DeclareOk declareOk = channel.exchangeDeclare("my.fanout3", "fanout",true);
		channel.exchangeDeclare("test", "fanout");
	    String queueName = channel.queueDeclare().getQueue();
	    channel.queueBind(queueName, "test", "");

	    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

	    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
	        String message = new String(delivery.getBody(), "UTF-8");
	        System.out.println(" [x] Received '" + message + "'");
	    };
	    channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });
	}


	@Override
	public void shutdown() {
		
	}

}
