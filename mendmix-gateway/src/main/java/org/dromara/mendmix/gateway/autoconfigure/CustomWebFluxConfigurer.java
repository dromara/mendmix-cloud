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
package org.dromara.mendmix.gateway.autoconfigure;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.config.PathMatchConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.gateway.GatewayConfigs;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Jun 14, 2022
 */
@Configuration
public class CustomWebFluxConfigurer implements WebFluxConfigurer {

	@Override
	public void configurePathMatching(PathMatchConfigurer configurer) {
		configurer.addPathPrefix(GatewayConfigs.PATH_PREFIX,c -> true);
	}
	
	@Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
		String propValue = ResourceUtils.getProperty("spring.codec.max-in-memory-size", String.valueOf(10 * 1024 * 1024));
		int size;
        if(StringUtils.isNumeric(propValue)) {
        	size = Integer.parseInt(propValue);
        }else {
        	size = (int) DataSize.parse(propValue).toBytes();
        }
		configurer.defaultCodecs().maxInMemorySize(size);
    }

	
}
