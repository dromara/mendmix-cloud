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
package com.mendmix.springweb.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.mendmix.common.ThreadLocalContext;
import com.mendmix.springweb.enhancer.RequestBodyEnhancerAdvice;
import com.mendmix.springweb.servlet.CustomHttpServletRequestWrapper;

/**
 * 
 * <br>
 * Class Name   : HttpServletWrapperFilter
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date Feb 22, 2022
 */
public class HttpServletWrapperFilter  implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		
		CustomHttpServletRequestWrapper requestWrapper = null;
        if(RequestBodyEnhancerAdvice.cachinBody() 
        		&& request instanceof HttpServletRequest 
        		&& request.getInputStream() != null) {
        	requestWrapper = new CustomHttpServletRequestWrapper((HttpServletRequest)request);
        	byte[] body = requestWrapper.getBody();
        	ThreadLocalContext.set(RequestBodyEnhancerAdvice.CTX_CACHING_BODY_KEY, body);
        }
        
        if(requestWrapper == null) {
            chain.doFilter(request, response);
        } else {
            chain.doFilter(requestWrapper, response);
        }

	}


}
