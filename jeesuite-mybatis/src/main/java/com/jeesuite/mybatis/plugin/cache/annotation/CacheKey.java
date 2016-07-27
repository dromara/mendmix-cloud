package com.jeesuite.mybatis.plugin.cache.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;


/**
 * 实体按字段缓存标注（改字段必须具有唯一性）
 * 如果实体指定字段加了<code>CacheKey</code>标注，通过findByXxxxx()查询将会自动缓存<br>
 * 如：字段名为deviceId,则对于方法名需定义为：findByDeviceId(String deviceId)
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年10月30日
 * @Copyright (c) 2015, jwww
 */
@Target({FIELD,PARAMETER}) 
@Retention(RUNTIME)
public @interface CacheKey {
	
	/**
	 * 指定对应实体字段名
	 * @return
	 */
	String value() default "";
	
	/**
	 * 是否仅缓存引用关系<br>
	 * 如果为true ，则仅缓存与按主键缓存的应用关系
	 * @return
	 */
	boolean isRef() default true;
}
