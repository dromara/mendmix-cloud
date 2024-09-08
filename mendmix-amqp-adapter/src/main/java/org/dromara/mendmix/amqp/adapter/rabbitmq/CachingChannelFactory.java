package org.dromara.mendmix.amqp.adapter.rabbitmq;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import org.dromara.mendmix.amqp.MQContext;
import org.dromara.mendmix.common.util.ResourceUtils;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * 
 * <br>
 * @author jiangwei
 * @version 1.0.0
 * @date 2023年5月5日
 */
public class CachingChannelFactory {

	private static String exchangeName;
	private static Channel produceChannel;
	private static List<Channel> channelHub = new ArrayList<>();

	public static String getExchangeName() {
		return exchangeName;
	}

	public static ConnectionFactory buildConnectionFactory(MQContext context) {
		Map<String, String> rabbitmqProperties = ResourceUtils.getMappingValues(context.getInstance() + ".amqp.rabbitmq");
		exchangeName = rabbitmqProperties.get("exchangeName");
		ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitmqProperties.get("host"));
        factory.setPort(Integer.parseInt(rabbitmqProperties.get("port")));
        factory.setUsername(rabbitmqProperties.get("username"));
        factory.setPassword(rabbitmqProperties.get("password"));
        factory.setVirtualHost(rabbitmqProperties.get("virtualHost"));
        if(rabbitmqProperties.containsKey("connectionTimeout")) {
        	factory.setConnectionTimeout(Integer.parseInt(rabbitmqProperties.get("connectionTimeout")));
        }
        
        Validate.notBlank(factory.getHost(), "[host] is missing");
        Validate.notBlank(factory.getVirtualHost(), "[virtualHost] is missing");
        if(StringUtils.isBlank(exchangeName)) {
        	exchangeName = "exchange" + StringUtils.replace(factory.getVirtualHost(), "/", "-");
        }
        Validate.notBlank(exchangeName, "[exchangeName] is missing");
        return factory;
        
	}
	
	public static Channel getProduceChannel(MQContext context) {
		if(produceChannel != null && produceChannel.isOpen())return produceChannel;
		try {
			synchronized (CachingChannelFactory.class) {
				if(produceChannel != null)return produceChannel;
				produceChannel = createChannel(context);
		        return produceChannel;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Channel createChannel(MQContext context) {
		try {
			synchronized (CachingChannelFactory.class) {
				ConnectionFactory connectionFactory = buildConnectionFactory(context);
				Connection connection = connectionFactory.newConnection();
				Channel channel = connection.createChannel();
				channel.exchangeDeclare(exchangeName, BuiltinExchangeType.TOPIC,false);
				channelHub.add(channel);
				return channel;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void closeAll() {
		for (Channel channel : channelHub) {
			try {channel.close();} catch (Exception e) {}
		}
	}
}
