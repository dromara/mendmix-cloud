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
package com.jeesuite.springboot.autoconfigure.feign;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import com.jeesuite.common.GlobalConstants;
import com.jeesuite.common.util.ResourceUtils;

import feign.Feign.Builder;
import feign.Target;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年5月14日
 */
@Configuration
@ConditionalOnClass(feign.RequestInterceptor.class)
public class CustomFeignBuilder extends Builder {

	private Map<String, String> contextPathMappings = ResourceUtils.getMappingValues("jeesuite.feign.contextPath");
	
	@Override
	public <T> T target(Target<T> target) {
		return super.target(new Target.HardCodedTarget<T>(target.type(), target.name(), target.url()) {
			@Override
			public String url() {
				String svcName = this.name();
				if(contextPathMappings.containsKey(svcName)) {
					StringBuilder withContextPath = new StringBuilder(svcName).append(GlobalConstants.PATH_SEPARATOR).append(contextPathMappings.get(svcName));
					String url = super.url().replace(svcName, withContextPath);
					return url;
				}
				return super.url();
			}
			
			
		});
	}

}
