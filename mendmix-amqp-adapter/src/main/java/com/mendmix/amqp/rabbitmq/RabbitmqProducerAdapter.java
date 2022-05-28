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

import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.mendmix.amqp.MQMessage;
import com.mendmix.amqp.MQProducer;

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
