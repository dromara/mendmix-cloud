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
	long expire() default 60 * 60 * 24;
	
	/**
	 * 结果是否唯一记录
	 * @return
	 */
	boolean uniqueResult() default true;
	
	String keyPattern() default "";
}
