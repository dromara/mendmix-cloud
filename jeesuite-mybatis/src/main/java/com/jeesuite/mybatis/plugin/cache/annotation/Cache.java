/**
 * 
 */
package com.jeesuite.mybatis.plugin.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月10日
 * @Copyright (c) 2015, jwww
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Cache {
	/**
	 * 过期时间(单位：秒)
	 * @return
	 */
	long expire() default 0;
	
	/**
	 * 是否业务上唯一索引
	 * @return
	 */
	boolean uniqueIndex() default false;

	/**
	 * 是否允许并发查询db
	 * @return
	 */
	boolean concurrency() default true;
	/**
	 * 缓存范围是否当前登录用户
	 * @return
	 */
	boolean userScope() default false;
	
	/**
	 * 缓存范围：
	 * @return
	 */
	String[] scopeContext() default {};
	
	/**
	 * 配置在那些方法执行后自动清除缓存<br>
	 * 支持通配符。like:[UserEntityMapper.updateType,AccountEntityMapper.*]
	 * @return
	 */
	String[] evictOnMethods() default {};
	
	/**
	 * 引用缓存key
	 * @return
	 */
	String[] refKey() default {};
}
