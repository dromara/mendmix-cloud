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
package org.dromara.mendmix.springweb.autoconfigure.feign;

import java.util.Map;

import org.dromara.mendmix.common.CustomRequestHeaders;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.springweb.client.RequestHeaderBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

import feign.RequestInterceptor;
import feign.RequestTemplate;

@Configuration
@ConditionalOnClass(feign.RequestInterceptor.class)
public class CustomRequestInterceptor implements RequestInterceptor {

	@Override
	public void apply(RequestTemplate template) {
		Map<String, String> customHeaders = RequestHeaderBuilder.getHeaders();
		customHeaders.forEach((k,v)->{					
			template.header(k, v);
		}); 
		template.header(HttpHeaders.USER_AGENT, GlobalConstants.FEIGN_USER_AGENT_NAME);
		//保持原始http状态码
		template.header(CustomRequestHeaders.HEADER_HTTP_STATUS_KEEP, Boolean.TRUE.toString());
		//标记不需要封装
		template.header(CustomRequestHeaders.HEADER_RESP_KEEP, Boolean.TRUE.toString());
		//内网访问
		template.header(CustomRequestHeaders.HEADER_INTERNAL_REQUEST, Boolean.TRUE.toString());
	}

}
