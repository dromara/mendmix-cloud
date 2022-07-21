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
package com.mendmix.gateway.task;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.http.HttpRequestEntity;
import com.mendmix.common.model.ApiInfo;
import com.mendmix.common.model.ValueParam;
import com.mendmix.common.util.DateUtils;
import com.mendmix.common2.task.SubTimerTask;
import com.mendmix.gateway.CurrentSystemHolder;
import com.mendmix.gateway.model.BizSystem;
import com.mendmix.gateway.model.BizSystemModule;
import com.mendmix.spring.DataChangeEvent;
import com.mendmix.spring.InstanceFactory;
import com.mendmix.springweb.exporter.AppMetadataHolder;
import com.mendmix.springweb.model.AppMetadata;

public class ModuleApiRefreshTask implements SubTimerTask {

	private final static Logger logger = LoggerFactory.getLogger("com.mendmix.gateway");

	private static Map<String, Date> lastUpdateMapping = new HashMap<>(3);

	private static Date defaultUpdateTime = new Date();

	@Override
	public void doSchedule() {
		List<BizSystem> systems = CurrentSystemHolder.getSystems();
		for (BizSystem system : systems) {
			for (BizSystemModule module : system.getModules()) {
				if (module.getApiInfos() == null || moduleMetaChanged(module)) {
					initModuleApiInfos(module);
					InstanceFactory.getContext().publishEvent(new DataChangeEvent("moduleApis", new Object()));
				}
			}
		}
	}

	@Override
	public long interval() {
		return 30000;
	}

	protected boolean moduleMetaChanged(BizSystemModule module) {
		if(module.isGateway())return false;
		Date lastUpdateTime = lastUpdateMapping.getOrDefault(module.getRouteName(), defaultUpdateTime);
		try {
			ValueParam value = HttpRequestEntity.get(module.getMetadataUri()).queryParam("spec", "version")
					.backendInternalCall().execute().toObject(ValueParam.class);
			Date newUpdateTime = DateUtils.parseDate(value.getValue());
			lastUpdateMapping.put(module.getRouteName(), newUpdateTime);

			boolean changed = newUpdateTime.after(lastUpdateTime);
			if (changed)
				logger.info("ZVOS-FRAMEWORK-TRACE-LOGGGING-->> service:{} version changed", module.getServiceId());
			return changed;
		} catch (Exception e) {
			return false;
		}
	}

	public static void initModuleApiInfos(BizSystemModule module) {
		Collection<ApiInfo> apiInfos = null;
		try {
			String url;
			if(module.isGateway()) {
				apiInfos = AppMetadataHolder.getMetadata().getApis();
			} else {		
				url = module.getMetadataUri();
				if(url == null)return;
				logger.debug("MENDMIX-TRACE-LOGGGING-->> initModuleApiInfos begin -> serviceId:{},url:{}",module.getServiceId(),url);
				apiInfos = HttpRequestEntity.get(url).execute().toObject(AppMetadata.class).getApis();
			}
		} catch (Exception e) {
			boolean ignore = e instanceof NullPointerException;
			if(!ignore && e instanceof MendmixBaseException) {
				MendmixBaseException ex = (MendmixBaseException) e;
				ignore = ex.getCode() == 404 || ex.getCode() == 401 || ex.getCode() == 403;
			}
			logger.warn("MENDMIX-TRACE-LOGGGING-->> initModuleApiInfos error -> serviceId:{},error:{}",module.getServiceId(),e.getMessage());
		}
		
		if(apiInfos != null) {
			for (ApiInfo api : apiInfos) {
				module.addApiInfo(api);
			}
			if(module.getApiInfos() == null) {
				module.setApiInfos(new HashMap<>(0));
			}
			logger.info("MENDMIX-TRACE-LOGGGING-->> initModuleApiInfos end -> serviceId:{},apiNums:{}",module.getServiceId(),module.getApiInfos().size());
		}
	}

}
