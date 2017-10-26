package com.jeesuite.cache.local;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.RandomUtils;

/**
 * 缓存本地map实现
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年10月28日
 */
public class MapCacheProvider implements Closeable{

	private Map<String, Object> cache = new ConcurrentHashMap<>();
	private PriorityBlockingQueue<CacheKey> cacheKeys = new PriorityBlockingQueue<>();

	private ScheduledExecutorService cleanScheduledExecutor = Executors.newScheduledThreadPool(1);
	
	private int maxSize = 5000;
	
	private AtomicInteger currentCacheSize = new AtomicInteger(0);

	public MapCacheProvider() {
		this(1000);
	}
	
	public MapCacheProvider(final long period,int maxSize) {
		this(period);
		this.maxSize = maxSize;
	}

	/**
	 * @param period
	 *            检查过期间隔（毫秒）
	 */
	public MapCacheProvider(final long period) {
		
		cleanScheduledExecutor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				CacheKey cacheKey = cacheKeys.poll();
				if(cacheKey == null){
					return;
				}
				
				long currentTimeMils = System.currentTimeMillis();
				if(cacheKey.expireAt > currentTimeMils){
					//放回去
					cacheKeys.add(cacheKey);
					return;
				}
				// 过期的移除
				cache.remove(cacheKey.key);
				currentCacheSize.decrementAndGet();
				
			}
		}, period, period, TimeUnit.MILLISECONDS);
		
	}

	/**
	 * 
	 * @param key
	 * @param value
	 * @param timeout
	 *            单位：秒
	 * @return
	 */
	public boolean set(String key, Object value, int timeout) {
		
		if(currentCacheSize.incrementAndGet() > maxSize)throw new RuntimeException("CacheSize over the max size");

		cache.put(key, value);
		if (timeout > 0) {
			cacheKeys.add(new CacheKey(key, System.currentTimeMillis() + timeout * 1000));
		}
		return true;
	
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		return (T) cache.get(key);
	}

	public boolean remove(String key) {
		Object removeObj = cache.remove(key);
		if(removeObj != null){
			cacheKeys.remove(new CacheKey(key, 0));
			currentCacheSize.decrementAndGet();
		}
		return true;
	}

	public boolean exists(String key) {
		return cache.containsKey(key);
	}

	public void close() {
		cleanScheduledExecutor.shutdown();
	}

	private class CacheKey implements Comparable<CacheKey>{
		String key;
		long expireAt;

		public CacheKey(String key, long expireAt) {
			super();
			this.key = key;
			this.expireAt = expireAt;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CacheKey other = (CacheKey) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			return true;
		}

		private MapCacheProvider getOuterType() {
			return MapCacheProvider.this;
		}

		@Override
		public int compareTo(CacheKey o) {
			return Long.compare(this.expireAt, o.expireAt);
		}

		@Override
		public String toString() {
			return "[key=" + key + ", expireAt=" + expireAt + "]";
		}

	}

	public static void main(String[] args) {
		final MapCacheProvider provider = new MapCacheProvider(100);
		
		provider.set("aa", "aa", 50);
		provider.set("bb", "bb", 30);
		System.out.println("cache size:" + provider.cache.size() + "-" + provider.currentCacheSize.get());
		provider.remove("aa");
		System.out.println("cache size:" + provider.cache.size() + "-" + provider.currentCacheSize.get());
		
		ExecutorService executorService = Executors.newFixedThreadPool(5);
		
		for (int i = 0; i < 500; i++) {
			int expire = RandomUtils.nextInt(1, 30);
			final String key = "key" + i + "_" + expire;
			executorService.submit(new Runnable() {
				@Override
				public void run() {
					provider.set(key, key, expire);
					try {
						Thread.sleep(TimeUnit.MILLISECONDS.toMillis(10));
					} catch (Exception e) {
					}
				}
			});
			
		}
		
		while(true){
			if(provider.cache.isEmpty())break;
		}
		
		provider.close();
		executorService.shutdown();
	}

}
