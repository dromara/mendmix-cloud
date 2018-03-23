package com.jeesuite.common2.lock.redis;

import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import com.jeesuite.cache.redis.JedisProviderFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import static com.jeesuite.cache.redis.JedisProviderFactory.getJedisCommands;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年3月22日
 */
public class RedisLockCoordinator {

	private static final long CLEAN_TIME = TimeUnit.MINUTES.toMillis(5);
	private static final String SPLIT_STR = "$$";
	private static final String EVENT_ID_PREFIX = RandomStringUtils.random(8, true, true);
	private AtomicLong eventIdSeq = new AtomicLong(0);

	private static String channelName = "redisLockCoordinator";
	private Map<String,List<String>> getLockEventIds = new ConcurrentHashMap<>();
	
	public RedisLockCoordinator() {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				Jedis subJedis = (Jedis) JedisProviderFactory.getJedisProvider(null).get();
				subJedis.subscribe(new LockStateListener(), new String[]{channelName});
			}
		}, channelName);
		
		thread.setDaemon(true);
		thread.start();
	}
	
	public String buildEvenId(){
		return new StringBuilder().append(EVENT_ID_PREFIX).append(System.currentTimeMillis()).append(eventIdSeq.incrementAndGet()).toString();
	}
	
	public void notifyNext(String lockName){
		try {
			Jedis jedis = (Jedis) JedisProviderFactory.getJedisProvider(null).get();
			
			long currentTimeMillis = System.currentTimeMillis();
			
			String nextEventId;
			while(true){
				nextEventId = jedis.rpop(lockName);
				if(StringUtils.isBlank(nextEventId)){
					return;
				}
				if(currentTimeMillis - Long.parseLong(nextEventId.substring(8,21)) < CLEAN_TIME){
					break;
				}
			}
			jedis.publish(channelName, lockName + SPLIT_STR + nextEventId);
		} finally {
			JedisProviderFactory.getJedisProvider(null).release();
		}
	}
	
	
	
	public boolean await(String lockName,String eventId,long timeoutMills){
		
		List<String> eventIds = getGetLockEventIds(lockName);

		long start = System.currentTimeMillis();
		
		int deadLockCheckPoint = 0;
		while (true) {
			try {TimeUnit.MILLISECONDS.sleep(1);} catch (InterruptedException e) {}
			if (eventIds.contains(eventId)){
				eventIds.remove(eventId);
				return true;
			}
			
			long useTime = System.currentTimeMillis() - start;
			if (useTime > timeoutMills) {
				return false;
			}
			//防止redis的锁数据丢失或者redis挂掉造成死锁
			int useTimeSeconds = (int) (useTime / 1000);
			if(useTimeSeconds > deadLockCheckPoint){
				deadLockCheckPoint = useTimeSeconds;
				try {
					if(getJedisCommands(null).llen(lockName) == 0){
						return false;
					}
				} catch (Exception e) {
					return false;
				}
			}
		}
	}
	
	private List<String> getGetLockEventIds(String lockName){
		List<String> eventIds = getLockEventIds.get(lockName);
		if(eventIds == null){
			synchronized (getLockEventIds) {
				eventIds = getLockEventIds.get(lockName);
				if(eventIds == null){
					eventIds = new Vector<>();
					getLockEventIds.put(lockName, eventIds);
				}
			}
		}
		
		return eventIds;
	}
	
	private class LockStateListener extends JedisPubSub {
		
		public LockStateListener() {}


		@Override
		public void onMessage(String channel, String message) {
			super.onMessage(channel, message);
			if(StringUtils.isBlank(message)){
				return;
			}
			String[] parts = StringUtils.split(message, SPLIT_STR);
			String lockName = parts[0];
			String nextEventId = parts[1];
			if(!nextEventId.startsWith(EVENT_ID_PREFIX)){
				return;
			}
			getGetLockEventIds(lockName).add(nextEventId);
		}
	}
	
	public static void main(String[] args) {
		System.out.println(1500 % 1000);
		System.out.println(1500 / 1000);
	}
	
}
