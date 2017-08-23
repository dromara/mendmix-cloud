package test.lock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.RandomUtils;

import com.jeesuite.cache.redis.JedisProvider;
import com.jeesuite.cache.redis.JedisProviderFactory;
import com.jeesuite.cache.redis.standalone.JedisStandaloneProvider;
import com.jeesuite.common2.lock.redis.RedisDistributeLock;

import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;

public class RedisDistributeLockTest {

	private static CountDownLatch latch;
	
	public static void main(String[] args) throws Exception {
		
		int taskcount = 5;
		latch = new CountDownLatch(taskcount);
		initRedisProvider();
		ExecutorService threadPool = Executors.newFixedThreadPool(taskcount);
		
		for (int i = 0; i < taskcount; i++) {
			threadPool.submit(new LockWorker("worker-"+i));
		}
		
		latch.await();
		threadPool.shutdown();
	}
	
	static class LockWorker implements Runnable{

		private String id;
		
		public LockWorker(String id) {
			super();
			this.id = id;
		}

		@Override
		public void run() {
			RedisDistributeLock lock = new RedisDistributeLock("test");
			lock.lock();
			System.out.println("LockWorker[" + id + "] get lock,doing");
			try {Thread.sleep(RandomUtils.nextLong(1000, 10000));} catch (Exception e) {}
			lock.unlock();
			latch.countDown();
			System.out.println("LockWorker[" + id + "] release lock,done");
		}
		
	}

	public static void initRedisProvider() {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxIdle(1);
		poolConfig.setMinEvictableIdleTimeMillis(60 * 1000);
		poolConfig.setMaxTotal(5);
		poolConfig.setMaxWaitMillis(30 * 1000);
		String[] servers = "127.0.0.1:6379".split(",");
		int timeout = 3000;
		String password = null;
		int database = 0;
		JedisProvider<Jedis,BinaryJedis> provider = new JedisStandaloneProvider("default", poolConfig, servers, timeout, password, database,null);
		JedisProviderFactory.setDefaultJedisProvider(provider);
	}
}
