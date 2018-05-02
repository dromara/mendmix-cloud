package com.jeesuite.common2.lock.redis;

import static com.jeesuite.cache.redis.JedisProviderFactory.getJedisCommands;

import java.util.ArrayList;
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

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年3月22日
 */
public class RedisLockCoordinator {

	private static final long CLEAN_TIME = TimeUnit.SECONDS.toMillis(90);
	private static final String SPLIT_STR = "$$";
	private static final String EVENT_NODE_ID = RandomStringUtils.random(8, true, true);
	private AtomicLong eventIdSeq = new AtomicLong(0);
	private List<String> activeNodeIds = new ArrayList<>();
	
	//private ScheduledExecutorService checkerSchedule = Executors.newScheduledThreadPool(1);
	
	private Jedis subClient;

	private static String channelName = "redisLockCoordinator";
	private Map<String,List<String>> getLockEventIds = new ConcurrentHashMap<>();
	
	public RedisLockCoordinator() {		
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				subClient = (Jedis) JedisProviderFactory.getJedisProvider(null).get();
				subClient.subscribe(new LockStateListener(), new String[]{channelName});
			}
		}, channelName);
		thread.setDaemon(true);
		thread.start();
		
		//TODO subClient 重连机制
	}
	
	public String buildEvenId(){
		return new StringBuilder().append(EVENT_NODE_ID).append(System.currentTimeMillis()).append(eventIdSeq.incrementAndGet()).toString();
	}
	
	public void notifyNext(Jedis pubClient,String lockName){
		long currentTimeMillis = System.currentTimeMillis();
		
		String nextEventId;
		while(true){
			nextEventId = pubClient.rpop(lockName);
			if(StringUtils.isBlank(nextEventId)){
				return;
			}
			if(currentTimeMillis - Long.parseLong(nextEventId.substring(8,21)) < CLEAN_TIME){
				break;
			}
			//TODO 判断通知的下一个节点是否存在
		}
		pubClient.publish(channelName, lockName + SPLIT_STR + nextEventId);
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
			//防止redis的锁数据丢失或者redis挂掉造成死锁,1秒钟检查一次
			int useTimeSeconds = (int) (useTime / 1000);
			if(useTimeSeconds > 0 && useTimeSeconds > deadLockCheckPoint){
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
	
	public void close(){
		try {subClient.close();} catch (Exception e) {}
		//checkerSchedule.shutdown();
	}
	
	private class LockStateListener extends JedisPubSub {
		
		public LockStateListener() {}


		@Override
		public void onMessage(String channel, String message) {
			super.onMessage(channel, message);
			if(StringUtils.isBlank(message)){
				return;
			}
			String[] parts = StringUtils.splitByWholeSeparator(message, SPLIT_STR);
			String lockName = parts[0];
			String nextEventId = parts[1];
			if(!nextEventId.startsWith(EVENT_NODE_ID)){
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
