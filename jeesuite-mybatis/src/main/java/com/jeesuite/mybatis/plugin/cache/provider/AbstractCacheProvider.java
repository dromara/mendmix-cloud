/**
 * 
 */
package com.jeesuite.mybatis.plugin.cache.provider;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.cache.CacheExpires;
import com.jeesuite.mybatis.plugin.cache.CacheProvider;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月13日
 */
public abstract class AbstractCacheProvider implements CacheProvider {
	
	protected static final Logger logger = LoggerFactory.getLogger(AbstractCacheProvider.class);
	
	protected static final char[] ID_PREFIX_CHARS = ("123456789".toCharArray());
	
	// 计算关联key集合权重的基数
	protected long baseScoreInRegionKeysSet = System.currentTimeMillis() / 1000 - CacheExpires.IN_1WEEK;

	/**
	 * 避免关联key集合越积越多，按插入的先后顺序计算score便于后续定期删除。<br>
	 * Score 即为 实际过期时间的时间戳
	 * 
	 * @return
	 */
	protected long calcScoreInRegionKeysSet(long expireSeconds) {
		long currentTime = System.currentTimeMillis() / 1000;
		long score = currentTime + expireSeconds - this.baseScoreInRegionKeysSet;
		return score;
	}
	
	protected boolean isStoreAsString(Object o) {  
		   Class<? extends Object> clazz = o.getClass();
	       return 
	       (   
	           clazz.equals(String.class) ||   
	           clazz.equals(Integer.class)||   
	           clazz.equals(Byte.class) ||   
	           clazz.equals(Long.class) ||   
	           clazz.equals(Double.class) ||   
	           clazz.equals(Float.class) ||   
	           clazz.equals(Character.class) ||   
	           clazz.equals(Short.class) ||   
	           clazz.equals(BigDecimal.class) ||     
	           clazz.equals(Boolean.class) ||   
	           clazz.isPrimitive()   
	       );   
	   }

}
