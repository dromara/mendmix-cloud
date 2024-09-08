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

import org.dromara.mendmix.springweb.enhancer.RequestBodyEnhancer;
import org.dromara.mendmix.springweb.enhancer.RequestBodyEnhancerAdvice;
import org.dromara.mendmix.springweb.enhancer.ResonseBodyEnhancerAdvice;
import org.dromara.mendmix.springweb.enhancer.ResponseBodyEnhancer;
import org.dromara.mendmix.springweb.filter.HttpServletWrapperFilter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 
 * <br>
 * Class Name   : CustomBeanRegistProcessor
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date Feb 22, 2022
 */
@Configuration
@ConditionalOnWebApplication(type = Type.SERVLET)
public class EnhancerAdviceConfiguration implements BeanPostProcessor {

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof RequestBodyEnhancer) {
			RequestBodyEnhancerAdvice.register((RequestBodyEnhancer)bean);
		}
		if (bean instanceof ResponseBodyEnhancer) {
			ResonseBodyEnhancerAdvice.register((ResponseBodyEnhancer)bean);
		}
		return bean;
	}
	
	@Bean
	public RequestBodyEnhancerAdvice requestBodyEnhancerAdvice() {
		return new RequestBodyEnhancerAdvice();
	}
	
	@Bean
	public ResonseBodyEnhancerAdvice resonseBodyEnhancerAdvice() {
		return new ResonseBodyEnhancerAdvice();
	}
	
	@Bean
	public FilterRegistrationBean<HttpServletWrapperFilter> httpServletWrapperFilter() {
		FilterRegistrationBean<HttpServletWrapperFilter> filterBean = new FilterRegistrationBean<>();
		filterBean.setOrder(0);
		filterBean.addUrlPatterns("/*");
		HttpServletWrapperFilter filter = new HttpServletWrapperFilter();
		filterBean.setFilter(filter);
		return filterBean;
	}
}
