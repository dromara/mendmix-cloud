/*
 * Copyright 2016-2022 dromara.org.
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
package org.dromara.mendmix.spring;

import java.lang.reflect.Method;

/**
 * 全局拦截器处理handler
 * 
 * <br>
 * Class Name   : InterceptorHanlder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date Oct 31, 2020
 */
public interface InterceptorHanlder {

	void preHandler(Method method,Object[] args);
	
	void postHandler(Object result,Exception ex);
	
	int priority();
}
