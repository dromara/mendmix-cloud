/*
 * Copyright 2016-2022 dromara.org.
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
package org.dromara.mendmix.common.lock;

import org.dromara.mendmix.common.lock.redis.RedisDistributeLock;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年7月22日
 */
public class DistributeLockTemplate {

	private static final int _DEFAULT_LOCK_HOLD_MILLS = 30;
	
	
	public static <T> T execute(String lockId,LockCaller<T> caller){
		return execute(lockId, caller, _DEFAULT_LOCK_HOLD_MILLS);
	}
	
	/**
	 * @param lockId 要确保不和其他业务冲突（不能用随机生成）
	 * @param caller 业务处理器
	 * @param timeout 超时时间（秒）
	 * @return
	 */
	public static <T> T execute(String lockId,LockCaller<T> caller,int timeout){
		RedisDistributeLock dLock = new RedisDistributeLock(lockId,(int)timeout/1000);
		
		boolean getLock = false;
		try {
			if(dLock.tryLock()){
				getLock = true;
				return caller.onHolder();
			}else{
				return caller.onWait();
			}
		} finally {
			if(getLock)dLock.unlock();
		}
		
	}
}
