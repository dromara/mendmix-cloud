/**
 * 
 */
package com.jeesuite.mybatis.plugin.cache;

import java.util.concurrent.Callable;

import com.jeesuite.mybatis.core.BaseEntity;

/**
 * 实体缓存辅助工具（关联自动缓存内容）
 * <br>通过该工具的缓存会自动缓存更新
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月19日
 */
public class EntityCacheHelper {

	/**
	 * 查询并缓存结果(默认缓存一天)
	 * @param entityClass 实体类class (用户组装实际的缓存key)
	 * @param key 缓存的key（和entityClass一起组成真实的缓存key。<br>如entityClass=UserEntity.class,key=findlist，实际的key为：UserEntity.findlist）
	 * @param dataCaller 缓存不存在数据加载源
	 * @return
	 */
	public static <T> T queryTryCache(Class<? extends BaseEntity> entityClass,String key,Callable<T> dataCaller){
		return queryTryCache(entityClass, key, CacheExpires.IN_1DAY, dataCaller);
	}
	
	/**
	 * 查询并缓存结果
	 * @param entityClass 实体类class (用户组装实际的缓存key)
	 * @param key 缓存的key（和entityClass一起组成真实的缓存key。<br>如entityClass=UserEntity.class,key=findlist，实际的key为：UserEntity.findlist）
	 * @param expireSeconds 过期时间，单位：秒
	 * @param dataCaller 缓存不存在数据加载源
	 * @return
	 */
	public static <T> T queryTryCache(Class<? extends BaseEntity> entityClass,String key,long expireSeconds,Callable<T> dataCaller){
		
		if(CacheHandler.cacheProvider == null){
			try {
				return dataCaller.call();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		String entityClassName = entityClass.getSimpleName();
		key = entityClassName + CacheHandler.SPLIT_PONIT + key;
		T result = CacheHandler.cacheProvider.get(key);
		if(result == null){
			try {				
				result = dataCaller.call();
				if(result != null){
					CacheHandler.cacheProvider.set(key, result, expireSeconds,false);
					String cacheGroupKey = entityClassName + CacheHandler.GROUPKEY_SUFFIX;
					CacheHandler.cacheProvider.putGroup(cacheGroupKey, key, expireSeconds);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return result;
	}
	
	/**
	 * 移除指定实体组所有缓存
	 * @param entityClass
	 */
	public static void removeCache(Class<? extends BaseEntity> entityClass){
		if(CacheHandler.cacheProvider == null)return;
		String entityClassName = entityClass.getSimpleName();
		CacheHandler.cacheProvider.clearGroup(entityClassName,true);
	}
	
	/**
	 * 移除指定实体组指定key的缓存
	 * @param entityClass
	 * @param key
	 */
    public static void removeCache(Class<? extends BaseEntity> entityClass,String key){
    	if(CacheHandler.cacheProvider == null)return;
    	String entityClassName = entityClass.getSimpleName();
		key = entityClassName + CacheHandler.SPLIT_PONIT + key;
		String cacheGroupKey = entityClassName + CacheHandler.GROUPKEY_SUFFIX;
		
		CacheHandler.cacheProvider.remove(key);
		CacheHandler.cacheProvider.removeFromGroup(cacheGroupKey, key);
		
	}
}
