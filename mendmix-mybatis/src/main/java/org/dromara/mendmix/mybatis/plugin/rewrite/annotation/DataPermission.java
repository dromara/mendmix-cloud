package org.dromara.mendmix.mybatis.plugin.rewrite.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(METHOD)
public @interface DataPermission {
	
	boolean handleJoin() default true;
	
	boolean handleOwner() default true;
	/**
	 * 定义表的策略
	 * @return
	 */
	DataPermissionItem[] strategy() default {};
	
	boolean joinConditionWithOn() default true;

}
