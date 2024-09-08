/*
 * Copyright 2016-2020 dromara.org.
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
package org.dromara.mendmix.springweb.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.springweb.utils.UserMockUtils;
import org.springframework.web.servlet.HandlerInterceptor;

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