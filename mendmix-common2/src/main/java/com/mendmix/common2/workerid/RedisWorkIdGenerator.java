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
package com.mendmix.common2.workerid;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.mendmix.cache.RedisTemplateGroups;
import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.WorkIdGenerator;
import com.mendmix.common2.lock.redis.RedisDistributeLock;
import com.mendmix.common2.task.SubTimerTask;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Dec 18, 2022
 */
public class RedisWorkIdGenerator implements WorkIdGenerator,SubTimerTask {

	private static final Logger logger = LoggerFactory.getLogger("com.mendmix");
	
	private static final int INTERVAL = 5000;
	private static final String NODE_REGISTER_KEY = "_mendmix_service_nodes:" + GlobalRuntimeContext.APPID;
	
	private StringRedisTemplate redisTemplate;
	
	private int workId;
	
	public RedisWorkIdGenerator() {
		try {
			redisTemplate = RedisTemplateGroups.getDefaultStringRedisTemplate();
		} catch (Exception e) {}
		GlobalRuntimeContext.setWorkIdGenerator(this);
	}
	
	private StringRedisTemplate redisTemplate() {
		if(redisTemplate != null) return redisTemplate;
		synchronized (this) {
			redisTemplate = RedisTemplateGroups.getDefaultStringRedisTemplate();
		}
		return redisTemplate;
	}




	@Override
	public int generate(String nodeId) {
		doSchedule();
		return workId;
	}

	
	@Override
	public void doSchedule() {
		long currentTime = System.currentTimeMillis();
		if(workId == 0) {
			RedisDistributeLock lock = new RedisDistributeLock(NODE_REGISTER_KEY);
			try {
				lock.lock();
				Set<String> nodeIds = redisTemplate().opsForZSet().rangeByScore(NODE_REGISTER_KEY, 0, currentTime + INTERVAL);
				if(nodeIds == null || nodeIds.isEmpty()) {
					workId = 1;
					logger.info("ZVOS-FRAMEWORK-TRACE-LOGGGING-->> init first workId:{}",workId);
				}else {
					nodeIds.stream().sorted().forEach(nodeId -> {
						long lastHeartbeatTime = redisTemplate.opsForZSet().score(NODE_REGISTER_KEY, nodeId).longValue();
						//节点已经下线
						if(workId == 0 && currentTime - lastHeartbeatTime > INTERVAL * 3) {
							workId = Integer.parseInt(nodeId);
							logger.info("ZVOS-FRAMEWORK-TRACE-LOGGGING-->> init workId:{} use expired workId",workId);
						}
					});
				}
				if(workId == 0) {
					workId = nodeIds.size() + 1;
					logger.info("ZVOS-FRAMEWORK-TRACE-LOGGGING-->> init workId:{} by incr",workId);
				}
				updateNodeSTat(currentTime);
			} finally {
				lock.unlock();
			}
		}else {
			updateNodeSTat(currentTime);
		}		
	}
				
	private void updateNodeSTat(long currentTime) {
		redisTemplate().opsForZSet().add(NODE_REGISTER_KEY, String.valueOf(workId), currentTime);
	}

	@Override
	public long interval() {
		return INTERVAL;
	}

}
