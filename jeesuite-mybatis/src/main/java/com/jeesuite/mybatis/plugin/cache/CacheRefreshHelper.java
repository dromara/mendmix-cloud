/**
 * Confidential and Proprietary Copyright 2019 By 卓越里程教育科技有限公司 All Rights Reserved
 */
package com.jeesuite.mybatis.plugin.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.ibatis.mapping.MappedStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.jeesuite.mybatis.MybatisRuntimeContext;

/**
 * 
 * <br>
 * Class Name   : CacheRefreshHelper
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年2月14日
 */
public class CacheRefreshHelper {

	protected static final Logger logger = LoggerFactory.getLogger(CacheRefreshHelper.class);
	
	private static final String VALUE_TEMPLATE = "_%s_%s";
	private static final String KEY_NAME = "mybatis:markCleanUserRalations";
	private static List<String> updateMethods = Arrays.asList("insert","insertSelective","updateByPrimaryKeySelective","updateByPrimaryKey","deleteByPrimaryKey");
	// 计算关联key集合权重的基数
	private static long baseScore = System.currentTimeMillis() / 1000 - 3600;
	private static final int TIME_PERIOD_SECONDS = 300;

	//<mtId,[关联查询方法列表]>
	private static Map<String, List<String>> requireCleanCacheMappedStatementIds = new HashMap<>();
		
	private StringRedisTemplate redisTemplate;
	
	private ScheduledExecutorService clearExpiredMarkKeysTimer = Executors.newScheduledThreadPool(1);
	
	private long minScore = 0;
	public CacheRefreshHelper(StringRedisTemplate redisTemplate,List<String> relationQueryMapperIds) {
		this.redisTemplate = redisTemplate;
		buildConfigs(relationQueryMapperIds);
		//
		clearExpiredMarkKeysTimer.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				//TODO 
				long maxScore = System.currentTimeMillis()/1000 - baseScore;
				Long res = redisTemplate.opsForZSet().removeRangeByScore(KEY_NAME, minScore, maxScore);
				minScore = maxScore-1;
				logger.info("clearExpiredMarkKeysTimer runing:cacheName:{} , score range:0~{} ,result:{}",KEY_NAME,maxScore,res);
			}
		}, TIME_PERIOD_SECONDS, TIME_PERIOD_SECONDS, TimeUnit.SECONDS);
	}

	/**
	 * @param relationQueryMapperIds
	 */
	private void buildConfigs(List<String> relationQueryMapperIds) {
		Map<String, List<String>> groupMaps = new HashMap<>();
		for (String mapperId : relationQueryMapperIds) {
			String mapperClassName = mapperId.substring(0,mapperId.lastIndexOf(".") + 1);
			if(!groupMaps.containsKey(mapperClassName)){
				groupMaps.put(mapperClassName, new ArrayList<>());
			}
			groupMaps.get(mapperClassName).add(mapperId);
		}
		
		for (String prefix : groupMaps.keySet()) {
			for (String updateMethod : updateMethods) {
				String key = prefix + updateMethod;
				requireCleanCacheMappedStatementIds.put(key, groupMaps.get(prefix));
			}
		}
		
		logger.info("load requireCleanCacheMappedStatementIds Ok >>\n {}",requireCleanCacheMappedStatementIds);
	}
	
	public void reloadConfigs(List<String> relationQueryMapperIds){
		synchronized (requireCleanCacheMappedStatementIds) {
			requireCleanCacheMappedStatementIds.clear();
			buildConfigs(relationQueryMapperIds);
		}
	}

	public boolean skipCache(MappedStatement mt){
		String userId = MybatisRuntimeContext.getCurrentUserId();
		if(userId == null)return false;
		boolean methodSkip = false;
		String mapperId = mt.getId();
		for (List<String> mtIds : requireCleanCacheMappedStatementIds.values()) {
			if(methodSkip = mtIds.contains(mapperId))break;
		}
		if(methodSkip){
			String value = String.format(VALUE_TEMPLATE, userId,mapperId);
			//Long res = redisTemplate.opsForSet().remove(KEY_NAME, value);
			Long res = redisTemplate.opsForZSet().remove(KEY_NAME, value);
			return res > 0;
		}
		return false;
	}

	public void tryRemarkCleanRalationCache(MappedStatement mt){
		String userId = MybatisRuntimeContext.getCurrentUserId();
		if(userId == null)return;
		String mapperId = mt.getId();
		if(!requireCleanCacheMappedStatementIds.containsKey(mapperId))return;
		List<String> list = requireCleanCacheMappedStatementIds.get(mapperId);
		//
		long score = System.currentTimeMillis() / 1000 - baseScore;
		for (int i = 0; i < list.size(); i++) {
			String value = String.format(VALUE_TEMPLATE, userId,list.get(i));
			redisTemplate.opsForZSet().add(KEY_NAME, value, score);
			if(logger.isDebugEnabled())logger.debug(">>cache_mark_expired ADD :{},cleanScore:{}",value,score);
		}
	}
	
	public void rearkCleanRalationCache(String userId,String[] queryMethods){
		long score = System.currentTimeMillis() / 1000 - baseScore;
		for (String queryMethod : queryMethods) {
			String value = String.format(VALUE_TEMPLATE, userId,queryMethod);
			redisTemplate.opsForZSet().add(KEY_NAME, value, score);
		}
	}
	
	public void close(){
		clearExpiredMarkKeysTimer.shutdown();
	}
}
