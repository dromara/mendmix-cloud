/**
 * 
 */
package com.jeesuite.mybatis.plugin.cache.provider;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.cache.CacheExpires;
import com.jeesuite.mybatis.plugin.cache.CacheHandler;
import com.jeesuite.mybatis.plugin.cache.CacheProvider;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月13日
 */
public abstract class AbstractCacheProvider implements CacheProvider {

	protected static final Logger logger = LoggerFactory.getLogger(AbstractCacheProvider.class);
	
	protected int batchSize = 100;
	
	protected String groupName;
	
	public AbstractCacheProvider(String groupName) {
		this.groupName = groupName;
	}

	protected boolean isStoreAsString(Object o) {
		Class<? extends Object> clazz = o.getClass();
		return (clazz.equals(String.class) || clazz.equals(Integer.class) || clazz.equals(Byte.class)
				|| clazz.equals(Long.class) || clazz.equals(Double.class) || clazz.equals(Float.class)
				|| clazz.equals(Character.class) || clazz.equals(Short.class) || clazz.equals(BigDecimal.class)
				|| clazz.equals(Boolean.class) || clazz.isPrimitive());
	}
	
	@Override
	public void clearGroup(final String groupName,String ...prefixs) {
		String cacheGroupKey = groupName.endsWith(CacheHandler.GROUPKEY_SUFFIX) ? groupName : groupName + CacheHandler.GROUPKEY_SUFFIX;
		int keyCount = (int) getListSize(cacheGroupKey);
		if(keyCount <= 0)return;
	    //保护策略
		if(keyCount > 1000) {
			setExpire(cacheGroupKey, CacheExpires.todayEndSeconds());
		}
		
		boolean withPrefixs = prefixs != null && prefixs.length > 0 && prefixs[0] != null;
		
		int toIndex;
		List<String> keys;
		for (int i = 0; i <= keyCount; i+=batchSize) {
			toIndex = (i + batchSize) > keyCount ? keyCount : (i + batchSize);
			keys = getListItems(cacheGroupKey,i, toIndex);
			if(keys.isEmpty())break;
			//
			if(withPrefixs) {
				keys = keys.stream().filter(key -> {
					for (String prefix : prefixs) {
						if(key.contains(prefix))return true;
					}
					return false;
				}).collect(Collectors.toList());
			}
			if(keys.isEmpty())continue;
			//
			remove(keys.toArray(new String[0]));
			if(logger.isDebugEnabled()) {
				logger.debug("_clearGroupKey -> group:{},keys:{}",groupName,Arrays.toString(keys.toArray()));
			}
		}
		//
		remove(cacheGroupKey);
	}

}
