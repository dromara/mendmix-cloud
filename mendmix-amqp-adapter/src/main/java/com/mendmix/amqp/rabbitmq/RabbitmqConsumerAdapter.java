/*
 * Copyright 2016-2022 www.mendmix.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mendmix.amqp.rabbitmq;

import java.util.Map;

import com.mendmix.amqp.MQConsumer;
import com.mendmix.amqp.MessageHandler;
import com.rabbitmq.client.AMQP.Exchange.DeclareOk;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

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

	public RabbitmqConsumerAdapter(Map<String, MessageHandler> messageHandlers) {
		
	}


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
