/**
 * 
 */
package com.jeesuite.cache.redis;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

import com.jeesuite.cache.redis.cluster.JedisClusterProvider;
import com.jeesuite.cache.redis.standalone.JedisStandaloneProvider;

import redis.clients.jedis.JedisPoolConfig;

/**
 * redis服务提供者注册工厂
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年04月26日
 */
public class JedisProviderFactoryBean implements ApplicationContextAware,InitializingBean{

	protected static final Logger logger = LoggerFactory.getLogger(JedisProviderFactoryBean.class);
	/**
	 * 
	 */
	public static final String DEFAULT_GROUP_NAME = "default";
	
	private static final String REDIS_PROVIDER_SUFFIX = "RedisProvider";
	
	private Pattern pattern = Pattern.compile("^.+[:]\\d{1,5}\\s*$");

	private String mode;//
	
	private JedisPoolConfig jedisPoolConfig;
	
	//用来区分不同组的缓存
	private String group;
	private String servers;
	private Integer timeout;
	
	private ApplicationContext context;

	public void setGroup(String group) {
		this.group = group;
	}

	public String getGroup() {
		if(group == null)group = DEFAULT_GROUP_NAME;
		return group;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public void setJedisPoolConfig(JedisPoolConfig jedisPoolConfig) {
		this.jedisPoolConfig = jedisPoolConfig;
	}

	public void setServers(String servers) {
		this.servers = servers;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}
	
	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}
	
	
	

	@Override
	public void afterPropertiesSet() throws Exception {
		if(jedisPoolConfig == null)throw new Exception("jedisPoolConfig Not config ??");
		if(org.apache.commons.lang3.StringUtils.isAnyBlank(mode,servers)){
			throw new Exception("type or servers is empty??");
		}
		registerRedisProvier(); 
	}

	/**
	 * 
	 */
	private void registerRedisProvier() {
		String beanName = getGroup() + REDIS_PROVIDER_SUFFIX;
		if(context.containsBean(beanName)){
			throw new RuntimeException("已包含group为［"+this.group+"］的缓存实例");
		}
		
		String[] servers = StringUtils.tokenizeToStringArray(this.servers, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
		
		//检查ip和port格式
        for (String server : servers) {
			if(!pattern.matcher(server).matches()){
				throw new RuntimeException("参数servers："+this.servers+"错误");
			}
		}
        
		Class<?> beanClass = null;
		if(JedisStandaloneProvider.MODE.equalsIgnoreCase(mode)){	
			beanClass = JedisStandaloneProvider.class;
		}else if(JedisClusterProvider.MODE.equalsIgnoreCase(mode)){
			beanClass = JedisClusterProvider.class;
		}else{
			throw new RuntimeException("参数mode："+this.mode+"不支持");
		}
		
		
		DefaultListableBeanFactory acf = (DefaultListableBeanFactory) context.getAutowireCapableBeanFactory();  
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(beanClass);
		beanDefinitionBuilder.addConstructorArgValue(getGroup()).addConstructorArgValue(jedisPoolConfig).addConstructorArgValue(servers).addConstructorArgValue(timeout);
		acf.registerBeanDefinition(beanName, beanDefinitionBuilder.getRawBeanDefinition());
		//
		logger.info("register JedisProvider OK,Class:{},beanName:{}",beanClass.getSimpleName(),beanName);
	}
}
