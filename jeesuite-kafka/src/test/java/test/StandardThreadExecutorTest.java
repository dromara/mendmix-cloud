/**
 * 
 */
package test;

import org.apache.commons.lang3.RandomUtils;

import com.jeesuite.kafka.thread.StandardThreadExecutor;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年10月21日
 */
public class StandardThreadExecutorTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		StandardThreadExecutor executor = new StandardThreadExecutor(1, 20, 10000);
		for (int i = 0; i < 5000; i++) {
			executor.submit(new Runnable() {
				@Override
				public void run() {
					System.out.println(Thread.currentThread().getName() + ":" + Thread.currentThread().getId() + "---->>>>>");
					try {Thread.sleep(RandomUtils.nextLong(100, 1000));} catch (Exception e) {}
				}
			});
		}
		
		while(true){
			try {Thread.sleep(100);} catch (Exception e) {}
			System.out.println("poolSize:" +executor.getPoolSize() + ",ActiveCount:" + executor.getActiveCount() + ",queueSize:" + executor.getQueue().size());
		}
	}

}
