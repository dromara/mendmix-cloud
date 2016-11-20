/**
 * 
 */
package com.jeesuite.mybatis.plugin.cache;

import com.jeesuite.mybatis.core.BaseEntity;

/**
 * 实体缓存辅助工具（关联自动缓存内容）
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月19日
 */
public class EntityCacheHelper {

	public static  void cache(Class<? extends BaseEntity> entityClass,String key,Object value){
		CacheHandler.cacheProvider.clearGroupKeys(entityClass.getSimpleName() + CacheHandler.GROUPKEY_SUFFIX);
		CacheHandler.cacheProvider.putGroupKeys(entityClass.getSimpleName() + CacheHandler.GROUPKEY_SUFFIX, key, CacheExpires.IN_1DAY);
	}
	
	public static <T extends BaseEntity> void cache(T entity){
		String entityClassName = entity.getClass().getSimpleName();
		String key = entityClassName + CacheHandler.SPLIT_PONIT + entity.getId();
		CacheHandler.cacheProvider.set(key, entity,CacheExpires.IN_1DAY);
		CacheHandler.cacheProvider.clearGroupKeys(entityClassName + CacheHandler.GROUPKEY_SUFFIX);
	}
	
    public static <T extends BaseEntity> void removeCache(T entity){
    	String entityClassName = entity.getClass().getSimpleName();
    	String key = entityClassName + CacheHandler.SPLIT_PONIT + entity.getId();
    	CacheHandler.cacheProvider.remove(key);
    	CacheHandler.cacheProvider.clearGroupKeys(entityClassName + CacheHandler.GROUPKEY_SUFFIX);
	}
    
    public static <T extends BaseEntity> void removeCache(Class<? extends BaseEntity> entityClass,String key){
    	CacheHandler.cacheProvider.remove(key);
    	CacheHandler.cacheProvider.clearGroupKey(entityClass.getSimpleName() + CacheHandler.GROUPKEY_SUFFIX, key);
    }
}
