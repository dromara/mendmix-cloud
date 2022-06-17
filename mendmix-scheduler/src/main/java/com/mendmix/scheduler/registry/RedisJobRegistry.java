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
package com.mendmix.scheduler.registry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.async.StandardThreadExecutor.StandardThreadFactory;
import com.mendmix.common.util.JsonUtils;
import com.mendmix.scheduler.JobContext;
import com.mendmix.scheduler.model.JobConfig;
import com.mendmix.spring.InstanceFactory;


/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2021年12月18日
 */
public class RedisJobRegistry extends AbstarctJobRegistry implements DisposableBean{

private static final Logger logger = LoggerFactory.getLogger("com.zvosframework");
	
	private static final int SYNC_STAT_PERIOD = 5000;
	private static final Duration NODE_ACTIVE_MILLIS = Duration.ofMillis(SYNC_STAT_PERIOD * 3 + 1000);
	private static final String NODE_REGISTER_KEY = "__schedulerNodes:" + GlobalRuntimeContext.APPID;
	private static final String JOB_REGISTER_KEY = "__schedulerJobs:" + GlobalRuntimeContext.APPID;

	private StringRedisTemplate redisTemplate;
	
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new StandardThreadFactory("RedisJobRegistry"));
	
	public RedisJobRegistry() {}
	
	public RedisJobRegistry(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	private StringRedisTemplate getRedisTemplate() {
		if(redisTemplate == null) {
			return InstanceFactory.getInstance(StringRedisTemplate.class);
		}
		return redisTemplate;
	}

	public void setRedisTemplate(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}
	
	@Override
	public void register(JobConfig conf) {

		JobConfig onOherNodeJob = getConf(conf.getJobName(), true);
		if(onOherNodeJob != null && onOherNodeJob.getCurrentNodeId() != null) {
			long currentTime = System.currentTimeMillis();
			//当前注册的节点
			Set<String> nodeIds = getRedisTemplate().opsForZSet().rangeByScore(NODE_REGISTER_KEY, currentTime - SYNC_STAT_PERIOD - 1, currentTime + 10000);
		    //注册运行节点已下线
			if(nodeIds == null || !nodeIds.contains(onOherNodeJob.getCurrentNodeId())) {
				conf.setCurrentNodeId(null);
		    }
		}
		//
		if(conf.getCurrentNodeId() == null) {
			conf.setCurrentNodeId(GlobalRuntimeContext.getNodeName());
		}
		
		getRedisTemplate().opsForHash().put(JOB_REGISTER_KEY, conf.getJobName(), JsonUtils.toJson(conf));
		//
		updateNodeSTat(GlobalRuntimeContext.getNodeName(), System.currentTimeMillis());
	}
	
	@Override
	public void updateJobConfig(JobConfig conf) {
		conf.setModifyTime(System.currentTimeMillis());
		redisTemplate.opsForHash().put(JOB_REGISTER_KEY, conf.getJobName(), JsonUtils.toJson(conf));
	}

	@Override
	public void setRuning(String jobName, Date fireTime) {
		JobConfig config = getConf(jobName,false);
		config.setRunning(true);
		config.setLastFireTime(fireTime);
		config.setErrorMsg(null);
		// 更新本地
		schedulerConfgs.put(jobName, config);
		//
		updateJobConfig(config);
		
	}
	
	@Override
	public void setStoping(String jobName, Date nextFireTime, Exception e) {
		JobConfig config = getConf(jobName,false);
		config.setRunning(false);
		config.setNextFireTime(nextFireTime);
		config.setErrorMsg(e == null ? null : ExceptionUtils.getMessage(e));
		// 更新本地
		schedulerConfgs.put(jobName, config);
		//
		updateJobConfig(config);
		
	}
	
	@Override
	public JobConfig getConf(String jobName, boolean forceRemote) {
		JobConfig config = schedulerConfgs.get(jobName);
		if(config == null || forceRemote) {
			String json = Objects.toString(redisTemplate.opsForHash().get(JOB_REGISTER_KEY, jobName), null);
			if(json != null)config = JsonUtils.toObject(json, JobConfig.class);
		}
		return config;
	}

	@Override
	public void unregister(String jobName) {
		redisTemplate.opsForHash().delete(JOB_REGISTER_KEY, jobName);
	}

	@Override
	public List<JobConfig> getAllJobs() {
		return new ArrayList<>(schedulerConfgs.values());
	}
	
	@Override
	public void onRegistered() {
		coordinate();
		executor.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				coordinate();
			}
		}, 1000, 1000, TimeUnit.MILLISECONDS);
	}
	
	private void updateNodeSTat(String nodeId,long currentTime) {
		redisTemplate.opsForZSet().add(NODE_REGISTER_KEY, nodeId, currentTime);
		redisTemplate.expire(NODE_REGISTER_KEY, NODE_ACTIVE_MILLIS);
	}
	
	private void coordinate() {
		//
		long currentTime = System.currentTimeMillis();
		//同步所有任务
		Set<Object> jobNames = redisTemplate.opsForHash().keys(JOB_REGISTER_KEY);
		JobConfig config;
		for (Object jobName : jobNames) {
			config = getConf(jobName.toString(), true);
			schedulerConfgs.put(jobName.toString(), config);
		}
		
		Set<String> lastActiveNodes = JobContext.getContext().getActiveNodes();
		//同步节点
		Set<String> nodeIds = redisTemplate.opsForZSet().rangeByScore(NODE_REGISTER_KEY, 0, currentTime + 10000);
		//是否需要负载均衡
		boolean rebalanceRequired = lastActiveNodes.size() != nodeIds.size() || !lastActiveNodes.containsAll(nodeIds);
		Iterator<String> iterator = nodeIds.iterator();
		String nodeId;
		while(iterator.hasNext()) {
			nodeId = iterator.next();
			if(GlobalRuntimeContext.getNodeName().equals(nodeId)) {
				updateNodeSTat(nodeId, currentTime);
			}else {
				long lastHeartbeatTime = redisTemplate.opsForZSet().score(NODE_REGISTER_KEY, nodeId).longValue();
				//节点下线
				if(currentTime - lastHeartbeatTime > SYNC_STAT_PERIOD * 2) {
					redisTemplate.opsForZSet().remove(NODE_REGISTER_KEY, nodeId);
					iterator.remove();
					rebalanceRequired = true;
					logger.info("MENDMIX-TRACE-LOGGGING-->> scheduler node[{}] lastHeartbeatTime:{},removing...",nodeId,lastHeartbeatTime);
				}
			}
		}
		//
		if(rebalanceRequired) {
			List<String> activeNodeIds = new ArrayList<>(nodeIds);
			JobContext.getContext().refreshNodes(activeNodeIds);
			logger.info("MENDMIX-TRACE-LOGGGING-->> current activeNodeIds:{}",activeNodeIds);
			//
			rebalanceJobNode(activeNodeIds);
		}
		
	}

	@Override
	public void destroy() throws Exception {
		executor.shutdown();
	}
}
