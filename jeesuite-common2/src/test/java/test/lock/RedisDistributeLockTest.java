package test.lock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;

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
		
		int taskcount = 10;
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
			Lock lock = new RedisDistributeLock("test",120);
			try {				
				lock.lock();
			} catch (Exception e) {
				latch.countDown();
				System.out.println("LockWorker[" + id + "] get lock error->"+e.getMessage());
				return;
			}
			System.out.println("LockWorker[" + id + "] get lock,doing-----" + ShareResource.add());
			try {Thread.sleep(1000);} catch (Exception e) {}
			lock.unlock();
			latch.countDown();
			System.out.println("LockWorker[" + id + "] release lock,done");
		}
		
	}

	public static void initRedisProvider() {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxIdle(1);
		poolConfig.setMinEvictableIdleTimeMillis(60 * 1000);
		poolConfig.setMaxTotal(20);
		poolConfig.setMaxWaitMillis(30 * 1000);
		String[] servers = "127.0.0.1:6379".split(",");
		int timeout = 3000;
		String password = "123456";
		int database = 0;
		JedisProvider<Jedis,BinaryJedis> provider = new JedisStandaloneProvider("default", poolConfig, servers, timeout, password, database,null);
		JedisProviderFactory.setDefaultJedisProvider(provider);
	}
}
