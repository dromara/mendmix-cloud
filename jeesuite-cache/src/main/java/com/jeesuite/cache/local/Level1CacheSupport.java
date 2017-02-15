package com.jeesuite.cache.local;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
public class Level1CacheSupport implements InitializingBean, DisposableBean{

	private static final Logger logger = LoggerFactory.getLogger(Level1CacheSupport.class);
	
	private String channelName = "clearLevel1_";
	
	private String bcastServer;
    private boolean distributedMode = true; //是否启用分布式模式
	
	private String password;
	
	private List<String> cacheNames; //
	private Jedis subJedisClient;
	private JedisPool pupJedisPool;
	
	private ScheduledExecutorService redisCheckTimer;
	
	private Level1CacheProvider cacheProvider;
	
	private LocalCacheSyncListener listener;
	
	private static Level1CacheSupport instance;
	
	public static Level1CacheSupport getInstance() {
		if(instance == null){
			synchronized (Level1CacheSupport.class) {				
				if(instance == null)instance = InstanceFactory.getInstance(Level1CacheSupport.class);
				if(instance == null)instance = new Level1CacheSupport();
			}
		}
		return instance;
	}
	

	public boolean publishSyncEvent(String key){
		if(cacheNames == null)return true;
		String cacheName = key.split("\\.")[0];
		if(!cacheNames.contains(cacheName))return true;
		//删除本地
		cacheProvider.remove(cacheName, key);
		logger.debug("remove local LEVEL1 cache: cacheName:[{}], key:[{}]",cacheName,key);
		if(!distributedMode)return true;
		boolean publish = publish(channelName, new ClearCommand(cacheName, key).serialize());
		if(publish){
			logger.debug("broadcast <clear-cache> command for key:[{}] by channelName:[{}]",key,channelName);			
		}
		return publish;
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
	
	public boolean set(String key, Object value) {
		if(cacheNames == null)return true;
		String cacheName = key.split("\\.")[0];
		if(!cacheNames.contains(cacheName))return true;
		boolean result = cacheProvider.set(cacheName, key, value);
		if(logger.isDebugEnabled())logger.debug("set LEVEL1 cache:{}",key);
		return result;
	}

	public <T> T get(String key) {
		if(cacheNames == null)return null;
		String cacheName = key.split("\\.")[0];
		if(!cacheNames.contains(cacheName))return null;
		T object = cacheProvider.get(cacheName, key);
		if(object != null)logger.debug("get cache:{} from LEVEL1",key);
		return object;
	}

	public void remove(String key) {
		if(cacheNames == null)return;
		String cacheName = key.split("\\.")[0];
		if(!cacheNames.contains(cacheName))return;
		cacheProvider.remove(cacheName, key);
		logger.debug("remove LEVEL1 cache,cacheName:{},key:{}",cacheName,key);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
	
		if(cacheNames == null)return;
		if(cacheProvider == null){
			cacheProvider = new GuavaLevel1CacheProvider();
		}
		//
		cacheProvider.start();
		//
		if(!distributedMode)return;
		Validate.notBlank(bcastServer);
		String[] serverInfos = StringUtils.tokenizeToStringArray(bcastServer, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS)[0].split(":");
		
		final String host =serverInfos[0];
		final int port = Integer.parseInt(serverInfos[1]);
		
		listener = new LocalCacheSyncListener();
		
		redisCheckTimer = Executors.newScheduledThreadPool(1);
		redisCheckTimer.scheduleWithFixedDelay(new Runnable() {
			
			@Override
			public void run() {
				if(subJedisClient == null){
					try {
						subJedisClient = new Jedis(host, port);
						if(password != null)subJedisClient.auth(password);
						if("PONG".equals(subJedisClient.ping())){							
							logger.info("subscribe localCache sync channel.....");
							subJedisClient.subscribe(listener, new String[]{channelName});
						}
					} catch (Exception e) {
						try {listener.unsubscribe();} catch (Exception ex) {}
						try {
							subJedisClient.close();
						} catch (Exception e2) {}finally {
							subJedisClient = null;
						}
					}
				}
			}
		}, 0, 30, TimeUnit.SECONDS);
		
		
		//
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxIdle(1);
		poolConfig.setMinEvictableIdleTimeMillis(60 * 1000);
		poolConfig.setMaxTotal(10);
		poolConfig.setMaxWaitMillis(30 * 1000);
		pupJedisPool = new JedisPool(poolConfig, host, port, 3000, password);
	}

	@Override
	public void destroy() throws Exception {
		if(cacheProvider != null)cacheProvider.close();
		if(redisCheckTimer != null)redisCheckTimer.shutdown();
		try {listener.unsubscribe();} catch (Exception e) {}
		if(subJedisClient != null){
			subJedisClient.close();
		}
		if(pupJedisPool != null){
			pupJedisPool.close();
		}
		
	}

	public void setBcastServer(String servers) {
		this.bcastServer = servers;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setCacheProvider(Level1CacheProvider cacheProvider) {
		this.cacheProvider = cacheProvider;
	}

	public void setDistributedMode(boolean distributedMode) {
		this.distributedMode = distributedMode;
	}

	public void setBcastScope(String bcastScope) {
		this.channelName =  "clearLevel1_" + bcastScope;
	}


	public void setCacheNames(String cacheNames) {
		if(org.apache.commons.lang3.StringUtils.isBlank(cacheNames)){
			return;
		}
		String[] tmpcacheNames= StringUtils.tokenizeToStringArray(cacheNames, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
		this.cacheNames = new ArrayList<>(Arrays.asList(tmpcacheNames));
	}


	private class LocalCacheSyncListener extends JedisPubSub {

		private static final String CLEAR_ALL = "clearall";

		@Override
		public void onMessage(String channel, String message) {
			super.onMessage(channel, message);
			if(channel.equals(channelName)){
				if(CLEAR_ALL.equals(message)){
					cacheProvider.clearAll();
					logger.info("receive command {} and clear local cache finish!",CLEAR_ALL);
				}else{	
					try {						
						ClearCommand command = ClearCommand.deserialize(message);
						if(command.isLocalCommand()){
							return;
						}
						cacheProvider.remove(command.getCacheName(), command.getKey());
					} catch (Exception e) {}
				}
			}
		}
	}
	
}
