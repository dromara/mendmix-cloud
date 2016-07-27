package com.jeesuite.mybatis.plugin.rwseparate.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;


/**
 * 主库路由：标识该方法使用从库（只读库）
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年11月18日
 * @Copyright (c) 2015, jwww
 */
@Target({METHOD, FIELD}) 
@Retention(RUNTIME)
public @interface Slave {
	
}
