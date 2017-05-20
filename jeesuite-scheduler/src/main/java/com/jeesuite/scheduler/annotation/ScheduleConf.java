/**
 * 
 */
package com.jeesuite.scheduler.annotation;

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
@Target({ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface ScheduleConf {
	/**
	 * 任务名称
	 * @return
	 */
	String jobName();
	
	/**
	 * 重试次数
	 * @return
	 */
	int retries() default 0;
	
	String cronExpr();
	/**
	 * 是否启动立即执行一次
	 * @return
	 */
	boolean executeOnStarted() default false;
}
