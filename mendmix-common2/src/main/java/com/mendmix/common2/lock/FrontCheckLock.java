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
package com.mendmix.common2.lock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;


/**
 * 本地前置检查锁
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年3月23日
 */
public class FrontCheckLock extends ReentrantLock{
	
	private static final long serialVersionUID = 1L;

	private static FrontCheckLock context = new FrontCheckLock();
	
	private Map<String,FrontCheckLock> localLocks = new ConcurrentHashMap<>();

	private String name;
	private AtomicInteger count  = new AtomicInteger(0);
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public AtomicInteger getCount() {
		return count;
	}
	public int updateCount(int delta) {
		return this.count.addAndGet(delta);
	}
	
    public static boolean lock(String lockName,long timeout, TimeUnit unit){
    	FrontCheckLock lc = context.localLocks.get(lockName);
    	if(lc == null){
    		synchronized (context.localLocks) {
    			lc = context.localLocks.get(lockName);
    			if(lc == null){
    				lc = new FrontCheckLock();
    				context.localLocks.put(lockName, lc);
    			}
			}
    	}
    	lc.updateCount(1);
    	
    	try {
			return lc.tryLock(timeout, unit);
		} catch (InterruptedException e) {
			return false;
		}
	}
    
    public static void unlock(String lockName){
    	FrontCheckLock lc = context.localLocks.get(lockName);
    	if(lc != null){
    		lc.unlock();
    		if(lc.updateCount(-1) == 0){
    			context.localLocks.remove(lockName);
    			lc = null;
    		}
    	}
    }

    
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FrontCheckLock other = (FrontCheckLock) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	
}
