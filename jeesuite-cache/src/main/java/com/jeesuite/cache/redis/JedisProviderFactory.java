/**
 * 
 */
package com.jeesuite.cache.redis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.jeesuite.cache.redis.cluster.JedisClusterProvider;
import com.jeesuite.cache.redis.standalone.JedisStandaloneProvider;
import com.jeesuite.spring.InstanceFactory;

import redis.clients.jedis.BinaryJedisClusterCommands;
import redis.clients.jedis.BinaryJedisCommands;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.MultiKeyBinaryCommands;
import redis.clients.jedis.MultiKeyBinaryJedisClusterCommands;
import redis.clients.jedis.MultiKeyCommands;
import redis.clients.jedis.MultiKeyJedisClusterCommands;

/**
 * redis实例工厂
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年04月26日
 */
public class JedisProviderFactory {

	protected static final Logger logger = LoggerFactory.getLogger(JedisProviderFactory.class);

	private static JedisProvider<?, ?> defaultJedisProvider;
	
	@SuppressWarnings("rawtypes")
	private static Map<String, JedisProvider> jedisProviders = new ConcurrentHashMap<>();
	
	public static void setDefaultJedisProvider(JedisProvider<?, ?> defaultJedisProvider) {
		JedisProviderFactory.defaultJedisProvider = defaultJedisProvider;
	}
	
	public static boolean containsGroup(String groupName){
		if(jedisProviders.isEmpty() && InstanceFactory.getInstanceProvider() != null){
			try {initFactoryFromSpring();} catch (Exception e) {}
		}
		return jedisProviders.containsKey(groupName);
	}
	
	public synchronized static void addProvider(JedisProvider<?, ?> provider){
		Assert.isTrue(!jedisProviders.containsKey(provider.groupName()), String.format("group[%s]已存在", provider.groupName()));
		jedisProviders.put(provider.groupName(), provider);
	}

	public static JedisProvider<?, ?> getJedisProvider(String groupName) {
		
		if(StringUtils.isNotBlank(groupName) && jedisProviders.containsKey(groupName)){
			return jedisProviders.get(groupName);
		}
		
		if(defaultJedisProvider == null){			
			initFactoryFromSpring();
		}
		
		if(StringUtils.isNotBlank(groupName)){
			logger.warn("未找到group[{}]对应的redis配置，使用默认缓存配置",groupName);
		}
		return defaultJedisProvider;
	}

	@SuppressWarnings("rawtypes")
	private synchronized static void initFactoryFromSpring() {
		if(defaultJedisProvider == null){
			//阻塞，直到spring初始化完成
			InstanceFactory.waitUtilInitialized();
			Map<String, JedisProvider> interfaces = InstanceFactory.getInstanceProvider().getInterfaces(JedisProvider.class);
			
			if(interfaces != null && interfaces.size() >0){				
				Iterator<JedisProvider> iterator = interfaces.values().iterator();
				while(iterator.hasNext()){
					JedisProvider jp = iterator.next();
					jedisProviders.put(jp.groupName(), jp);
				}
			}
			defaultJedisProvider = jedisProviders.get(JedisProviderFactoryBean.DEFAULT_GROUP_NAME);
			if(defaultJedisProvider == null && jedisProviders.size() == 1){
				defaultJedisProvider = new ArrayList<>(jedisProviders.values()).get(0);
			}
			
			Assert.notNull(defaultJedisProvider,"无默认缓存配置，请指定一组缓存配置group为default");
		}
	}

	public static JedisCommands getJedisCommands(String groupName) {
		return (JedisCommands) getJedisProvider(groupName).get();
	}

	public static BinaryJedisCommands getBinaryJedisCommands(String groupName) {
		return (BinaryJedisCommands) getJedisProvider(groupName).getBinary();
	}

	public static BinaryJedisClusterCommands getBinaryJedisClusterCommands(String groupName) {
		return (BinaryJedisClusterCommands) getJedisProvider(groupName).getBinary();
	}

	public static JedisCommands getJedisClusterCommands(String groupName) {
		return (JedisCommands) getJedisProvider(groupName).get();
	}
	
	public static MultiKeyCommands getMultiKeyCommands(String groupName) {
		return (MultiKeyCommands) getJedisProvider(groupName).get();
	}
	
	public static MultiKeyBinaryCommands getMultiKeyBinaryCommands(String groupName) {
		return (MultiKeyBinaryCommands) getJedisProvider(groupName).get();
	}
	
	public static MultiKeyJedisClusterCommands getMultiKeyJedisClusterCommands(String groupName) {
		return (MultiKeyJedisClusterCommands) getJedisProvider(groupName).get();
	}
	
	public static MultiKeyBinaryJedisClusterCommands getMultiKeyBinaryJedisClusterCommands(String groupName) {
		return (MultiKeyBinaryJedisClusterCommands) getJedisProvider(groupName).get();
	}
	
	public static String currentMode(String groupName){
		return getJedisProvider(groupName).mode();
	}
	
	public static boolean isStandalone(String groupName){
		return JedisStandaloneProvider.MODE.equals(currentMode(groupName));
	}
	
	public static boolean isCluster(String groupName){
		return JedisClusterProvider.MODE.equals(currentMode(groupName));
	}
}
