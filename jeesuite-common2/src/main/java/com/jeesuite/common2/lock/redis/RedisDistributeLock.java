package com.jeesuite.common2.lock.redis;

import static com.jeesuite.cache.redis.JedisProviderFactory.getJedisCommands;
import static com.jeesuite.cache.redis.JedisProviderFactory.getJedisProvider;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import com.jeesuite.common2.lock.LockException;

public class RedisDistributeLock implements Lock {

	private static final String CHAR = "1";
	private static final String LOCK_KEY_PREFIX = "dlock:";
	private static final int _DEFAULT_MAX_WAIT = 60;
	private String lockName;
	private int maxLiveSeconds;
	
	
	
	/**
	 * 默认最大存活时间60秒
	 * @param lockName
	 */
	public RedisDistributeLock(String lockName) {
		this(lockName, _DEFAULT_MAX_WAIT);
	}

	/**
	 * 
	 * @param lockName
	 * @param maxLiveSeconds 锁最大存活时间（秒）
	 */
	public RedisDistributeLock(String lockName,int maxLiveSeconds) {
		this.lockName = LOCK_KEY_PREFIX + lockName;
		this.maxLiveSeconds = maxLiveSeconds;
	}

	@Override
	public void lock() {

		boolean locked = false;
		try {
			locked = tryLock(maxLiveSeconds, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new LockException(e);
		}
		if(!locked){
			unlock();
			throw new LockException("Lock["+lockName+"] timeout");
		}
	
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
		
	}

	@Override
	public boolean tryLock() {
		try {		
			return checkLock();
		} finally {
			getJedisProvider(null).release();
		}
	
	}

	@Override
	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
		try {
			long start = System.currentTimeMillis();
			boolean res = checkLock();
			if(res)return res;
			
			while (!res) {
				try {
					TimeUnit.MILLISECONDS.sleep(100);
				} catch (InterruptedException e) {
					throw e;
				}
				if (res = checkLock())
					return res;
				if (System.currentTimeMillis() - start > unit.toMillis(time)) {
					return false;
				}
			}
		}  finally {
			getJedisProvider(null).release();
		}
		return false;
	}

	@Override
	public void unlock() {
    	try {			
    		getJedisCommands(null).del(lockName);
		} finally {
			getJedisProvider(null).release();
		}
	}

	@Override
	public Condition newCondition() {
		return null;
	}
	
	private boolean checkLock(){
		boolean res = getJedisCommands(null).setnx(lockName, CHAR) > 0;
		if(res)getJedisCommands(null).expire(lockName, maxLiveSeconds);
		return res;
	}

}
