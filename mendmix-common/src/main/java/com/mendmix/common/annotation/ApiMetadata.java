/*
 * Copyright 2016-2022 www.mendmix.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mendmix.common.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.mendmix.common.constants.PermissionLevel;

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
	boolean requestLog() default false;
	
	/**
	 * 是否记录请求日志
	 * @return
	 */
	boolean responseLog() default false;
	
	/**
	 * 是否保持response，即网关不作统一包装
	 * @return
	 */
	boolean responseKeep() default false;
	
	boolean openApi() default false;
	
}
