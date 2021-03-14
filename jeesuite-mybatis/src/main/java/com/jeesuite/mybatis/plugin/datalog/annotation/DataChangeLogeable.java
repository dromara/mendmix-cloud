package com.jeesuite.mybatis.plugin.datalog.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.jeesuite.mybatis.core.BaseEntity;

/**
 * 数据变更日志注解
 * <br>
 * Class Name   : DataChangeLogeable
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年10月26日
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface DataChangeLogeable {
 
	Class<? extends BaseEntity>[] entityClass();
}
