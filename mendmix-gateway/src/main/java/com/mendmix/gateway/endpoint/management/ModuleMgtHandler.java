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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import com.mendmix.common.util.BeanUtils;
import com.mendmix.gateway.CurrentSystemHolder;
import com.mendmix.gateway.model.BizSystemModule;
import com.mendmix.spring.InstanceFactory;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年3月19日
 */
public class ModuleMgtHandler implements MgtHandler {

	@Override
	public String category() {
		return "module";
	}

	@Override
	public Object handleRequest(String actName,HandleParam handleParam) {
		
		String serviceId = handleParam.getParameter("serviceId");
		Collection<BizSystemModule> modules = CurrentSystemHolder.getModules();
		
		if("list".equals(actName)) {
    		if(StringUtils.isBlank(serviceId)) {
    			List<BizSystemModule> filterList = modules.stream().filter(
    					o -> !o.isGlobal() && !o.isGateway()
    			).collect(Collectors.toList());
    			//
    			if(!Boolean.parseBoolean(handleParam.getParameter("details"))) {
    				filterList = BeanUtils.copy(filterList, BizSystemModule.class);
    				for (BizSystemModule module : filterList) {
    					module.setApiInfos(null);
					}
    			}
				return filterList;
    		}else {
    			return modules.stream().filter(
    					o -> serviceId.equalsIgnoreCase(o.getServiceId()) || serviceId.equals(o.getRouteName())
    			).findFirst().orElse(null);
    		}
    	}else if("instances".equals(actName)) {
    		Map<String, List<ServiceInstance>> map = new HashMap<>(modules.size());
    		for (BizSystemModule module : modules) {
    			if(serviceId != null && !StringUtils.equalsIgnoreCase(serviceId, module.getServiceId())) {
    				continue;
    			}
    			List<ServiceInstance> instances = InstanceFactory.getInstance(DiscoveryClient.class).getInstances(module.getServiceId());
    			map.put(module.getServiceId(), instances);
    		}
    		
    		return map;
    	}
		return null;
	}

}
