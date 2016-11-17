package com.jeesuite.common2.lock;

import java.util.concurrent.TimeUnit;

/**
 * 多次重试锁
 * @author tim
 *
 */
public class MultiRetryLock {

	/**
	 * 默认锁时间
	 */
	private long DEFAULT_LOCK_TIME = 10000;
	
	private DistributeLock distributeLock ;
	
	private boolean isLock=false;
	
	public MultiRetryLock(String lock){
		distributeLock = new DistributeLock(lock);
	}
	
	public void lock(){
		lock(DEFAULT_LOCK_TIME);
	}
	
	/**
	 * 获取锁
	 * @param lock
	 * @param timeout
	 * @return
	 */
	public void lock(long timeout) {
		isLock = distributeLock.lock(timeout+2*1000);
		long last = System.currentTimeMillis();
		while(!isLock){
			 
			try {
				TimeUnit.MILLISECONDS.sleep(100);
			} catch (InterruptedException e) {
				;
			}
			
			if(System.currentTimeMillis()-last>timeout){
				//锁超时
				throw new RuntimeException("multi retry lock timeout!");
			}
			//重新获取锁
			isLock=distributeLock.lock(timeout);
		}
		
	}
	
	
	public void unlock(){
		if(distributeLock!=null&& isLock){
			distributeLock.unlock();
			isLock=false;
		}
	}
	
}
