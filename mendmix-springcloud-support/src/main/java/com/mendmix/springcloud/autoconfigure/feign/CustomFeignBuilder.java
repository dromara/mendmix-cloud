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
package com.mendmix.springcloud.autoconfigure.feign;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import com.mendmix.common.http.CustomRequestHostHolder;

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


	@Override
	public <T> T target(Target<T> target) {
		return super.target(new Target.HardCodedTarget<T>(target.type(), target.name(), target.url()) {
			@Override
			public String url() {
				String svcName = this.name();
				if(CustomRequestHostHolder.containsContextPathMapping(svcName)) {
					StringBuilder withContextPath = new StringBuilder(svcName).append(CustomRequestHostHolder.getContextPathMapping(svcName));
					String url = super.url().replace(svcName, withContextPath);
					return url;
				}
				return super.url();
			}
			
			
		});
	}

}
