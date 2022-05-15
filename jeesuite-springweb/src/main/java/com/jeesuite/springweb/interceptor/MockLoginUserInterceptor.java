/*
 * Copyright 2016-2020 www.jeesuite.com.
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
package com.jeesuite.springweb.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.springweb.utils.UserMockUtils;

public class MockLoginUserInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
    	
    	if(CurrentRuntimeContext.getCurrentUser() == null){
    		CurrentRuntimeContext.setAuthUser(UserMockUtils.initMockContextIfOnCondition());
    	}
    	return true;
    }
}