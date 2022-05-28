/*
 * Copyright 2016-2022 www.mendmix.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package test.lock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mendmix.common2.lock.zk.ZkDistributeLock;

public class ZkDistributeLockTest {

	private static CountDownLatch latch;

	public static void main(String[] args) throws Exception {

		int taskcount = 5;
		latch = new CountDownLatch(taskcount);
		ExecutorService threadPool = Executors.newFixedThreadPool(taskcount);

		for (int i = 0; i < taskcount; i++) {
			threadPool.execute(new LockWorker("worker-" + i));
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
