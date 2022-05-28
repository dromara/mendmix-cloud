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
package com.mendmix.spring.web;

import java.util.Map;

import javax.servlet.ServletContextEvent;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.mendmix.spring.ApplicationStartedListener;
import com.mendmix.spring.InstanceFactory;



public class ContextLoaderListener extends org.springframework.web.context.ContextLoaderListener {

	@Override
	public void contextInitialized(ServletContextEvent event) {
		String serviceName = event.getServletContext().getInitParameter("appName");
		System.setProperty("serviceName", serviceName == null ? "undefined" : serviceName);
		super.contextInitialized(event);
		WebApplicationContext applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(event.getServletContext());
		InstanceFactory.setApplicationContext(applicationContext);
		
		Map<String, ApplicationStartedListener> interfaces = applicationContext.getBeansOfType(ApplicationStartedListener.class);
		if(interfaces != null){
			for (ApplicationStartedListener listener : interfaces.values()) {
				System.out.println(">>>begin to execute listener:"+listener.getClass().getName());
				listener.onApplicationStarted(applicationContext);
				System.out.println("<<<<finish execute listener:"+listener.getClass().getName());
			}
		}
	}
}
