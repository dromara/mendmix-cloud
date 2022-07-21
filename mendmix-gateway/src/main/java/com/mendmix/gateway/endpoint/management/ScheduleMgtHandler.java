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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.http.HttpRequestEntity;
import com.mendmix.gateway.CurrentSystemHolder;
import com.mendmix.gateway.model.BizSystemModule;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年3月19日
 */
public class ScheduleMgtHandler implements MgtHandler {

	private static List<String> ignoreServiceIds = new ArrayList<>();
	
	@Override
	public String category() {
		return "schedule";
	}

	@Override
	public Object handleRequest(String actName, HandleParam handleParam) {
		if("list".equals(actName)) {
			Collection<BizSystemModule> modules = CurrentSystemHolder.getModules();
			List<Map> list = new ArrayList<>();
			for (BizSystemModule module : modules) {
				if(module.isGlobal() || module.isGateway())continue;
				if(ignoreServiceIds.contains(module.getServiceId()))continue;
				list.addAll(fetchModuleJobs(module));
				
			}
			return list;
		}else if(handleParam.isPostMethod()) {
	
		}
		return null;
	}
	
	
	@SuppressWarnings("rawtypes")
	private List<Map> fetchModuleJobs(BizSystemModule module) {
		
		List<Map> jobs;
		try {
			String url = module.getHttpBaseUri() + "/scheduler/status";
			jobs = HttpRequestEntity.get(url).backendInternalCall().execute().toList(Map.class,"jobs");
		} catch (MendmixBaseException e) {
			if(e.getCode() == 404 || e.getCode() == 401 || e.getCode() == 403) {
				ignoreServiceIds.add(module.getServiceId());
			}
			jobs = new ArrayList<>(0);
		}
		return jobs;
	}

	
}
