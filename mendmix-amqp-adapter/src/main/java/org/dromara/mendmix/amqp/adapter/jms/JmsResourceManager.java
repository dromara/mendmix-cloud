/**
 * 
 */
package org.dromara.mendmix.amqp.adapter.jms;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Objects;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.commons.lang3.reflect.FieldUtils;

import org.dromara.mendmix.amqp.MQContext;
import org.dromara.mendmix.common.GlobalContext;

/**
 * <br>
 * @author vakinge(vakinge)
 * @date 2023年10月25日
 */
public class JmsResourceManager {

	private static volatile Connection connection;
	private static volatile Session session;
	
	public static Connection getConnection(MQContext context) {
		if(connection != null)return connection;
		synchronized (JmsResourceManager.class) {
			if(connection != null)return connection;
			Properties properties = context.getProfileProperties();
			String targetConnectionFactoryClass = Objects.toString(properties.remove("targetConnectionFactoryClass"));
			try {
				Class<?> clazz = Class.forName(targetConnectionFactoryClass);
				ConnectionFactory connectionFactory = (ConnectionFactory) clazz.newInstance();
				Field[] fields = FieldUtils.getAllFields(clazz);
				for (Field field : fields) {
					field.setAccessible(true);
					String configKey = field.getName();
					Object configValue = null;
					if(properties.containsKey(configKey)){
						if(field.getType() == int.class || field.getType() == Integer.class){
							configValue = Integer.parseInt(properties.getProperty(configKey));
						}else if(field.getType() == long.class || field.getType() == Long.class){
							configValue = Long.parseLong(properties.getProperty(configKey));
						}else if(field.getType() == double.class || field.getType() == Double.class){
							configValue = Double.parseDouble(properties.getProperty(configKey));
						}else if(field.getType() == boolean.class || field.getType() == Boolean.class){
							configValue = Boolean.parseBoolean(properties.getProperty(configKey));
						}else if(field.getType() == URI.class){
							configValue = URI.create(properties.getProperty(configKey));
						}else{
							configValue = properties.getProperty(configKey);
						}
						field.set(connectionFactory, configValue);
					}
				}
				connection = connectionFactory.createConnection();
		        connection.setClientID(context.getGroupName() + GlobalContext.getWorkerId());
		        connection.start();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return connection;
	}

	public static Session getSession(MQContext context) {
		if(session != null)return session;
		synchronized (JmsResourceManager.class) {
			if(session != null)return session;
			try {
				session = getConnection(context).createSession(false, Session.CLIENT_ACKNOWLEDGE);
			} catch (JMSException e) {
				throw new RuntimeException(e);
			}
		}
		return session;
	}
	
	public static Session createSession(MQContext context) {
		try {
			return  getConnection(context).createSession(false, Session.AUTO_ACKNOWLEDGE);
		} catch (JMSException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void close() {
		if(session != null) {
			try {
				session.close();
				session = null;
			} catch (JMSException e) {}
		}
		if(connection != null) {
			try {				
				connection.close();
				connection = null;
			} catch (Exception e) {}
		}
	}
}
