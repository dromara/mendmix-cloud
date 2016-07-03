/**
 * 
 */
package com.jeesuite.scheduler.distribute;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.jeesuite.common.serializer.SerializeUtils;
import com.jeesuite.scheduler.ControlHandler;
import com.jeesuite.scheduler.SchedulerConfg;
import com.jeesuite.scheduler.SchedulerContext;

import redis.clients.jedis.Jedis;
import redis.clients.util.SafeEncoder;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年7月3日
 */
public class RedisControlHandler implements ControlHandler,InitializingBean,DisposableBean {

	private static final Logger logger = LoggerFactory.getLogger(RedisControlHandler.class);
	
	/**
	 * 
	 */
	private static final String REDIS_KEY_PREFIX = "dis_tasks:";
	private Map<String, SchedulerConfg> schedulerConfgs = new ConcurrentHashMap<>();
	private String servers;
	private Jedis jedis;
	
	//是否第一个启动节点
	boolean isFirstNode = false;
	
	private String nodeRedisKey;
	
	private final static long NODE_STATE_EXPIRE_MILS = 30 * 1000;
	
	private Timer heartBeatTimer;
	
	public void setServers(String servers) {
		this.servers = servers;
	}

	@Override
	public synchronized void register(SchedulerConfg conf) {
		byte[] cacheKey = getCacheKey(conf);
		
		if(nodeRedisKey == null){
			nodeRedisKey = "dis_tasks.nodes." + conf.getGroupName() + ":" + SchedulerContext.getNodeId();
		}
		if(!isFirstNode){
			Set<String> nodesKeys = jedis.keys("dis_tasks.nodes." + conf.getGroupName()+"*");
			isFirstNode = nodesKeys == null || nodesKeys.isEmpty();
			if(isFirstNode)jedis.set(nodeRedisKey, String.valueOf(1));
		}
		
		if(isFirstNode){
			jedis.set(cacheKey, SerializeUtils.serialize(conf));
		}else{
			conf = (SchedulerConfg) SerializeUtils.deserialize(jedis.get(cacheKey));
		}

		schedulerConfgs.put(conf.getJobName(), conf);
		
		if(heartBeatTimer == null){
			heartBeatTimer = new Timer(true);
			heartBeatTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					jedis.pexpire(nodeRedisKey, NODE_STATE_EXPIRE_MILS);
				}
			}, 1000, NODE_STATE_EXPIRE_MILS - 1000);
		}
		
		logger.info("finish register schConfig:{}",ToStringBuilder.reflectionToString(conf, ToStringStyle.MULTI_LINE_STYLE));
	}

	@Override
	public synchronized void setRuning(String jobName, Date fireTime) {
		SchedulerConfg config = getConf(jobName,true);
		config.setRunning(true);
		config.setLastFireTime(fireTime);
		config.setCurrentNodeId(SchedulerContext.getNodeId());
		
		jedis.set(getCacheKey(config), SerializeUtils.serialize(config));
	}

	@Override
	public synchronized void setStoping(String jobName, Date nextFireTime) {
		SchedulerConfg config = getConf(jobName,true);
		config.setRunning(false);
		config.setNextFireTime(nextFireTime);
		//非强制单点模式清除执行节点id
		if(!SchedulerContext.isSingleMode()){
			config.setCurrentNodeId(null);
		}
		jedis.set(getCacheKey(config), SerializeUtils.serialize(config));
	}

	@Override
	public synchronized SchedulerConfg getConf(String jobName,boolean forceRemote) {
		SchedulerConfg config = schedulerConfgs.get(jobName);
		byte[] cacheKey = getCacheKey(config);
		return (SchedulerConfg) SerializeUtils.deserialize(jedis.get(cacheKey));
	}


	@Override
	public synchronized void unregister(String jobName) {
		SchedulerConfg config = getConf(jobName,true);
		
		jedis.del(nodeRedisKey);
		
		Set<String> nodesKeys = jedis.keys("dis_tasks.nodes." + config.getGroupName()+"*");
		if(nodesKeys == null || nodesKeys.isEmpty()){
			byte[] cacheKey = getCacheKey(config);
			jedis.del(cacheKey);
			logger.info("all node is stop delete task config cache,key:" + SafeEncoder.encode(cacheKey));
		}
	}


	@Override
	public synchronized void destroy() throws Exception {
		if(jedis == null)return;
		jedis.close();
		jedis = null;
		heartBeatTimer.cancel();
	}

	private byte[] getCacheKey(SchedulerConfg config){
		return SafeEncoder.encode(REDIS_KEY_PREFIX + config.getGroupName() + "." + config.getJobName());
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		String[] strings = servers.split(":");
		jedis = new Jedis(strings[0], Integer.parseInt(strings[1]));
	}

}
