package test.lock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;

import org.apache.commons.lang3.RandomUtils;

import com.jeesuite.common2.lock.redis.RedisDistributeLock;

public class RedisDistributeLockTest {

	private static CountDownLatch latch;
	
	public static void main(String[] args) throws Exception {
		

		int taskcount = 21;
		latch = new CountDownLatch(taskcount);
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
			Lock lock = new RedisDistributeLock("test",60);
			try {				
				lock.lock();
			} catch (Exception e) {
				latch.countDown();
				System.out.println("LockWorker[" + id + "] get lock error->"+e.getMessage());
				return;
			}
			System.out.println("LockWorker[" + id + "] get lock,doing-----" + ShareResource.add());
			try {Thread.sleep(RandomUtils.nextLong(100, 1000));} catch (Exception e) {}
			lock.unlock();
			latch.countDown();
			System.out.println("LockWorker[" + id + "] release lock,done");
		}
		
	}

}
