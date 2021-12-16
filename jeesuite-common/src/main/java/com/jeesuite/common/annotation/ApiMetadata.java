package com.jeesuite.common.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.jeesuite.common.constants.PermissionLevel;

@Retention(RUNTIME)
@Target({ TYPE, METHOD })
/**
 * 
 * <br>
 * Class Name   : ApiMetadata
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年7月12日
 */
public @interface ApiMetadata {
	/**
	 * 操作名称
	 * @return
	 */
	String actionName() default "";
	/**
	 * 是否仅内网访问
	 * @return
	 */
	boolean IntranetAccessOnly() default false;
	
	/**
	 * 接口权限级别
	 * @return
	 */
	PermissionLevel permissionLevel() default PermissionLevel.PermissionRequired;
	
	/**
	 * 是否记录操作日志
	 * @return
	 */
	boolean actionLog() default true;
	
	/**
	 * 是否记录请求日志
	 * @return
	 */
	boolean requestLog() default true;
	
	/**
	 * 是否记录请求日志
	 * @return
	 */
	boolean responseLog() default true;
	
	/**
	 * 是否保持response，即网关不作统一包装
	 * @return
	 */
	boolean responseKeep() default false;
	
}
