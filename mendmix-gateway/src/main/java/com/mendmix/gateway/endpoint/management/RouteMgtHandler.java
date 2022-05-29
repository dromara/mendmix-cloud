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
package com.mendmix.gateway.endpoint.management;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.mendmix.common.model.NameValuePair;
import com.mendmix.common.model.SelectOption;
import com.mendmix.gateway.CurrentSystemHolder;
import com.mendmix.spring.InstanceFactory;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年3月19日
 */
public class RouteMgtHandler implements MgtHandler {

	@Override
	public String category() {
		return "route";
	}

	@Override
	public Object handleRequest(String actName, HandleParam handleParam) {
		if("list".equals(actName)) {
			List<SelectOption> options = CurrentSystemHolder.getModules().stream().map(o -> {
    			return new SelectOption(o.getRouteName(), o.getServiceId());
    		}).collect(Collectors.toList());
			return options;
		}else if("refresh".equals(actName)) {
			//模拟一条配置变更事件
			//List<NameValuePair> changeConfigs = Arrays.asList(new NameValuePair("spring.cloud.gateway.routes.x", null));
			//ConfigChangeEvent event = new ConfigChangeEvent(new Object(), changeConfigs);
			//InstanceFactory.getContext().publishEvent(event);
		}
		return null;
	}

}
