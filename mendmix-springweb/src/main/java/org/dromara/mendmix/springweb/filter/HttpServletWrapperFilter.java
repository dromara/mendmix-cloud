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
package org.dromara.mendmix.springweb.filter;

import java.io.IOException;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.model.ApiInfo;
import org.dromara.mendmix.common.util.JsonUtils;
import org.dromara.mendmix.common.util.WebUtils;
import org.dromara.mendmix.logging.LogConfigs;
import org.dromara.mendmix.springweb.exporter.AppMetadataHolder;
import org.dromara.mendmix.springweb.servlet.CustomHttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.springweb");
	
    private boolean apilogEnabled = LogConfigs.API_LOGGING;
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
	
		ThreadLocalContext.unset();
		
		CustomHttpServletRequestWrapper requestWrapper = null;
        if(request instanceof HttpServletRequest) {
        	HttpServletRequest _request = (HttpServletRequest) request;
        	if(WebUtils.isJsonRequest(_request)) {
        		ApiInfo apiInfo = AppMetadataHolder.getCurrentApiInfo();
            	if(CurrentRuntimeContext.isDebugMode()) {
            		logger.info(">>HttpServletWrapperFilter currentAPI:{}",apiInfo);
            	}
        		if(apiInfo != null) {
            		requestWrapper = new CustomHttpServletRequestWrapper(_request);
            		String bodyString = requestWrapper.getBody();
            	    if(StringUtils.isNotBlank(bodyString)) {
            	    	Object body;
            	    	try {
            	    		if(JsonUtils.isJsonObjectString(bodyString)) {
                	    		body = JsonUtils.toHashMap(bodyString, Object.class);
                	    	}else {
                	    		body = JsonUtils.toList(bodyString, Map.class);
                	    	}
						} catch (Exception e) {
							body = bodyString;
						}
            	    	ThreadLocalContext.set(GlobalConstants.CONTEXT_REQUEST_BODY_KEY, body);
                        if(CurrentRuntimeContext.isDebugMode()) {
                    		logger.info(">>HttpServletWrapperFilter cachingBody:{}",bodyString);
                    	}
            	    }
            	}
        	}
        }
        
        ThreadLocalContext.markReset();
        
        if(requestWrapper == null) {
            chain.doFilter(request, response);
        } else {
            chain.doFilter(requestWrapper, response);
        }

	}

}
