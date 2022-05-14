/*
 * Copyright 2016-2022 www.jeesuite.com.
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
package com.jeesuite.springweb.enhancer;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;

/**
 * 
 * <br>
 * Class Name   : RequestBodyEnhancer
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date Feb 22, 2022
 */
public interface RequestBodyEnhancer {

	Object process(HttpServletRequest request,Object body, MethodParameter parameter);
	
	int order();
}