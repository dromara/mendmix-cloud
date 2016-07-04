package com.jeesuite.cache.local;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.jeesuite.common.util.DateUtils;

/**
 * 缓存本地map实现
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年10月28日
 */
public class MapCacheProvider {
	
	
	static Map<String, 	Object> cache = new ConcurrentHashMap<>();
	static Map<String, 	Date> cacheExpire = new HashMap<>();
	
	private Lock lock = new ReentrantLock();// 锁 
	private AtomicBoolean runing = new AtomicBoolean();
	
	public MapCacheProvider() {
		this(5);
	}
	
	/**
	 * @param period 检查过期间隔（秒）
	 */
	public MapCacheProvider(final long period) {
		runing.set(true);
		//缓存过期维护线程
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(runing.get()){					
					Date now = Calendar.getInstance().getTime();
					
					lock.lock();
					try {
						Iterator<Map.Entry<String, 	Date>> it = cacheExpire.entrySet().iterator();  
						while(it.hasNext()){  
							Map.Entry<String, Date> entry=it.next();  
							//过期的移除
							if(entry.getValue().compareTo(now)<=0){
								cache.remove(entry.getKey());
								it.remove(); 
							}  
						}  
						try {Thread.sleep(TimeUnit.SECONDS.toMillis(period));} catch (Exception e) {}
					} finally {
						lock.unlock();
					}
				}
			}
		}).start();
	}

	
	/**
	 * 
	 * @param key
	 * @param value
	 * @param timeout 单位：秒
	 * @return
	 */
	public boolean set(String key, Object value, int timeout) {
		Date expireAt = timeout > -1 ? DateUtils.add(new Date(), Calendar.SECOND, timeout) : null;
		return set(key, value, expireAt);
	}

	
	public boolean set(String key, Object value, Date expireAt) {
		cache.put(key, value);
		if(expireAt != null){
			cacheExpire.put(key, expireAt);
		}
		return true;
	}

	
	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		return (T)cache.get(key);
	}

	
	public boolean remove(String key) {
		cache.remove(key);
		cacheExpire.remove(key);
		return true;
	}

	public boolean exists(String key) {
		return cache.containsKey(key);
	}
	
	public void close(){
		runing.set(false);
	}

}
