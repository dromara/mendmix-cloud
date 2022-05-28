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
package com.mendmix.common2.lock.redis;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import com.mendmix.common2.lock.LockException;

import redis.clients.jedis.Jedis;


/**
 * 基于redis的锁
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年3月22日
 */
public class RedisDistributeLockV2 implements Lock {

	
	private static RedisLockCoordinator coordinator = new RedisLockCoordinator();
	
	private static final String LOCK_KEY_PREFIX = "dlock:";
	private static final int _DEFAULT_MAX_WAIT = 30;
	private static final int _DEFAULT_MAX_WAIT_LIMIT = 20;
	private String lockName;
	private String eventId;
	private int maxWaitLimt;
	private int timeoutSeconds;
	private int maxLiveSeconds;
	private Jedis client;
	
	/**
	 * 默认最大存活时间30秒,最大排队等待数：20
	 * @param lockName
	 */
	public RedisDistributeLockV2(String lockName) {
		this(lockName, _DEFAULT_MAX_WAIT,_DEFAULT_MAX_WAIT_LIMIT);
	}
	
	/**
	 * 默认最大排队等待数：20
	 * @param lockName
	 * @param timeoutSeconds
	 */
	public RedisDistributeLockV2(String lockName,int timeoutSeconds) {
		this(lockName,timeoutSeconds,_DEFAULT_MAX_WAIT_LIMIT);
	}

	/**
	 * 
	 * @param lockName
	 * @param timeoutSeconds 锁超时时间（秒）
	 * @param maxWaitLimt 最大排队等待数限制
	 */
	public RedisDistributeLockV2(String lockName,int timeoutSeconds,int maxWaitLimt) {
		this.lockName = LOCK_KEY_PREFIX + lockName;
		this.timeoutSeconds = timeoutSeconds;
		this.maxLiveSeconds = timeoutSeconds + 1;
		this.eventId = coordinator.buildEvenId();
		this.maxWaitLimt = maxWaitLimt;
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
		
		boolean getLock = true;
		client = coordinator.getRedisClient();
		try {
			Long waitCount = client.lpush(lockName, eventId);
			client.expire(lockName, maxLiveSeconds);
			
			if(waitCount == 1){
				return getLock;
			}
			if(waitCount > maxWaitLimt){
				getLock = false;
				throw new LockException(String.format("Lock[%s] Too many wait", lockName));
			}
			if(timeout == 0){
				getLock = false;
				return getLock;
			}
			//await
			getLock = coordinator.await(lockName,eventId, unit.toMillis(timeout));
			
			if(!getLock){
				throw new LockException(String.format("Lock[%s] Timeout", lockName));
			}
			return getLock;
		} finally {
			if(!getLock)client.lrem(lockName, 1, eventId);
		}
		
	}

	@Override
	public void unlock() { 
		if(client == null)throw new LockException("cant't unlock,because Lock not found");
    	try {	
    		client.lrem(lockName, 1, eventId);
    		coordinator.notifyNext(client,lockName);
		} finally {
			if(client != null)coordinator.release(client);
		}
	}

	@Override
	public Condition newCondition() {
		return null;
	}
	
}
