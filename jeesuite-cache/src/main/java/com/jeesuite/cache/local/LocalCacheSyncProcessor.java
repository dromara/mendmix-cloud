package com.jeesuite.cache.local;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

import com.jeesuite.spring.InstanceFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

/**
 * 本地缓存同步处理器
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月21日
 */
public class LocalCacheSyncProcessor implements InitializingBean, DisposableBean{

	private static final Logger logger = LoggerFactory.getLogger(LocalCacheSyncProcessor.class);
	
	private static String channelName = "clearLevel1Cache";
	
	private String servers;
    private boolean enable = false;
	
	private long maxSize = 100000;
	private long expireAfterMins = 60 * 24;
	
	private List<String> keyPrefixs; //keys 
	private Jedis subJedisClient;
	private JedisPool pupJedisPool;
	
	private Timer timer;
	
	private LocalCacheSyncListener listener;
	
	private static LocalCacheSyncProcessor instance;
	
	public static LocalCacheSyncProcessor getInstance() {
		if(instance == null){
			synchronized (LocalCacheSyncProcessor.class) {				
				if(instance == null)instance = InstanceFactory.getInstance(LocalCacheSyncProcessor.class);
				if(instance == null)instance = new LocalCacheSyncProcessor();
			}
		}
		return instance;
	}
	

	public boolean publishSyncEvent(String key){
		if(enable ==false)return true;
		for (String prefix : keyPrefixs) {
			if(key.indexOf(prefix) == 0){
				logger.debug("redis publish:{} for key:{}",channelName,key);
				return publish(channelName, key);
			}
		}
		return true;
	}
	
	private boolean publish(String channel,String message){
		Jedis jedis = null;
		try {
			jedis = pupJedisPool.getResource();
			return jedis.publish(channel, message) > 0;
		} finally {
			if(jedis != null)jedis.close();
		}
		
	}

	@Override
	public void afterPropertiesSet() throws Exception {
	
		if(!enable)return;
		Validate.notNull(keyPrefixs);
		Validate.notBlank(servers);
		
		String[] serverInfos = StringUtils.tokenizeToStringArray(servers, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS)[0].split(":");
		
		final String host =serverInfos[0];
		final int port = Integer.parseInt(serverInfos[1]);
		
		listener = new LocalCacheSyncListener();
		
		timer = new Timer(true);
		timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				if(subJedisClient == null){
					try {
						subJedisClient = new Jedis(host, port);
						enable = true;
						if("PONG".equals(subJedisClient.ping())){							
							logger.info("subscribe localCache sync channel.....");
							subJedisClient.subscribe(listener, new String[]{channelName});
						}
					} catch (Exception e) {
						enable = false;
						try {listener.unsubscribe();} catch (Exception ex) {}
						try {
							subJedisClient.close();
						} catch (Exception e2) {}finally {
							subJedisClient = null;
						}
					}
				}
			}
		}, 100,1000 * 30);
		
		
		//
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxIdle(1);
		poolConfig.setMinEvictableIdleTimeMillis(60 * 1000);
		poolConfig.setMaxTotal(50);
		poolConfig.setMaxWaitMillis(30 * 1000);
		pupJedisPool = new JedisPool(poolConfig, host, port, 3000);
		
		LocalCacheProvider.init(keyPrefixs, maxSize,expireAfterMins);
	}

	@Override
	public void destroy() throws Exception {
		timer.cancel();
		try {listener.unsubscribe();} catch (Exception e) {}
		if(subJedisClient != null){
			subJedisClient.close();
		}
		if(pupJedisPool != null){
			pupJedisPool.close();
		}
		
	}

	public void setServers(String servers) {
		this.servers = servers;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public void setExpireAfterMins(long expireAfterMins) {
		this.expireAfterMins = expireAfterMins;
	}

	public void setMaxSize(long maxSize) {
		this.maxSize = maxSize;
	}

	public void setKeyPrefixs(String keyPrefixs) {
		if(org.apache.commons.lang3.StringUtils.isBlank(keyPrefixs)){
			return;
		}
		String[] tmpKeyPrefixs = StringUtils.tokenizeToStringArray(keyPrefixs, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
		this.keyPrefixs = new ArrayList<>(Arrays.asList(tmpKeyPrefixs));
	}


	private class LocalCacheSyncListener extends JedisPubSub {

		private static final String CLEAR_ALL = "clearall";

		@Override
		public void onMessage(String channel, String message) {
			super.onMessage(channel, message);
			if(channel.equals(channelName)){
				if(CLEAR_ALL.equals(message)){
					LocalCacheProvider.getInstance().clearAll();
					logger.info("receive command {} and clear local cache finish!",CLEAR_ALL);
				}else{					
					LocalCacheProvider.getInstance().remove(message);
				}
			}
		}
	}
	
}
