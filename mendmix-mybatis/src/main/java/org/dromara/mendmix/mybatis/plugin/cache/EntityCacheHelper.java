/*
 * Copyright 2016-2020 dromara.org.
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
package org.dromara.mendmix.mybatis.plugin.cache;

import java.io.Serializable;
import java.util.concurrent.Callable;

import org.dromara.mendmix.cache.CacheUtils;
import org.dromara.mendmix.mybatis.core.BaseEntity;
import org.dromara.mendmix.mybatis.plugin.OnceContextVal;

/**
 * 实体缓存辅助工具（关联自动缓存内容）
 * <br>通过该工具的缓存会自动缓存更新
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月19日
 */
public class EntityCacheHelper {
	
	public static <T extends BaseEntity> void addCache(T bean,int expireSeconds){
		String key = buildCacheKey(bean.getClass(), bean.getId());
		CacheUtils.set(key,bean, expireSeconds);
	}

	/**
	 * 查询并缓存结果(默认缓存一天)
	 * @param entityClass 实体类class (用户组装实际的缓存key)
	 * @param key 缓存的key（和entityClass一起组成真实的缓存key。<br>如entityClass=UserEntity.class,key=findlist，实际的key为：UserEntity.findlist）
	 * @param dataCaller 缓存不存在数据加载源
	 * @return
	 */
	public static <T> T queryTryCache(Class<? extends BaseEntity> entityClass,String key,Callable<T> dataCaller){
		return queryTryCache(entityClass, key, CacheHandler.defaultCacheExpire, dataCaller);
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
		key = entityClassName + OnceContextVal.DOT + key;
		T result = CacheHandler.cacheProvider.get(key);
		if(result == null){
			try {				
				result = dataCaller.call();
				if(result != null){
					CacheHandler.cacheProvider.set(key, result, expireSeconds);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return result;
	}
	
	/**
	 * 移除指定实体组指定key的缓存
	 * @param entityClass
	 * @param key
	 */
    public static void removeCache(Class<? extends BaseEntity> entityClass,String key){
    	if(CacheHandler.cacheProvider == null)return;
    	String entityClassName = entityClass.getSimpleName();
		key = entityClassName + OnceContextVal.DOT + key;
		CacheHandler.cacheProvider.remove(key);
	}
	
	/**
	 * 移除指定对象缓存
	 * @param bean
	 */
	public static <T extends BaseEntity> void removeCache(T bean){
		if(CacheHandler.cacheProvider == null)return;
		String key = buildCacheKey(bean.getClass(), bean.getId());
		CacheHandler.cacheProvider.remove(key);
	}
	
	/**
	 * 移除指定实体组所有缓存
	 * @param entityClass
	 */
	public static void removeCache(Class<? extends BaseEntity> entityClass){
		if(CacheHandler.cacheProvider == null)return;
		String entityClassName = entityClass.getSimpleName();
	}
	
    
    public static String buildCacheKey(Class<?> entityClass,Serializable id){
		return entityClass.getSimpleName() + ".id:" + id;
	}
}
