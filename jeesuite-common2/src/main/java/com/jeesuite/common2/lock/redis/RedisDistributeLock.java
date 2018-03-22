package com.jeesuite.common2.lock.redis;

import static com.jeesuite.cache.redis.JedisProviderFactory.getJedisCommands;
import static com.jeesuite.cache.redis.JedisProviderFactory.getJedisProvider;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.jeesuite.common2.lock.LockException;

import redis.clients.jedis.JedisCommands;


/**
 * 基于redis的锁
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年3月22日
 */
public class RedisDistributeLock implements Lock {

	
	private static RedisLockCoordinator coordinator = new RedisLockCoordinator();
	
	private static final String LOCK_KEY_PREFIX = "dlock:";
	private static final int _DEFAULT_MAX_WAIT = 30;
	private String lockName;
	private String eventId;
	private int maxWaitLimt = 10;
	private int timeoutSeconds;
	private int maxLiveSeconds;
	
	private ReentrantLock localLock =  new ReentrantLock();
	
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
	 * @param timeoutSeconds 锁超时时间（秒）
	 */
	public RedisDistributeLock(String lockName,int timeoutSeconds) {
		this.lockName = LOCK_KEY_PREFIX + lockName;
		this.timeoutSeconds = timeoutSeconds;
		this.maxLiveSeconds = timeoutSeconds + 1;
		this.eventId = coordinator.buildEvenId();
	}

	@Override
	public void lock() {
		try {
			tryLock(timeoutSeconds, TimeUnit.SECONDS);			
		} catch (InterruptedException e) {}
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
		
	}

	@Override
	public boolean tryLock() {
		try {
			return tryLock(0, TimeUnit.SECONDS);			
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
		
		boolean getLock = false;
		try {
			try {getLock = localLock.tryLock(timeout, unit);} catch (Exception e) {}
			
			if(!getLock){
				if(timeout == 0)return getLock;
				throw new LockException(String.format("Lock[%s] Timeout", lockName));
			}
			
			Long waitCount = getJedisCommands(null).lpush(lockName, eventId);
			getJedisCommands(null).expire(lockName, maxLiveSeconds);
			
			if(waitCount == 1){
				getLock = true;
				return getLock;
			}
			if(waitCount >= maxWaitLimt){
				getLock = false;
				throw new LockException(String.format("Lock[%s] Too many wait", lockName));
			}
			if(timeout == 0){
				getLock = false;
				return getLock;
			}
			//await future
			getLock = coordinator.await(lockName,eventId, unit.toMillis(timeout));
			
			if(!getLock){
				throw new LockException(String.format("Lock[%s] Timeout", lockName));
			}
			return getLock;
		} finally {
			if(!getLock)getJedisCommands(null).lrem(lockName, 1, eventId);
			getJedisProvider(null).release();
		}
		
	}

	@Override
	public void unlock() {  
    	try {	
    		localLock.unlock();
    		JedisCommands command = getJedisCommands(null);
			command.lrem(lockName, 0, eventId);
    		coordinator.notifyNext(lockName);
		} finally {
			getJedisProvider(null).release();
		}
	}

	@Override
	public Condition newCondition() {
		return null;
	}
	
}
