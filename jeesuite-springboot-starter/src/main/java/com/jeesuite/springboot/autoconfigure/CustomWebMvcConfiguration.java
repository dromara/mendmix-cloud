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
package com.jeesuite.springboot.autoconfigure;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.springweb.interceptor.GlobalDefaultInterceptor;
import com.jeesuite.springweb.interceptor.MockLoginUserInterceptor;
import com.jeesuite.springweb.utils.UserMockUtils;

@Configuration
@ConditionalOnWebApplication(type = Type.SERVLET)
public class CustomWebMvcConfiguration implements WebMvcConfigurer {

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {

		Charset charset = Charset.forName(ResourceUtils.getProperty("jeesuite.response.charset.name", "UTF-8"));

		for (HttpMessageConverter<?> converter : converters) {
			// 解决controller返回普通文本中文乱码问题
			if (converter instanceof StringHttpMessageConverter) {
				((StringHttpMessageConverter) converter).setDefaultCharset(charset);
			}
			// 解决controller返回json对象中文乱码问题
			if (converter instanceof MappingJackson2HttpMessageConverter) {
				((MappingJackson2HttpMessageConverter) converter).setDefaultCharset(charset);
			}
		}
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new GlobalDefaultInterceptor())
		       .addPathPatterns("/**")
		       .excludePathPatterns("/error","/swagger-ui.html", "/v2/api-docs", "/swagger-resources/**", "/webjars/**", "/info", "/health");

		if (UserMockUtils.isEnabled()) {
			registry.addInterceptor(new MockLoginUserInterceptor()).addPathPatterns("/**");
		}
	}
	
	@Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
		String pathPrefix = ResourceUtils.getProperty("jeesuite.request.pathPrefix");
		if(pathPrefix != null) {
			configurer.addPathPrefix(pathPrefix,c -> true);
		}
		
		Map<String, String> mappings = ResourceUtils.getMappingValues("jeesuite.request.pathPrefix.mapping");
		if(!mappings.isEmpty()) {
			mappings.forEach( (packageName,prefix) -> {
				configurer.addPathPrefix(prefix,HandlerTypePredicate.forBasePackage(packageName));
			} );
		}
		
    }
	
	@Override
    public void addCorsMappings(CorsRegistry registry) {
		if (!ResourceUtils.getBoolean("jeesuite.request.cors.enabled"))return;
        registry.addMapping("/**").allowedOrigins("*");
    }

}
