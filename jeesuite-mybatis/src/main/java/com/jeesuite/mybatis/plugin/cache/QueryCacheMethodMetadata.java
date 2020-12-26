package com.jeesuite.mybatis.plugin.cache;

import org.apache.commons.lang3.RandomUtils;

import com.jeesuite.cache.CacheExpires;

/**
 *  查询方法缓存元信息
 * 
 * <br>
 * Class Name   : QueryMethodCache
 *
 * @author jiangwei
 * @version 1.0.0
 * @date Dec 23, 2020
 */
public class QueryCacheMethodMetadata {
	String methodName;
	String keyPattern;
	public String cacheGroupKey;//缓存组key
	long expire;//过期时间（秒）
	boolean isPk = false;//主键查询
	boolean uniqueIndex = false;
	boolean collectionResult = false;//查询结果是集合
	public boolean groupRalated = false; //是否需要关联group
	String[] fieldNames;//作为查询条件的字段名称
	boolean checkExpired = false; //是否需要检查缓存过期时间
	String contextParam;  
	boolean concurrency = true;
	String refKey;
	public QueryCacheMethodMetadata() {}

	public long getExpire() {
		if(expire == 0) {
			return CacheExpires.todayEndSeconds();
		}
		//缓存时间加上随机，防止造成缓存同时失效雪崩
		long rnd = RandomUtils.nextLong(0, CacheExpires.IN_1HOUR);
		return expire + (rnd > expire ? RandomUtils.nextLong(0, expire) : rnd);
	}
	
	/**
	 * 是否需要通过关联主键二次查询
	 * @return
	 */
	public boolean isSecondQueryById(){
		return isPk == false && uniqueIndex;
	}
}
