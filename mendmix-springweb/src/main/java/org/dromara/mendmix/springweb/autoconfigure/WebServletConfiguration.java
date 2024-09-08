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
package org.dromara.mendmix.springweb.autoconfigure;

import java.util.Map;

import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.springweb.exception.GlobalExceptionHandler;
import org.dromara.mendmix.springweb.exporter.AppMetadataServlet;
import org.dromara.mendmix.springweb.exporter.RuntimeConfigServlet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnWebApplication(type = Type.SERVLET)
public class WebServletConfiguration {

	
	@Bean
	public GlobalExceptionHandler globalExceptionHandler() {
		return new GlobalExceptionHandler();
	}

	@Bean
	public ServletRegistrationBean<AppMetadataServlet> appMetadataServlet() {
		Map<String, String> mappings = ResourceUtils.getMappingValues("mendmix-cloud.request.pathPrefix.mapping");
		ServletRegistrationBean<AppMetadataServlet> servletRegistrationBean;
	    final AppMetadataServlet appMetadataServlet = new AppMetadataServlet();
		servletRegistrationBean = new ServletRegistrationBean<>(appMetadataServlet);
		servletRegistrationBean.addUrlMappings(AppMetadataServlet.DEFAULT_URI);
        if(!mappings.isEmpty()) {
			String prefix;
			for (String packageName : mappings.keySet()) {
				prefix = mappings.get(packageName);
				if(!prefix.startsWith("/")) {
					prefix = "/" + prefix;
				}
				String uriPattern = prefix + AppMetadataServlet.DEFAULT_URI;
				appMetadataServlet.addUriSubPackageMapping(uriPattern,packageName);
				servletRegistrationBean.addUrlMappings(uriPattern);
			}
		}
	    return servletRegistrationBean;
	}
	
	@Bean
	public ServletRegistrationBean<RuntimeConfigServlet> runtimeConfigServlet() {
		ServletRegistrationBean<RuntimeConfigServlet> servletRegistrationBean;
		servletRegistrationBean = new ServletRegistrationBean<>(new RuntimeConfigServlet());
		servletRegistrationBean.addUrlMappings("/exporter/runtime_configs");
		return servletRegistrationBean;
	}
	

}
