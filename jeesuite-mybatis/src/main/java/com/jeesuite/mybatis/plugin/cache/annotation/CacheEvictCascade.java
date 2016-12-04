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

import com.jeesuite.mybatis.core.BaseEntity;

/**
 * 缓存关联更新
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月4日
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CacheEvictCascade {
	/**
	 * 级联更新其他的实体组
	 * @return
	 */
	Class<? extends BaseEntity>[] cascadeEntities() default {};
}
