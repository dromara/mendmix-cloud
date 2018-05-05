package test.lock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.jeesuite.common2.lock.zk.ZkDistributeLock;

public class ZkDistributeLockTest {

	private static CountDownLatch latch;

	public static void main(String[] args) throws Exception {

		int taskcount = 5;
		latch = new CountDownLatch(taskcount);
		ExecutorService threadPool = Executors.newFixedThreadPool(taskcount);

		for (int i = 0; i < taskcount; i++) {
			threadPool.submit(new LockWorker("worker-" + i));
		}

		latch.await();
		threadPool.shutdown();
	}

	static class LockWorker implements Runnable {

		private String id;

		public LockWorker(String id) {
			super();
			this.id = id;
		}

		@Override
		public void run() {
			ZkDistributeLock lock = new ZkDistributeLock("test");
			lock.lock();
			System.out.println("LockWorker[" + id + "] get lock,doing");
			try {
				Thread.sleep(2000);
			} catch (Exception e) {
			}
			lock.unlock();
			latch.countDown();
			System.out.println("LockWorker[" + id + "] release lock,done");
		}

	}

}
