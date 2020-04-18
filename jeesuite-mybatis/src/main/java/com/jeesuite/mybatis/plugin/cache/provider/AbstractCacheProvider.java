/**
 * 
 */
package com.jeesuite.mybatis.plugin.cache.provider;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.mybatis.plugin.cache.CacheProvider;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月13日
 */
public abstract class AbstractCacheProvider implements CacheProvider {
	
	protected static final Logger logger = LoggerFactory.getLogger(AbstractCacheProvider.class);
	
	protected static final char[] ID_PREFIX_CHARS = ("123456789".toCharArray());

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
